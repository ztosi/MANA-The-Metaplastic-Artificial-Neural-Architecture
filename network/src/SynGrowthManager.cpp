#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <list>
#include <cmath>
#include <cstdlib>
#include <random>
#include "Neuron.hpp"
#include "SynMatrices.hpp"
#include "Utils.hpp"


SynGrowthManager::SynGrowthManager(	const SynMatrices &_synHost,
									const float _delThresh,
									const float _distMod,
									const float _decFac	)
	: synHost(_synHost), delThresh(_delThresh), distMod(_distMod),
	decFac(_decFac)
{
	initLambda(_synHost.MAX_DIST);
	unconnectMap(_synHost.tarHost.size);
	// Iterate over targets
	for (uint32_t i = 0; i < _synHost.tarHost.size; i++)
	{
		// Sort the src index values pertaining to each
		// synapse in the host.
		array srtInds = sort(_synHost.indices[i], 1, true);
		uint32_t k = 0;
		// Iterate over source neurons--because srtInds is
		// sorted this will catch all indices where a connection
		// exists
		for (uint32_t j = 0; j < _synHost.srcHost.size; j++)
		{
			if(k>=srtInds.dims[0]) { break; }
			if (srtInds(k) == j)
			{
				k++;
			} else {
				// no index j exists in srtInds--add to
				// the map of unconnected pairs
				unconnectMap[i].push_back(j);
			}
		}
	}
	maxNewCons = (uint32_t) (_synHost.maxCap * MAX_NCON_FRAC);
}

void SynGrowthManager::initLambda(float maxDist)
{
	lambda_sq = maxDist/std::sqrt(-std::log(MIN_BASE_CON_PROB/distMod));
}

double SynGrowthManager::connectProb(const Position &p1, const Position &p2)
{
	float dist = Position::euclidean(p1, p2);
	return distMod * std::exp(-((dist*dist)/lambda_sq));
}

void SynGrowthManager::invoke()
{
	uint32_t srcsz = synHost.srcHost.size;
	uint32_t tarsz = synHost.tarHost.size;

	// Find the maximum local synaptic weight
	float mx = 0;
	float mxs[tarsz]; 
	gfor(seq i, synHost.tarHost.size)
	{
		// Update any non-updated weights prior to pruning
		// ... we need to know what their actual strengths are
		synHost.wt_And_dw[i].col(0) = SynMatrices::dampen(
			netHost.dt * (netHost.getTime()-synHost.lastUp[i]),
			synHost.wt_And_dw[i].col(0), synHost.wt_And_dw[i].col(1));
		mxs[i] = af::max(synHost.wt_And_dw[i].col(0));
	}
	for (uint32_t i = 0; i < tarsz; i++)
	{
		if (mxs[i] > mx) { mx = mxs[i]; }
	}

	// Eligibility for deletion value
	float cutVal = delThresh * mx;

	// Determine local in and out degree ratios
	array outDRat_loc = constant(0, dim4(srcsz, 1), f32);
	float inDRat_loc[tarsz];
	for (uint32_t i  = 0; i < tarsz; i++)
	{
		outDRat_loc(synHost.indices[i]) =
			outDRat_loc(synHost.indices[i]) + 1;
		inDRat_loc[i] = (float) synHost.indices[i].size() / tarsz;
	}
	outDRat_loc = outDRat_loc/(float)tarsz;
	outDRat_loc *= outDRat_loc; // Out ratio squared...


	// TODO: make sure to reconfigure the delay change/indices actual...
	std::vector<array> toDelete(tarsz);
	gfor(seq i, synHost.tarHost.size)
	{
		array ndel = inDRat_loc[i] * outDRat_loc(synHost.indices[i]) + minDel;
		array coin = randu(ndel.dims());
		array del = (coin < ndel) && (synHost.wt_And_dw[i].col(0) < cutVal);
		ndel = where(!del);
		del = where(del);
		toDelete[i] = synHost.indices[i](del);
	
		// Remove synpase data from relevant fields...
		synHost.wt_And_dw[i] = synHost.wt_And_dw[i](ndel, span);
		synHost.lastUp[i] = synHost.lastUp[i](ndel);
		synHost.lastArrT[i] = synHost.lastArrT[i](ndel);
		synHost.indices[i] = synHost.indices[i](ndel);
		synHost.indicesActual[i] = synHost.indicesActual[i](ndel);
	}

	// Add the removed synapses to the unconnected pairs map
	// and tally how many were removed
	uint32_t numDeleted = 0;
	for (uint32_t i = 0; i < tarsz; i++)
	{
		uint32_t* delHost = new uint32_t[toDelete[i].dims(0)];
		toDelete.host(delHost);
		numDeleted += toDelete[i].dims(0);
		for (uint32_t j = 0; j < toDelete[i].dims(0); j++)
		{
			unconnectedMap[i].push_back(delHost[j]);
		}
		delete[]  delHost;
	}

	uint32_t num2Add = numDeleted > maxNewCons ? maxNewCons : numDeleted;
	std::vector<list<uint32_t> toAddByTar(tarsz);
	while (num2Add > 0)
	{
		uint32_t t = std::rand() % tarsz; // Pick a random target
		// pick a random postition in the list of indices this target
		// doesn't recieve connections from
		uint32_t spos = std::rand() % unconnectedMap[t].size();
		// Retrieve the src neuron index at the selected ucMap index
		std::list<uint32_t>::iterator iter = unconnectedMap[t].begin();
		advance(iter, spos);
		uint32_t s = *iter;
		// Determine the probability of making a connection based on distance...
		double cProb = connectProb(synHost.tarHost.getPosition(t), synHost.srcHost.getPosition(s));
		// Roll the dice--see if we get to make a connection...
		if (Utils::rand_float(0, 1) < cProb)
		{
			//Connection successful!
			// Add src index to the list of new incoming
			// connections to t
			toAddByTar[t].push_back(s);
			// Remove this entry from the map of src neurons
			// t *doesn't* receive inputs from
			unconnectedMap[t].erase(iter);
			// Decrement the number to be added, since we added one
			num2Add--;
		}

	}




}







