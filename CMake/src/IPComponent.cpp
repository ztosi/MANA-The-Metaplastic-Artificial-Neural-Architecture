#include <arrayfire.h>
#include <cstdint>
#include "../include/Neuron.hpp"
#include "../include/IPComponent.hpp"
#include "../include/SynMatrices.hpp"


using namespace af;

IPComponent::IPComponent(	AIFNeuron &_neuHost,
							HPComponent* _hpHost	)

	: neuHost(_neuHost), eta(DEF_ETA0), minPFR(DEF_MIN_PFR),
	lowFR(DEF_LOW_FR), alpha(DEF_ALPHA), beta(DEF_BETA), eta_0(DEF_ETA0),
    eta_f(DEF_ETAF), noiseSD(DEF_NOISE_SD), eta_dec(DEF_ETA_DEC)
{
	hpHost = _hpHost;
	prefFR = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
}

IPComponent::IPComponent(	AIFNeuron &_neuHost,
							HPComponent* _hpHost,
							const float _eta_0,
							const float _eta_f,
							const float _eta_dec	)

	: neuHost(_neuHost), eta(_eta_0), minPFR(DEF_MIN_PFR), lowFR(DEF_LOW_FR),
    alpha(DEF_ALPHA), beta(DEF_BETA), eta_0(_eta_0),  eta_f(_eta_f),
    eta_dec(_eta_dec), noiseSD(DEF_NOISE_SD)
{
	hpHost = _hpHost;
	prefFR = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
}

void IPComponent::calcEstFRsForHost()
{
	neuHost.epsilon += -neuHost.netHost.dt * neuHost.epsilon/neuHost.tauEps + neuHost.spks;
	neuHost.nu_hat += neuHost.netHost.dt * (-neuHost.nu_hat + neuHost.epsilon/neuHost.tauEps);
}

void IPComponent::perform()
{
	const uint32_t noIncExcGrps = neuHost.incoExcSyns.size();
	const uint32_t noIncInhGrps =  neuHost.incoInhSyns.size();
	array f_plus = af::exp(-prefFR/(beta*lowFR));
	array f_minus = constant(0, dim4(neuHost.size, 1), f32);
	array above = where(prefFR > lowFR);
	array below = where(prefFR <= lowFR);
	f_minus(above) = 1 + (log(1-alpha*(prefFR(above)/lowFR - 1)))/alpha;
	f_minus(below) = prefFR(below)/lowFR; 
	
	// Store the FR potentiating values and depressing values
	array poten = constant(0, dim4(neuHost.size, 1), f32);
	array depre = constant(0, dim4(neuHost.size, 1), f32);



	// Iterate over incoming excitatory neuron groups
	for (uint32_t j = 0; j < noIncExcGrps; j++)
	{
        array i_indices = neuHost.incoExcSyns[j]->ijInds->col(0);
        array j_indices = neuHost.incoExcSyns[j]->ijInds->col(1);
        
		array diffs = neuHost.incoExcSyns[j]->srcHost.nu_hat(i_indices);
		diffs -= neuHost.nu_hat(j_indices);
		array srcSlower = diffs<0;
		diffs = exp(-abs(diffs)/prefFR(j_indices));

		array diffsp = diffs * srcSlower;
		diffs *= !srcSlower;

		diffsp = scanByKey(j_indices, diffsp, 0, AF_BINARY_ADD);
		diffs = scanByKey(j_indices, diffs, 0, AF_BINARY_ADD);
    
        array ends = neuHost.incoExcSyns[j]->tarStartFin->col(1);

		poten += diffsp(ends);
		depre += diffs(ends);
	}

	// Iterate over incoming excitatory neuron groups
	for (uint32_t j = 0; j < noIncInhGrps; j++)
	{
        array i_indices = neuHost.incoExcSyns[j]->ijInds->col(0);
        array j_indices = neuHost.incoExcSyns[j]->ijInds->col(1);
        
		array diffs = neuHost.incoInhSyns[j]->srcHost.nu_hat(i_indices);
		diffs -= neuHost.nu_hat(j_indices);
		array srcSlower = diffs<0;
		diffs = exp(-abs(diffs)/prefFR(j_indices));

		array diffsp = diffs * srcSlower;
		diffs *= !srcSlower;

		diffsp = scanByKey(j_indices, diffsp, 0, AF_BINARY_ADD);
		diffs = scanByKey(j_indices, diffs, 0, AF_BINARY_ADD);

        array ends = neuHost.incoExcSyns[j]->tarStartFin->col(1);

		poten += diffsp(ends);
		depre += diffs(ends);
	}
	
	poten *= f_plus;
	depre *= f_minus;
	prefFR_Buff = prefFR + (neuHost.netHost.dt * eta * (poten - depre + af::randn(neuHost.size)*noiseSD));
	eta -= neuHost.netHost.dt*(eta-eta_f)*eta_dec;
}

void IPComponent::pushBuffers() {	prefFR = prefFR_Buff;	}

array IPComponent::getPrefFRs() {	return prefFR;	}

