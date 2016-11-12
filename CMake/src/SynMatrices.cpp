#include <arrayfire.h>
#include <assert.h>
#include <cmath>
#include <cstdint>
#include <vector>
#include <algorithm>
#include "../include/SynMatrices.hpp"
#include "../include/Neuron.hpp"
#include "../include/UDFPlasticity.hpp"
#include "../include/Utils.hpp"
#include "../include/Network.hpp"
#include "../include/STDP.hpp"

using namespace af;

SynMatrices* SynMatrices::connectNeurons(	GenericNeuron &_src,
											GenericNeuron &_tar,
											STDP* _splas,
											const uint32_t _minDly,
											const uint32_t _maxDly,
                                            const float initDensity,
											const bool useUDF )
{
	assert(&_src.netHost == &_tar.netHost); //no trans-network synapses

	SynMatrices* syns = new SynMatrices(_src, _tar, _splas, _maxDly, _minDly, initDensity);
	if (_src.pol)
	{
		_tar.addIncExcSyn(*syns);
		//_tar.incoExcSyns.push_back(*syns);
	} else {
        _tar.addIncInhSyn(*syns);
		//_tar.incoInhSyns.push_back(*syns);
	}
	if (useUDF)
	{
		syns->udf = UDFPlasticity::instantiateUDF(*syns);
	}
	
	return syns;
}

void SynMatrices::calcDelayMat(	const GenericNeuron* _src,
                                const GenericNeuron* _tar,
                                const uint32_t maxDly,
                                const uint32_t minDly,
                                uint32_t* dlys) 
{
	float** dists = new float *[_src->size];
	float MAX_DIST = 0;
	for (int i = 0; i < _src->size; i++) {
		dists[i] = new float[_tar->size];
		for (int j = 0; j < _tar->size; j++) {
			float xdist = _src->x[i] - _tar->x[j];
			float ydist = _src->y[i] - _tar->y[j];
			float zdist = _src->z[i] - _tar->z[j];
			float dist = std::sqrt(xdist*xdist + ydist*ydist + zdist*zdist);
			dists[i][j] = dist;
			if (dist > MAX_DIST) {
				MAX_DIST = dist;
			}
		}
	}

	//uint32_t* dlys = new uint32_t [_src->size * _tar->size];
	for (int j = 0; j < _tar->size; j++) {
		for (int i = 0; i < _src->size; i++) {
			if (_src == _tar && i==j) continue; // No self-connections
			dlys[j*_src->size + i] = (uint32_t) ((maxDly-minDly)*dists[i][j]/MAX_DIST+minDly);
		}
	}
    for (uint32_t i=0; i < _src->size; i++)
    {
       	delete [] dists[i]; 
    }
}

uint32_t SynMatrices::calcDelay(	const Position &p1,
									const Position &p2,
									const uint32_t _maxDly,
                                    const float MAX_DIST)
{
	uint32_t dist = Position::euclidean(p1, p2);
	dist = dist > MAX_DIST ? MAX_DIST : dist;
	return (uint32_t) (_maxDly * dist / MAX_DIST);
}


void SynMatrices::propagate()
{
	float dt = netHost.dt;
	uint32_t _time = netHost.getTime();
	
	// Find indices of the spk history relevant for these synaptic delays
	// 1s represent arrivals of synapses at the current time
	array arrivals  = srcHost.spkHistory(*srcDlyInds);
	
	// Get an array that is 1 for posy synaptic spikes and expand to the 
	// size of the synapse array
    //array inds = (*ijInds).col(1);
	array postTriggered = tarHost.spks((*ijInds).col(1).copy());

	// Determine indices of neurons requiring updates w = w + dw*time since last update
	array w2up = where(arrivals || postTriggered);

	// Update weights of synapses where pre or post is active...
	(*wt_And_dw)(w2up, 0) += dampen(_time - (*lastUp)(w2up), (*wt_And_dw)(w2up, 0), (*wt_And_dw)(w2up, 1));

	// All those that were updated have their last update time changed to now
	(*lastUp)(w2up) = _time;
	
	// Get these as indices instead of logicals...
	arrivals = where(arrivals);
	postTriggered = where(postTriggered);

	array results;

	if (usingUDF)
	{
		results = udf->perform((*lastArr)(arrivals)-_time, (*wt_And_dw)(arrivals, 0));	// Only results for arrivals (unique to is clumped by js)
		(*lastArr)(arrivals) = _time;
	} else {
		results = (*wt_And_dw)(arrivals, 0);
	}

	array inds = (*ijInds)(arrivals, 1); // j, target neurons of arrivals

	results = scanByKey(inds, results, 0, AF_BINARY_ADD);
    inds = diff1(inds);
    inds = where(inds);
	if (srcPol) {
		tarHost.I_e += results(inds); // where(diff()) shows last position of each target neuron i.e. where the scan operation will have put the sum over all going to that j/neuron
	} else {
		tarHost.I_i += results(inds);
	}

	// Find the new dws for synapses with arriving PSPs
	// Just find out the difference for all neurons since there are
	// O(sqrt(Syns)) number of neurons and the extra indexing step probably
	// wouldn't speed things up
	array pdws = dt*(tarHost.lastSpkTime-_time);
	splas->preTrigger(pdws); 

	(*wt_And_dw)(arrivals,1) = pdws(inds);

	pdws = dt*(srcHost.lastSpkTime-_time);
	splas->postTrigger(pdws);

	(*wt_And_dw)(postTriggered, 1) = pdws((*ijInds)(postTriggered, 0).copy());

}

array SynMatrices::dampen(const array &duration, const array &initVal, const array &dv)
{
	return dInteg(initVal + (dv*duration)) - dInteg(initVal);
}

array SynMatrices::dInteg(const array &val)
{
	return P1/4 * (val*val*val*val) + P2/3 * (val*val*val) + P3/2 * (val*val) + P4 * val;
}


SynMatrices::SynMatrices(	GenericNeuron &_src,
							GenericNeuron &_tar,
                            STDP* _splas,
							const uint32_t _minDly,
							const uint32_t _maxDly,
							const float _initDensity	) 
	: netHost(_src.netHost), srcHost(_src), tarHost(_tar), splas(_splas),
	 minDly(_minDly), maxDly(_maxDly), usingUDF(true)
{

	// TODO: Perform error checking such that the specified max delay does not exceed
	// that supported by the source neurons' spike history array
	uint32_t* dlys = new uint32_t[_src.size * _tar.size];
    calcDelayMat(&_src, &_tar, _maxDly, _minDly, dlys);
	dlyArr = new array(_src.size * _tar.size, 1, dlys, afHost);
	array mask = randu(_src.size * _tar.size, 1, f32) < _initDensity;
	*dlyArr = (*dlyArr) * mask;
	*ijInds = lin2IJ(where(*dlyArr), _src.size);
	*dlyArr = (*dlyArr)(where(*dlyArr));
	*srcDlyInds = srcInd2dlysrcInd(_src.size, (*ijInds)(span,0), *dlyArr);
	size = dlyArr->dims(0);
	float* wanddw = new float[size*2];
	uint32_t* lUp = new uint32_t[size];
	for (uint32_t i=0; i < size; i++) 
	{
		wanddw[i]=START_WT;
		wanddw[size+i]=START_WT;
		lUp[i]=0;
	}
	wt_And_dw = new array(size, 2, wanddw, afHost);
	lastUp = new array(size, 1, lUp, afHost);
	tarStartFin = new array(findStAndEnds(*ijInds));

	srcPol = _src.pol;
	tarPol = _tar.pol;

	delete[] wanddw;
	delete[] lUp;
	delete[] dlys;

}



