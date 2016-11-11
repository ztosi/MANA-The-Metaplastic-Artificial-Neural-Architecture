#include <arrayfire.h>
#include <cstdint>
#include "../include/Neuron.hpp"
#include "../include/IPComponent.hpp"


using namespace af;

IPComponent::IPComponent(	const AIFNeuron &_neuHost,
							const HPComponent* _hpHost	)

	: neuHost(_neuHost), eta(DEF_ETA0), minPFR(DEF_MIN_PFR),
	lowFR(DEF_LOW_FR), beta(DEF_BETA), eta_0(DEF_ETA0), eta_f(DEF_ETAF),
	noiseSD(DEF_NOISE_SD), eta_dec(DEF_ETA_DEC)
{
	hpHost = _hpHost;
	prefFR = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
}

IPComponent::IPComponent(	const AIFNeuron &_neuHost,
							const HPComponent* _hpHost,
							const float _eta_0,
							const float _eta_f,
							const float _eta_dec	)

	: neuHost(_neuHost), eta(_eta_0), minPFR(DEF_MIN_PFR), lowFR(DEF_LOW_FR),
	  beta(DEF_BETA), eta_0(_eta_0), eta_dec(_eta_dec), noiseSD(DEF_NOISE_SD)
{
	hpHost = _hpHost;
	prefFR = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(neuHost.size, 1), f32));
	eta_f = DEF_ETAF; 
}

// void IPComponent::performTypeI(const float dt, const array &flipped)
// {

// 	array thTerm = abs(mThresh - (neuHost.thresholds)) * hpHost->dThdt
// 		* hpHost->lambda;

// 	array chg = (tooFast || prefFR > watcher->nu_E)
// 		&& !(tooFast || prefFR > watcher->nu_E);

// 	tooFast = prefFR > watcher->nu_E;

// 	prefFR_Buff = exp(-prefFR/(beta*lowFR)) * tooFast;

// 	array wayTooSlow = (prefFR < lowFR) && !tooFast;

// 	prefFR_Buff += (1 + ((log(1+alpha*(prefFR/lowFR - 1))) / alpha)) * !tooFast * !wayTooSlow;

// 	prefFR_Buff +=  prefFR/lowFR * wayTooSlow;

// 	zeta = zeta * !chg + ((randn(dim4(1, neuHost.size), f32) * noiseSD) + 1) * chg;

// 	eta = flipped * eta_f + !flipped * eta_0;

// 	prefFR_Buff = prefFR + (eta * dt * thTerm * zeta * prefFR_Buff);

// 	mThresh = (*(host.thresholds) * dt/hpHost->lambda)
// 		+ ((1 - dt/hpHost->lambda) * mThresh);

// }

void IPComponent::calcEstFRsForHost()
{
	neuHost.epsilon += -neuHost.host.dt * neuHost.epsilon/neuHost.tauEps + neuHost.spks;
	neuHost.nu_hat += neuHost.host.dt * (-neuHost.nu_hat + neuHost.epsilon/neuHost.tauEps);
}

void IPComponent::perform()
{
	const uint32_t noIncExcGrps = neuHost.incoExcSyns.getSize();
	const uint32_t noIncInhGrps =  neuHost.incoInhSyns.getSize();
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
		array diffs = neuHost.incoExcSyns[j].srcHost.nuHat((*(neuHost.incoExcSyns[j].ijInds)).col(0));
		diffs -= neuHost.nuHat((*(neuHost.incoExcSyns[j].ijInds)).col(1));
		array srcSlower = diffs<0;
		diffs = exp(-abs(diffs)/prefFR((*(neuHost.incoExcSyns[j].ijInds)).col(1)));

		diffsp = diffs * srcSlower;
		diffs *= !srcSlower;

		diffsp = scanByKey((*(neuHost.incoExcSyns[j].ijInds)).col(1), diffsp, 0, AF_BINARY_ADD);
		diffs = scanByKey((*(neuHost.incoExcSyns[j].ijInds)).col(1), diffs, 0, AF_BINARY_ADD);

		poten += diffsp((*(neuHost.incoExcSyns[j].tarStartFin)).col(1));
		depre += diffs((*(neuHost.incoExcSyns[j].tarStartFin)).col(1));
	}

	// Iterate over incoming excitatory neuron groups
	for (uint32_t j = 0; j < noIncInhGrps; j++)
	{
		array diffs = neuHost.incoInhSyns[j].srcHost.nuHat((*(neuHost.incoInhSyns[j].ijInds)).col(0));
		diffs -= neuHost.nuHat((*(neuHost.incoInhSyns[j].ijInds)).col(1));
		array srcSlower = diffs<0;
		diffs = exp(-abs(diffs)/prefFR((*(neuHost.incoInhSyns[j].ijInds)).col(1)));

		diffsp = diffs * srcSlower;
		diffs *= !srcSlower;

		diffsp = scanByKey((*(neuHost.incoInhSyns[j].ijInds)).col(1), diffsp, 0, AF_BINARY_ADD);
		diffs = scanByKey((*(neuHost.incoInhSyns[j].ijInds)).col(1), diffs, 0, AF_BINARY_ADD);

		poten += diffsp((*(neuHost.incoInhSyns[j].tarStartFin)).col(1));
		depre += diffs((*(neuHost.incoInhSyns[j].tarStartFin)).col(1));
	}
	
	poten *= f_plus;
	depre *= f_minus;
	prefFR_Buff = prefFR + (neuHost.host.dt * eta * (poten - depre + af::randn(neuHost.size)*noiseSD));
	eta -= neuHost.host.dt*(eta-eta_f)*eta_dec;
}

void IPComponent::pushBuffers() {	prefFR = prefFR_Buff;	}

array IPComponent::getPrefFRs() {	return prefFR;	}

