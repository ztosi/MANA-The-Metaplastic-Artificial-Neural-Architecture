#include <arrayfire.h>
#include <math.h>
#include <cstdint>
#include <vector>
#include <algorithm>
#include "SynMatrices.h"

using namespace af;

SynMatrices::SynMatrices(	const AIFNeuron &_src,
							const AIFNeuron &_tar,
							const Spk_Delay_Mngr &_manager,
							const STDP &_splas,
							const uint32_t _minDly,
							const uint32_t _maxDly ) 
	: srcHost(_src), tarHost(_tar), splas(_splas)//, manager(_manager)
{
	maxDly = _maxDly;
	minDly = _minDly;
	uint32_t sSz = src.size;
	uint32_t tSz = tar.size;
	dlyRange = maxDly - minDly;
	dlyMat = calcDelayMat(&src, &tar, maxDly);
	uint32_t totSz = 0;
	for (uint32_t j = 0; j < tSz; j++) {
		//std::vector<std::vector<uint32_t>> wtTemp(sSz, std::vector<uint32_t>);
		std::vector<std::vector<uint32_t>> indTemp(dlyRange, std::vector<uint32_t>);
		totSz = 0;
		for (uint32_t i = 0; i < sSz; i++) {
			if (dlyMat[i][j] !=0) {
						// Implicit dly ind           incoming index
				indTemp[dlyMat[i][j]-minDly].push_back(i);	
				totSz++;
			}
		}
		// Store where delay changes occur
		array ptr_loc = constant(0, dlyRange, u32);
		// Create the array that stores source neuron indices
		array inds_loc = constant(0, totSz, u32);
		array inds_emp_loc = constant(0, totSz, u32);
		for (uint32_t k = 0; k < dlyRange; k++) {
			// If dlyMat is well behaved sorting is a minor optimization
			// that coule improve data locality.
			std::sort(indTemp[k].begin(), indTemp[k].end());

			// Copy these values into the index array 
			for (uint32_t l = 0; l < indTemp[k].size(); l++) {
				// Store the empirical indices of the source neurons in
				// their object
				inds_emp_loc(ptr_loc(k)+l) = indTemp[k][l];
				// Store the indices they'll have in the giant 
				// array of delayed spikes (#neurons * dlyRange)
				inds_loc(ptr_loc(k)+l) = indTemp[k][l]+((k+minDly) * src.size);
			}

			// Store the size in the ptr
			if (k < dlyRange - 1) {
				ptr_loc(k+1) = indTemp[k].size() + ptr_loc(k);
			}
		}

		indices.push_back(inds_loc);
		indicesActual.push_back(inds_emp_loc);
		ptrs.push_back(ptr_loc);

		incWts.push_back(constant(START_WT, totSz, f32));
		incDw.push_back(constant(0, totSz, f32));
		lastUp.push_back(constant(0, totSz, u32));
		lastArrT.push_back(constant(0, totSz, u32));
		incDw.push_back(constant(0, totSz, f32));

	}

	srcPol = src.polarity;
	tarPol = tar.polarity;

}

SynMatrices* SynMatrices::connectNeurons(AIFNeuron &_src,
										AIFNeuron &_tar,
										const Spk_Delay_Mngr &_manager,
										const STDP &_splas,
										const uint32_t _minDly,
										const uint32_t _maxDly )
{
	SynMatrices* syns = new SynMatrices(_src, _tar, _manager, _splas,
		_maxDly, _minDly); 
	if (_src.polarity)
	{
		_tar.incoExcSyns.push_back(*syns);
	} else {
		_tar.incoInhSyns.push_back(*syns);
	}
	return syns;
}


uint32_t** SynMatrices::calcDelayMat(AIFNeuron* src, AIFNeuron* tar, uint32_t maxDly) 
{
	float** dists = new float *[src->size];
	float MAX_DIST = 0;
	for (int i = 0; i < src->size; i++) {
		dists[i] = new float[tar->size];
		for (int j = 0; j < tar->size; j++) {
			float xdist = src->x[i] - tar->x[j];
			float ydist = src->y[i] - tar->y[j];
			float zdist = src->z[i] - tar->z[j];
			float dist = sqrt(xdist*xdist + ydist*ydist + zdist*zdist);
			dists[i][j] = dist;
			if (dist > MAX_DIST) {
				MAX_DIST = dist;
			}
		}
	}

	uint32_t** dlys = new uint32_t *[src->size];
	for (int i = 0; i < src->size; i++) {
		dlys[i] = new uint32_t [tar->size];
		for (int j = 0; j < tar->size; j++) {
			dlys[i][j] = (uint32_t) (maxDly*dists[i][j]/MAX_DIST);
		}
		delete [] dists[i];
	}
	delete [] dists;

	return dlys;
}

	// TODO: Order of synapse actions needs to be re-written
void SynMatrices::propagate_selective(	const uint32_t _time,
										const float dt,
										const UDFPlasticity &udf)
{
	array allSpks = src.getSpkHistory();
	std::vector<array> upSyns(host.tar.size);

	// Since branching is not allowed in gfor, pre-select the 
	// pre-triggered STDP function
	array (*stdpFunc)(uint32_t, array &);
	stdpFun = splas.hebb ? &(splas.preTriggerHebb)
		: &(splas.preTriggerAntiHebb);

	array lastSpks = *(host.tar.lastSpkTime);	

	std::vector<array>results(host.tar.size);

	//Least efficient part of the whole process... lots of cache misses gonna 
	// happen here...
	gfor (seq i, host.tar.size)
	{
		// Take the indices of the neurons projecting onto this neuron
		// (which are shifted based on their delay) and "lookup"
		// whether or not allSpks is "1" at these indices indicating
		// a spike arrived from that neuron at this time.
		// mask [i] will be same size as indicesAcutal[i]
		mask[i] = lookup(allSpks, indicesActual[i], 1);
		
		// Since all Spks is only 0s or 1s a sum tells us how many
		// pre-synaptic spikes we need to deal with
		uint32_t num2up = sum(mask);
		
		// determine what actual indices in the pre-synaptic arrays
		// have incoming spikes right now
		mask[i] = where(mask[i]); 
		
		// Select out the wts that will be changed
		array wts2Up = wt_And_dw[i](mask[i], 1);
		
		// find the time differential between the last time the weight
		// was updated and the current time to apply dw
		array tdiff = (_time-lastUp[i](mask[i]))*dt;
		lastUp[i](mask[i]) = _time;

		// Integrate using the dampening function to prevent wt overgrowth
		wts2Up = dampen(tdiff,  wts2Up, wt_And_dw[i](mask[i], 2));

		// Find all the UDF variables that will need to be updated
		array all2Up = udf.UDFuR(span, mask[i]);
		// Perform UDF calculations
		results[i] = udf.perform(all2Up, tdiff, wts2Up);
		
		// TODO Make sure that this is okay:
		udf.UDFuR(span, mask[i]) = all2Up;
		wt_And_dw[i](mask, 1) = wts2Up;

		wt_And_dw[i](mask, 2) = stdpFunc(_time,
			lastSpks(i), lastArrT[i]);

		lastArrT[i](mask) = _time;
	}


	if (host.srcPol) {
		gfor (seq i, host.tar.size)
		{
			host.tar.I_e(i) += sum(results[i]);
		}
	} else {
		gfor (seq i, host.tar.size)
		{
			host.tar.I_i(i) += sum(results[i]);
		}
	}

	

}


array SynMatrices::dampen(const array &duration, const array &initVal, const &array dv)
{
	return dInteg(initVal + (dv*duration)) - dInteg(initVal);
}

array SynMatrices::dInteg(const array &val)
{
	return P1/4 * (val*val*val*val) + P2/3 * (val*val*val) + P3/2 (val*val) + P4 * val;
}
