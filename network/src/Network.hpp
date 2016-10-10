#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"

#ifndef NETWORK_H_
#define NETWORK_H_

#define DEF_TIME_STEP 0.1f // ms


class Network {

	public:

		const float dt;



		uint32_t getTime() { return simTime; }

	private:

		uint32_t simTime;

		std::vector<Module> modules;
		std::vector<GenericNeuron> floatingNeurons;
		std::vector<SynMatrices> floatingSynapses;



};


#endif // Network_H_