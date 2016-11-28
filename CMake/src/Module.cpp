#include <arrayfire.h>
#include <cstdint>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/HPComponent.hpp"
#include "../include/IPComponent.hpp"
#include "../include/SynActors.hpp"
#include "../include/UDFPlasticity.hpp"
#include "../include/Network.hpp"
#include "../include/Module.hpp"
#include "../include/STDP.hpp"

#define DEF_INTRA_DLY_MN .5 // ms
#define DEF_INTRA_DLY_MX 15 // ms

Module::Module (    const Network &_netHost,
                    const uint32_t _size,
                    const Position _minPos,
                    const Position _maxPos, 
                    const float _ieRatio	)
    : netHost(_netHost), size(_size), minPos(_minPos), maxPos(_maxPos),
    numExc((uint32_t)(_size * (1-_ieRatio))),
    numInh((uint32_t)(_size * _ieRatio))
{}

Module::Module( 	 const Network &_netHost,
                    const uint32_t _size,
                    const Position _minPos,
                    const Position _maxPos	)
    : Module(_netHost, _size, _minPos, _maxPos, DEF_IE_RATIO) 
{}
                        

MANA_Module::MANA_Module(	const Network &_netHost,
							const uint32_t _size,
							const Position _minPos,
							const Position _maxPos	)
	: MANA_Module(_netHost, _size, _minPos, _maxPos, DEF_IE_RATIO)
{}

MANA_Module::MANA_Module(	const Network &_netHost,
					const uint32_t _size,
					const Position _minPos,
					const Position _maxPos,
					const float _ieRatio	)
	: Module(_netHost, _size, _minPos, _maxPos, _ieRatio)
{
    excNeuGrp = new AIFNeuron(_netHost, (uint32_t)(size * (1-_ieRatio)),
        Polarity::EXC, minPos, maxPos);
	inhNeuGrp = new AIFNeuron(_netHost, (uint32_t)(size * _ieRatio),
        Polarity::INH, minPos, maxPos);
	hpExc = NULL;
	hpInh = NULL;
	ipExc = NULL;
	ipInh = NULL;
	sNrmExc = NULL;
	sNrmInh = NULL;
}

MANA_Module* MANA_Module::buildMANA_Module(	const Network &_netHost,
											const uint32_t _size,
											const Position _minPos,
											const Position _maxPos )
{
	return MANA_Module::buildMANA_Module(_netHost, _size,
        _minPos, _maxPos, DEF_IE_RATIO);
}

MANA_Module* MANA_Module::buildMANA_Module(	const Network &_netHost,
									const uint32_t _size,
									const Position _minPos,
									const Position _maxPos,
									const float _ieRatio )
{
		// Construct module skeleton
	MANA_Module* mod = MANA_Module::buildMANA_Module(_netHost, _size, _minPos,
		_maxPos, _ieRatio);
    float dt = _netHost.dt;

	// Construct synaptic connections & STDP
    Polarity exc = Polarity::EXC;
    Polarity inh = Polarity::INH;
	
	// Magic Defaults; TODO: include constructor/factory function allowing
	// users to set these
	// Construct STDP first--minimal dependents...
	STDP* eeSTDP = new StandardSTDP(exc, exc, DEF_ETA);
	STDP* eiSTDP = new StandardSTDP(exc, inh, DEF_ETA);
	STDP* ieSTDP = new MexicanHatSTDP(inh, exc, DEF_ETA);
	STDP* iiSTDP = new MexicanHatSTDP(inh, inh, DEF_ETA);

	// Specify max/min delays
	uint32_t minDelay = (uint32_t) DEF_INTRA_DLY_MN/dt; // ms -> iterations
	uint32_t maxDelay = (uint32_t) DEF_INTRA_DLY_MX/dt; // ms -> iterations

	// Construct actual synapse groups (one for each Exc/Inh
	// combination), and store for later...
	SynMatrices* eeSyns = SynMatrices::connectNeurons(
		*(mod->excNeuGrp), *(mod->excNeuGrp), eeSTDP, minDelay, maxDelay, 1.0, true);
	SynMatrices* eiSyns = SynMatrices::connectNeurons(
		*(mod->excNeuGrp), *(mod->inhNeuGrp), eiSTDP, minDelay, maxDelay, 1.0, true);
	SynMatrices* ieSyns = SynMatrices::connectNeurons(
		*(mod->inhNeuGrp), *(mod->excNeuGrp), ieSTDP, minDelay, maxDelay, 1.0, true);
	SynMatrices* iiSyns = SynMatrices::connectNeurons(
		*(mod->inhNeuGrp), *(mod->inhNeuGrp), iiSTDP, minDelay, maxDelay, 1.0, true);
	mod->synGrps.push_back(eeSyns);
	mod->synGrps.push_back(eiSyns);
	mod->synGrps.push_back(ieSyns);
	mod->synGrps.push_back(iiSyns);

	// Construct the short-term plasticity components which attach themselves...
	mod->eeUDF = UDFPlasticity::instantiateUDF(*eeSyns);
	mod->eiUDF = UDFPlasticity::instantiateUDF(*eiSyns);
	mod->ieUDF = UDFPlasticity::instantiateUDF(*ieSyns);
	mod->iiUDF = UDFPlasticity::instantiateUDF(*iiSyns);

    AIFNeuron* excAIF = dynamic_cast<AIFNeuron*>(mod->excNeuGrp);
    AIFNeuron* inhAIF = dynamic_cast<AIFNeuron*>(mod->inhNeuGrp);

	// Finally construct synaptic normalizers
	mod->sNrmExc = new MANA_SynNormalizer(*excAIF); // Default params...
	mod->sNrmInh = new MANA_SynNormalizer(*inhAIF);

	// Start building neuron-plasticity components...

	// Homeostatic plasticity first, IP assumes there is an HP,
	// HP makes no such assumptions
	mod->hpExc = new HPComponent(*excAIF);
	mod->hpInh = new HPComponent(*inhAIF);

	// Intrinsic plasticity
	mod->ipExc = new IPComponent(*excAIF, mod->hpExc);
	mod->ipInh = new IPComponent(*inhAIF, mod->hpInh);

	return mod;
}

void MANA_Module::iterate_one()
{
    updateComplete = false;
        
	// Calculate the local estimate firing rates
	ipExc->calcEstFRsForHost();
	ipInh->calcEstFRsForHost();

	// Excitatory sub-group
	// TODO: Order of synapse actions needs to be re-written
	for (uint32_t i = 0; i < (excNeuGrp->incoExcSyns).size(); i++) {
		excNeuGrp->incoExcSyns[i]->propagate();
	}
	for (uint32_t i = 0; i < (excNeuGrp->incoInhSyns).size(); i++) {
		excNeuGrp->incoInhSyns[i]->propagate();
	}
	sNrmExc->perform(*(ipExc->prefFR), lambda);
	excNeuGrp->runForward();
	hpExc->perform(*(ipExc->prefFR));
	ipExc->perform();

	// Data no longer needed this iteration--synchronize for next
	// iteration while still somewhere closer by...
	hpExc->pushBuffers();
	ipExc->pushBuffers();

	// Inhibitory sub-group
	// TODO: Order of synapse actions needs to be re-written
	for (uint32_t i = 0; i < (inhNeuGrp->incoExcSyns).size(); i++) {
		inhNeuGrp->incoExcSyns[i]->propagate();
	}
	for (uint32_t i = 0; i < (inhNeuGrp->incoInhSyns).size(); i++) {
		inhNeuGrp->incoInhSyns[i]->propagate();
	}
	sNrmInh->perform(*(ipInh->prefFR), lambda);
	inhNeuGrp->runForward();
	hpInh->perform(*(ipInh->prefFR));
	ipInh->perform();

    lambda -= LAMB_DEC*(lambda-DEF_END_LAMB);
    
    updateComplete = true;

}

void MANA_Module::push_buffers()
{
    hpInh->pushBuffers();
	ipInh->pushBuffers();

	// synchronize everyone for next iteration...
	excNeuGrp->pushBuffers();
	inhNeuGrp->pushBuffers();
}

void MANA_Module::runForward(const uint32_t numIters)
{
	for(uint32_t i = 0; i < numIters; i++)
	{
		iterate_one();
        push_buffers();
		//netHost.requestClockForward();
	}
}
