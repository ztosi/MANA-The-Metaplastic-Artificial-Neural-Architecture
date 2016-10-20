#include <arrayfire.h>
#include <cstdint>
#include "Neuron.hpp"
#include "SynMatrices.hpp"
#include "Utils.hpp"

#ifndef SYNGROWTHMANAGER_H_
#define SYNGROWTHMANAGER_H_

class SynGrowthManager
{
	public:

		static const float DEF_PRUNE_THRESH = 0.05f;
		static const float DEF_DIST_MOD = 0.4f;
		static const float DEF_DECAY_FAC = 0.9f;
		static const float MIN_BASE_CON_PROB = 0.01;
		// Max number of connections that can be grown per invocation
		// shall be 0.1% of the maximum possible number of synapses
		// TODO: make this settable...
		static const float MAX_NCON_FRAC = 0.001;

		SynGrowthManager(const SynMatrices &_synHost);
		SynGrowthManager(	const SynMatrices &_synHost,
							const float _delThresh,
							const float _distMod,
							const float _lambda,
							const float _decFac	);

		void initLambda(float maxDist);
		double connectProb(const Position &p1, const Position &p2);
		void invoke();

		// TODO: Create multiple elimination/growth rules
		// and machinery for their aribitrary selection and use
		// in invoke()

	private:

		SynMatrices &synHost;

		// The cutoff for deletion elegibility as a proportion
		// of the strongest synapse in the group.
		float delThresh = DEF_PRUNE_THRESH;

		float minDel = DEF_MIN_DEL;

		// Constant scalar factor in determining how likely
		// a connection is to be made based on distance
		float distMod = DEF_DIST_MOD;

		// The "mean distance" of established new connections
		float lambda_sq;

		// Proportion of the number synapses lost in pruning
		// to add to the synapse group
		float decFac = DEF_DECAY_FAC;

		uint32_t maxNewCons;

		std::vector<std::list<uint32_t>> unconnectMap; 

};


#endif // SYNGROWTHMANAGER_H_