#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <list>
#include <cmath>
#include <cstdlib>
#include <random>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/Utils.hpp"
#include "../include/SynGrowthManager.hpp"
#include "../include/UDFPlasticity.hpp"

using namespace af;

SynGrowthManager::SynGrowthManager(	SynMatrices &_synHost,
									const float _delThresh,
									const float _minVal,
                                    const float _minDensity)
									//const float _decFac	)
	: synHost(_synHost), delThresh(_delThresh), minVal(_minVal), minDensity(_minDensity) {}

void SynGrowthManager::invoke()
{
	uint32_t srcsz = synHost.srcHost.size;
	uint32_t tarsz = synHost.tarHost.size;
	uint32_t maxNoSyns = &(synHost.srcHost)==&(synHost.tarHost) ? srcsz*(srcsz-1) : srcsz * tarsz;

	// don't prune more synapses if we are below our minimum synaptic density
	if (synHost.getSize() < minDensity*maxNoSyns) { return; } 

	array mxWt = max((*(synHost.wt_And_dw)).col(0));
	array cutoff = mxWt * delThresh;

	// put zeros (flagging for deletion later) in places where synapses have a weaker strength
	// than the minimum allowed value. 
	(*(synHost.wt_And_dw))(span,0) *= (*(synHost.wt_And_dw)).col(0) > minVal; 

	// find all synapses eligible for deletion
	array eligible = where((*(synHost.wt_And_dw)).col(0) < cutoff);

	array i_eligible = (*(synHost.ijInds))(eligible, 0); // srcs of syns elegible for deletion
	array j_eligible = (*(synHost.ijInds))(eligible, 1); // tars of syns elegible for deletion

	// retrieve in and out degrees of all neurons relevant to the group
	array oDegs(synHost.srcHost.size, 1, synHost.srcHost.outDegs, afHost);
	array iDegs(synHost.tarHost.size, 1, synHost.srcPol ? synHost.tarHost.exInDegs : synHost.tarHost.inInDegs, afHost);

	// find/expand only the relevant ones...
	oDegs = oDegs(j_eligible);
	oDegs /= synHost.tarHost.size; // as a proportion of possible connections...
	iDegs = iDegs(i_eligible);
	iDegs /= synHost.srcHost.size; // ditto

	// Determine probabilities for removal based on degrees...
	array toDelete = ((1.0 - minDelProb) * oDegs * oDegs * iDegs) + minDelProb;
	// determine who gets deleted... 0s where deletions will occur
	toDelete = toDelete > randu(toDelete.dims(0), f32);

	(*synHost.wt_And_dw)(eligible,0) *= toDelete;

	// find indices of all who weren't pruned
	array survivors = where((*(synHost.wt_And_dw)).col(0) > 0);

	// remove the deleted from all relevant data structures
	*(synHost.wt_And_dw )= (*(synHost.wt_And_dw))(survivors, span);
	*(synHost.ijInds) = (*(synHost.ijInds))(survivors, span);
	*(synHost.lastUp) = (*(synHost.lastUp))(survivors);
	*(synHost.lastArr) = (*(synHost.lastArr))(survivors);
	*(synHost.srcDlyInds) = (*(synHost.srcDlyInds))(survivors);
	*(synHost.dlyArr) = (*(synHost.dlyArr))(survivors);
	*(synHost.tarStartFin) = Utils::findStAndEnds(*(synHost.ijInds));
	if (synHost.isUsingUDF()) {
		(synHost.udf)->FUuRD = ((synHost.udf)->FUuRD)(survivors, span);
	}

	// change the in and out deg counts accordingly...
	array i_deleted = i_eligible * !toDelete; // now 1 where deletions occured...
	i_deleted = i_deleted(where(i_deleted));
	uint32_t *isDel = i_deleted.host<uint32_t>();
	array j_deleted = j_eligible * !toDelete;
	j_deleted = j_deleted(where(j_deleted));
	uint32_t *jsDel = j_deleted.host<uint32_t>();

	for (uint32_t i = 0; i < i_deleted.dims(0); i++)
	{
		--synHost.srcHost.outDegs[isDel[i]];
	}
	for (uint32_t j = 0; j < j_deleted.dims(0); j++)
	{
		if (synHost.srcPol)
			--synHost.tarHost.exInDegs[jsDel[j]];
	 	else
			--synHost.tarHost.inInDegs[jsDel[j]];		
	}
	synHost.size = (*(synHost.ijInds)).dims(0);
    delete isDel;
    delete jsDel;
}







