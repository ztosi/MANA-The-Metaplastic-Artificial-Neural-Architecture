#include <arrayfire.h>
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
	for (uint32_t i = 0; i < _synHost.tarHost.size; i++)
	{
		array srtInds = sort(_synHost.indices[i], 1, true);
		uint32_t k = 0;
		for (uint32_t j = 0; j < _synHost.srcHost.size; j++)
		{
			if(k>=srtInds.dims[0]) { break; }
			if (srtInds(k) == j)
			{
				k++;
			} else {
				unconnectMap[i].push_back(j);
			}
		}
	}
	maxNewCons = (uint32_t) (_synHost.maxCap * MAX_NCON_FRAC);
}

double SynGrowthManager::connectProb(const Position &p1, const Position &p2)
{
	float dist = Position::euclidean(p1, p2);
	return distMod * math::exp(-((dist*dist)/lambda_sq));
}

void SynGrowthManager::invoke()
{


}