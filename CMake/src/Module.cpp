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

#define DEF_INTRA_DLY_MN 1.5 // ms
#define DEF_INTRA_DLY_MX 15 // ms

MANA_Module::MANA_Module(	const Network &_host,
							const uint32_t _size,
							const Position _minPos,
							const Position _maxPos	)
	: host(_host),
	excNeuGrp(_host, (uint32_t)(size * (1-DEF_IE_RATIO)), (bool)1,
		minPos, maxPos),
	inhNeuGrp(_host, (uint32_t)(size * DEF_IE_RATIO), (bool)0,
		minPos, maxPos),
	size(_size), numExc((uint32_t) ((1-DEF_IE_RATIO) * _size),
	numInh((uint32_t) (DEF_IE_RATIO * _size))
{
	watcher = NULL;
	hpExc = NULL;
	hpInh = NULL;
	ipExc = NULL;
	ipInh = NULL;
	sNrmExc = NULL;
	sNrmInh = NULL;
}

MANA_Module::MANA_Module(	const Network &_host,
					const uint32_t _size,
					const Position _minPos,
					const Position _maxPos,
					const float _ieRatio	)
	: host(_host),
	excNeuGrp(_host, (uint32_t)(size * (1-ieRatio)), (bool)1,
		minPos, maxPos),
	inhNeuGrp(_host, (uint32_t)(size * ieRatio), (bool)0,
		minPos, maxPos),
	size(_size)
{
	watcher = NULL;
	hpExc = NULL;
	hpInh = NULL;
	ipExc = NULL;
	ipInh = NULL;
	sNrmExc = NULL;
	sNrmInh = NULL;
}

MANA_Module* MANA_Module::buildMANA_Module(	const Network &_host,
											const uint32_t _size,
											const Position _minPos,
											const Position _maxPos,
											const float _dt	)
{
	return MANA_Module::buildMANA_Module(_host, _size, _minPos, _maxPos,
		DEF_IE_RATIO, _dt);
}

MANA_Module* MANA_Module::buildMANA_Module(	const Network &_host,
									const uint32_t _size,
									const _Position minPos,
									const _Position maxPos,
									const float _ieRatio,
									const float _dt )
{
		// Construct module skeleton
	MANA_Module* mod = MANA_Module::buildMANA_Module(_host, _size, _minPos,
		_maxPos, _ieRatio);

	// Construct synaptic connections & STDP
	bool exc = (bool)1;
	bool inh = (bool)0;
	bool heb = (bool)1;
	
	// Magic Defaults; TODO: include constructor/factory function allowing
	// users to set these
	// Construct STDP first--minimal dependents...
	StandardSTDP* eeSTDP = new StandardSTDP(exc, exc, heb);
	StandardSTDP* eiSTDP = new StandardSTDP(exc, inh, heb);
	MexicanHatSTDP* ieSTDP = new MexicanHatSTDP(inh, exc);
	MexicanHatSTDP* iiSTDP = new MexicanHatSTDP(inh, inh);

	// Specify max/min delays
	uint32_t minDelay = (uint32_t) DEF_INTRA_DLY_MN/_dt; // ms -> iterations
	uint32_t maxDelay = (uint32_t) DEF_INTRA_DLY_MX/_dt; // ms -> iterations

	// Construct actual synapse groups (one for each Exc/Inh
	// combination), and store for later...
	SynMatrices* eeSyns = SynMatrices::connectNeurons(
		mod->excNeuGrp, mod->excNeuGrp, *eeSTDP, minDelay, maxDelay);
	SynMatrices* eiSyns = SynMatrices::connectNeurons(
		mod->excNeuGrp, mod->inhNeuGrp, *eiSTDP, minDelay, maxDelay);
	SynMatrices* ieSyns = SynMatrices::connectNeurons(
		mod->inhNeuGrp, mod->excNeuGrp, *ieSTDP, minDelay, maxDelay);
	SynMatrices* iiSyns = SynMatrices::connectNeurons(
		mod->inhNeuGrp, mod->inhNeuGrp, *iiSTDP, minDelay, maxDelay);
	synGrps.push_back(eeSyns);
	synGrps.push_back(eiSyns);
	synGrps.push_back(ieSyns);
	synGrps.push_back(iiSyns);

	// Construct the short-term plasticity components which attach themselves...
	eeUDF = new UDFPlasticity(*eeSyns);
	eiUDF = new UDFPlasticity(*eiSyns);
	ieUDF = new UDFPlasticity(*ieSyns);
	iiUDF = new UDFPlasticity(*iiSyns);

	// Finally construct synaptic normalizers
	sNrmExc = new MANA_SynNormalizer(excNeuGrp); // Default params...
	sNrmInh = new MANA_SynNormalizer(inhNeuGrp);

	// Start building neuron-plasticity components...

	// Firing rate estimation prerequisite for all neuronal plasticity
	watcherExc = new FREstimator(excNeuGrp, 1.0f, DEF_STATIC_TAU);
	watcherInh = new FREstimator(inhNeuGrp, 1.0f, DEF_STATIC_TAU);

	// Homeostatic plasticity first, IP assumes there is an HP,
	// HP makes no such assumptions
	hpExc = new HPComponent(excNeuGrp, watcherExc);
	hpInh = new HPComponent(inhNeuGrp, watcherInh);

	// Intrinsic plasticity
	ipExc = new IPComponent(excNeuGrp, hpExc);
	ipInh = new IPComponent(inhNeuGrp, hpInh);

	return mod;
}

void MANA_Module::iterateOne()
{

	// Calculate the local estimate firing rates
	ipExc.calcEstFRsForHost();
	ipInh.calcEstFRsForHost();

	// Excitatory sub-group
	// TODO: Order of synapse actions needs to be re-written
	for (uint32_t i = 0; i < excNeuGrp.incoExcSyns.getSize(); i++) {
		excNeuGrp.incoExcSyns[i]->propagate_selective(host.simTime, host.dt,
			eeUDF );
	}
	for (uint32_t i = 0; i < excNeuGrp.incoInhSyns.getSize(); i++) {
		excNeuGrp.incoInhSyns[i]->propagate_selective(host.simTime, host.dt,
			ieUDF );
	}
	sNrmExc.perform();
	excNeuGrp.runForward();
	hpExc.perform();
	ipExc.perform();

	// Data no longer needed this iteration--synchronize for next
	// iteration while still somewhere closer by...
	hpExc.pushBuffers();
	ipExc.pushBuffers();

	// Inhibitory sub-group
	// TODO: Order of synapse actions needs to be re-written
	for (uint32_t i = 0; i < inhNeuGrp.incoExcSyns.getSize(); i++) {
		inhNeuGrp.incoExcSyns[i]->propagate_selective(host.simTime, host.dt,
			eiUDF );
	}
	for (uint32_t i = 0; i < inhNeuGrp.incoInhSyns.getSize(); i++) {
		inhNeuGrp.incoInhSyns[i]->propagate_selective(host.simTime, host.dt,
			iiUDF );
	}
	sNrmInh.perform();
	inhNeuGrp.runForward();
	hpInh.perform();
	ipInh.perform();

	hpInh.pushBuffers();
	ipInh.pushBuffers();

	// synchronize everyone for next iteration...
	excNeuGrp.pushBuffers();
	inhNeuGrp.pushBuffers();

}

void MANA_Module::runForward(const uint32_t numIters)
{
	for(uint32_t i = 0; i < numIters; i++)
	{
		iterateOne();
		host.requestClockForward();
	}
}
