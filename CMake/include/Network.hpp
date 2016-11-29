#include <arrayfire.h>
#include <cstdint>
#include <list>
#include "Utils.hpp"

#ifndef NETWORK_H_
#define NETWORK_H_

const float DEF_TIMESTEP = 0.1;     // ms
const float DEF_MAX_DLY = 25.0;     // ms
const uint32_t DEF_MIN_DELAY = 5;   // 5 * 0.1 = 0.5ms
const uint32_t DEF_MAX_DELAY = 200; // 20 * 0.1 = 20ms

class Module;
class GenericNeuron;
class SynMatrices;

class Network
{

public:
    const float dt;
    const float maxDelay;

    uint32_t getTime() const
    {
        return simTime;
    }

    Network()
        : dt(DEF_TIMESTEP)
        , maxDelay(DEF_MAX_DLY)
        , simTime(0)
    {
    }

    Network(const float _dt, const float _maxDelay)
        : dt(_dt)
        , maxDelay(_maxDelay)
        , simTime(0)
    {
    }

    void addModule(Module& _mod2Add);
    void addNeurons(GenericNeuron& _neu2Add);
    void addSyns(SynMatrices& _syns2Add);

    void create_MANA_Mod(const uint32_t _size,
                         const Position _minPos,
                         const Position _maxPos); // using default parameters
    void create_SORN_Mod(const uint32_t _size,
                         const Position _minPos,
                         const Position _maxPos); // using default parameters
    void create_LIF_SORN_Mod(const uint32_t _size,
                             const Position _minPos,
                             const Position _maxPos); // using default paramters
    void create_Neurons(const uint32_t _size);
    
    /* Connect two groups of generic neurons with all synaptic plasticity turned on
     * with all defualt paramters.
     * */
    SynMatrices* connect_Neurons_Plastic(GenericNeuron& _src, GenericNeuron& _tar, const float _density);
    
    /* Connect two modules with all synaptic plasticity turned on
     * with all defualt paramters. All groups have the same density and all EE, EI, IE, and II connections
     * are made.
     * */
    SynMatrices** connect_Modules_Plastic(Module& _srcMod, Module& _tarMod, const float _density);
    
    /* Connect two groups of generic neurons with all synaptic plasticity turned off.
     * Plasticity can be turned on at a later time.
     * */
    SynMatrices* connect_Neurons_Static(GenericNeuron& _src, GenericNeuron& _tar, const float _density);
    
    /* Connect two modules with all synaptic plasticity turned on
     * with all defualt paramters. All groups have the same density and all EE, EI, IE, and II connections
     * are made.
     * */
    SynMatrices** connect_Modules_Static(Module& _srcMod, Module& _tarMod, const float _density);

    void runForward(const uint32_t _iterations);
    void runForward();

private:
    uint32_t simTime;
    std::list<Module*> modules;
    std::list<GenericNeuron*> floatingNeurons;
    std::list<SynMatrices*> floatingSynapses;
};

#endif // Network_H_
