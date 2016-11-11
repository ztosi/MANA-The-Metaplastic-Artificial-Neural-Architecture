#include <arrayfire.h>
#include <cstdint>


#ifndef NETWORK_H_
#define NETWORK_H_

const float DEF_TIMESTEP = 0.1; // ms

class Module;
class GenericNeuron;
class SynMatrices;

class Network {

	public:

		const float dt;

		uint32_t getTime() const {
            return simTime;
        }

	private:

		uint32_t simTime;

		std::vector<Module> modules;
		std::vector<GenericNeuron> floatingNeurons;
		std::vector<SynMatrices> floatingSynapses;



};


#endif // Network_H_
