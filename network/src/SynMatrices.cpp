#include <arrayfire.h>
#include <math.h>
#include <cstdint>
#include <vector>
#include <algorithm>
#include "SynMatrices.h"

using namespace af;

SynMatrices::SynMatrices(	const AIFNeuron &src,
							const AIFNeuron &tar,
							const uint32_t _maxDly,
							const uint32_t _minDly ) 
	: srcHost(src), tarHost(tar), manager(_manager)
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
		lastUp.push_back(constant(0, totSz, f32));
		lastArrT.push_back(constant(0, totSz, f32));
		incDw.push_back(constant(0, totSz, f32));

	}

	srcPol = src.polarity;
	tarPol = tar.polarity;

}

SynMatrices::void propagate_selective(const uint32_t time, const float dt)
{
	array allSpks = manager.getAll();
	std::vector<array> vec(host.tar.size);

	//True terribleness
	gfor (seq i, host.tar.size)
	{
		mask[i] = lookup(allSpks, indicesActual[i], 1);
		mask[i] = where(mask[i]); 
		array 
	}
}

SynMatrices::void propagate_selective(const uint32_t time, const float dt, const UDFPlasticity udf)
{
	array allSpks = manager.getAll();
	std::vector<array> upSyns(host.tar.size);

	//True terribleness
	gfor (seq i, host.tar.size)
	{
		mask[i] = lookup(allSpks, indicesActual[i], 1);
		uint32_t num2up = sum(mask);
		mask[i] = where(mask[i]); 
		array wts2Up = wt_And_dw[i](mask[i], 1);
		array tdiff = (time-lastUp(mask[i]))*dt;
		wts2Up = dampen(tdiff,  wts2Up, wt_And_dw[i](mask[i], 2));
		array all2Up = constant(0, dim4(5, num2up));

	}
}

SynMatrices::void propagate_brute(const uint32_t time, const float dt)
{
	array allSpks = manager.getAll();
	std::vector<array> vec(host.tar.size);

	//True terribleness
	gfor (seq i, host.tar.size)
	{
		mask[i] = lookup(allSpks, indicesActual[i], 1);
		mask[i] = where(mask[i]); 
		array 
	}
}

SynMatrices::array dampen(const array duration, const array initVal, const array dv)
{
	return dInteg(initVal + (dv*duration)) - dInteg(initVal);
}

SynMatrices::array dInteg(const array val)
{
	return P1/4 * (val*val*val*val) + P2/3 * (val*val*val) + P3/2 (val*val) + P4 * val;
}

SynMatrices::array vertConcat(std::vector<array> mats) {

}