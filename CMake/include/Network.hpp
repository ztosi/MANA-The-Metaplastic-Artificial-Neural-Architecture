#include <arrayfire.h>
#include <cstdint>
#include <list>


#ifndef NETWORK_H_
#define NETWORK_H_

const float DEF_TIMESTEP = 0.1; // ms
const float DEF_MAX_DLY = 25.0; // ms

class Module;
class GenericNeuron;
class SynMatrices;

class Network {

	public:

		const float dt;
        const float maxDelay;

		uint32_t getTime() const {
            return simTime;
        }
        
        Network() 
        : dt(DEF_TIMESTEP), maxDelay(DEF_MAX_DLY), simTime(0) {}
        
        Network(    const float _dt,
                    const float _maxDelay )
        : dt(_dt), maxDelay(_maxDelay), simTime(0) {}
        
        void addModule(Module &_mod2Add);
        void addNeurons(GenericNeuron &_neu2Add);
        void addSyns(SynMatrices &_syns2Add);
        
        void create_MANA_Mod(const uint32_t _size); // using default parameters
        void create_SORN_Mod(const uint32_t _size);     // using default parameters
        void create_LIF_SORN_Mod(const uint32_t _size); // using default paramters
        void create_Neurons(const uint32_t _size);
        SynMatrices* connect_Neurons(   GenericNeuron& _src,
                                        GenericNeuron& _tar,
                                        const float _density );
        SynMatrices** connect_Modules(   Module &_srcMod,
                                        Module &_tarMod, 
                                        const float _density );

        void runForward(const uint32_t _iterations);
        void runForward();

        private:

		uint32_t simTime;
		std::list<Module*> modules;
		std::list<GenericNeuron*> floatingNeurons;
		std::list<SynMatrices*> floatingSynapses;

};


#endif // Network_H_
