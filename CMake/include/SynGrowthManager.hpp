#include <arrayfire.h>
#include <cstdint>
#include "Neuron.hpp"
#include "SynMatrices.hpp"
#include "Utils.hpp"

#ifndef SYNGROWTHMANAGER_H_
#define SYNGROWTHMANAGER_H_

class SynMatrices;
class DataRecorder;

const float DEF_PRUNE_THRESH = 0.05;
const float DEF_MIN_VAL = 1E-5;
const float DEF_MIN_DENS = 0.1;
const float DEF_MIN_DEL_PROB = 0.001;

class SynGrowthManager
{
	public:

		SynGrowthManager(   SynMatrices &_synHost) : synHost(_synHost){}
		SynGrowthManager(	SynMatrices &_synHost,
							const float _delThresh,
							const float _minVal,
                            const float _minDensity);

		void invoke();

	private:

		SynMatrices &synHost;

		// The cutoff for deletion elegibility as a proportion
		// of the strongest synapse in the group.
		float delThresh = DEF_PRUNE_THRESH;
        
        float minDelProb = DEF_MIN_DEL_PROB;

		float minVal = DEF_MIN_VAL;
        
        float minDensity = DEF_MIN_DENS;

};


#endif // SYNGROWTHMANAGER_H_