#include <arrayfire.h>
#include <cstdint>
#include <list>

#include "../include/Network.hpp"
#include "../include/Module.hpp"
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/Utils.hpp"
#include "../include/STDP.hpp"
#include "../include/HPComponent.hpp"
#include "../include/IPComponent.hpp"
#include "../include/UDFPlasticity.hpp"
#include "../include/SynActors.hpp"
#include "../include/SynGrowthManager.hpp"


void Network::addModule(Module &_mod2Add)
{
    modules.push_back(&_mod2Add);
}

void Network::addNeurons(GenericNeuron &_neu2Add)
{
    floatingNeurons.push_back(&_neu2Add);
}

void Network::addSyns(SynMatrices &_syns2Add)
{
    floatingSynapses.push_back(&_syns2Add);
}

void Network::create_MANA_Mod(const uint32_t _size)
{
    
}

void Network::create_SORN_Mod(const uint32_t _size)
{
    // TODO
}

void Network::create_LIF_SORN_Mod(const uint32_t _size)
{
// TODO    
}

void Network::create_Neurons(const uint32_t _size)
{
    
}

SynMatrices* Network::connect_Neurons(GenericNeuron &_src, GenericNeuron &_tar, const float _density)
{
    SynMatrices* synm = SynMatrices::connectNeurons(_src,_tar,NULL, DEF_MIN_DELAY, DEF_MAX_DELAY, 1.0, false);
    addSyns(*synm);
    return synm;
}

SynMatrices** Network::connect_Modules(Module &_srcMod, Module &_tarMod, const float _density )
{
    
}

void Network::runForward()
{
    for(std::list<Module*>::iterator mit = modules.begin();
        mit != modules.end(); ++mit)
    {
        (*mit)->iterate_one();
    }
    for(std::list<GenericNeuron*>::iterator nit = floatingNeurons.begin();
            nit != floatingNeurons.end(); ++nit)
    {
        (*nit)->runForward();
    }
    for(std::list<Module*>::iterator mit = modules.begin();
        mit != modules.end(); ++mit)
    {
        (*mit)->push_buffers();
    }
    for(std::list<GenericNeuron*>::iterator nit = floatingNeurons.begin();
            nit != floatingNeurons.end(); ++nit)
    {
        (*nit)->runForward();
    }
    simTime++;
}

void Network::runForward(const uint32_t _iterations)
{
    for(uint32_t i=0; i < _iterations; i++)
    {
        runForward();
    }
}