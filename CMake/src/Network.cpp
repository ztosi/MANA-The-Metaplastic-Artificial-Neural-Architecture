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


void Network::addModule(Module& _mod2Add)
{
    modules.push_back(&_mod2Add);
}

void Network::addNeurons(GenericNeuron& _neu2Add)
{
    floatingNeurons.push_back(&_neu2Add);
}

void Network::addSyns(SynMatrices& _syns2Add)
{
    floatingSynapses.push_back(&_syns2Add);
}

void Network::create_MANA_Mod(const uint32_t _size, const Position _minPos, const Position _maxPos)
{
    MANA_Module* modl = MANA_Module::buildMANA_Module(*this, _size, _minPos, _maxPos);
    modules.push_back(modl);
}

void Network::create_SORN_Mod(const uint32_t _size, const Position _minPos, const Position _maxPos)
{
    // TODO
}

void Network::create_LIF_SORN_Mod(const uint32_t _size, const Position _minPos, const Position _maxPos)
{
// TODO    
}

void Network::create_Neurons(const uint32_t _size)
{
    
}

SynMatrices* Network::connect_Neurons_Static(GenericNeuron &_src, GenericNeuron &_tar, const float _density)
{
    SynMatrices* synm = SynMatrices::connectNeurons(_src,_tar, NULL, DEF_MIN_DELAY, DEF_MAX_DELAY, _density, false);
    addSyns(*synm);
    return synm;
}

SynMatrices* Network::connect_Neurons_Plastic(GenericNeuron &_src, GenericNeuron &_tar, const float _density)
{
    STDP* stdpRule;
    if(_src.pol == Polarity::EXC)
        stdpRule = new StandardSTDP(_src.pol, _tar.pol);
    else
        stdpRule = new MexicanHatSTDP(_src.pol, _tar.pol);
    SynMatrices* synm = SynMatrices::connectNeurons(_src,_tar, stdpRule , DEF_MIN_DELAY, DEF_MAX_DELAY, _density, true);
    addSyns(*synm);
    return synm;
}

SynMatrices** Network::connect_Modules_Plastic(Module& _srcMod,
                                               Module& _tarMod,
                                               const float _density = MANA_Module::DEF_INIT_DENSE)
{
    SynMatrices** synMats = new SynMatrices* [4];
    synMats[0] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             new StandardSTDP(Polarity::EXC, Polarity::EXC),
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             true);
    synMats[1] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.inhNeuGrp),
                                             new StandardSTDP(Polarity::EXC, Polarity::INH),
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             true);
                                                 synMats[0] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             new StandardSTDP(Polarity::EXC, Polarity::EXC),
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             true);
    synMats[2] = SynMatrices::connectNeurons(*(_srcMod.inhNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             new MexicanHatSTDP(Polarity::INH, Polarity::EXC, DEF_ETA, DEF_A, DEF_IE_SIG),
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             true);
    synMats[3] = SynMatrices::connectNeurons(*(_srcMod.inhNeuGrp),
                                             *(_tarMod.inhNeuGrp),
                                             new MexicanHatSTDP(Polarity::INH, Polarity::INH),
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             true);
    for(int i=0; i < 4;i++) floatingSynapses.push_back(synMats[i]);
    return synMats;
}

SynMatrices** Network::connect_Modules_Static(Module& _srcMod,
                                              Module& _tarMod,
                                              const float _density = MANA_Module::DEF_INIT_DENSE)
{
    SynMatrices** synMats = new SynMatrices* [4];
    synMats[0] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             NULL,
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             false);
    synMats[1] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.inhNeuGrp),
                                             NULL,
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             false);
                                                 synMats[0] = SynMatrices::connectNeurons(*(_srcMod.excNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             NULL,
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             false);
    synMats[2] = SynMatrices::connectNeurons(*(_srcMod.inhNeuGrp),
                                             *(_tarMod.excNeuGrp),
                                             NULL,
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             false);
    synMats[3] = SynMatrices::connectNeurons(*(_srcMod.inhNeuGrp),
                                             *(_tarMod.inhNeuGrp),
                                             NULL,
                                             DEF_MIN_DELAY,
                                             DEF_MAX_DELAY,
                                             _density,
                                             false);
    for(int i=0; i < 4;i++) floatingSynapses.push_back(synMats[i]);
    return synMats;
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