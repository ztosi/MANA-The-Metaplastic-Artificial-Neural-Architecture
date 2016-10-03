#include <arrayfire.h>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "FREstimator.h"


IPComponent::IPComponent(	const AIFNeuron &_neuHost,
							const HPComponent* _hpHost	)

	: neuHost(_neuHost), tooFast(constant(0, dim4(1, hostNeurons.size), f32)),
	zeta(constant(1, dim4(1, hostNeurons.size), f32)),
	eta(constant(DEF_ETA0, dim4(1, hostNeurons.size), f32)), minPFR(DEF_MIN_PFR),
	lowFR(DEF_LOW_FR), beta(DEF_BETA), eta_0(DEF_ETA0), eta_f(DEF_ETAF),
	noiseSD(DEF_NOISE_SD)
{
	hpHost = _hpHost;
	watcher = _hpHost->watcher;
	prefFR = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
}

IPComponent::IPComponent(	const AIFNeuron &_neuHost,
							const HPComponent* _hpHost
							const float eta_f	)

	: neuHost(_neuHost), tooFast(constant(0, dim4(1, hostNeurons.size), f32)),
	zeta(constant(1, dim4(1, hostNeurons.size), f32)),
	eta(constant(DEF_ETA0, dim4(1, hostNeurons.size), f32)), minPFR(DEF_MIN_PFR),
	normFactor(DEF_NRM_FAC), lowFR(DEF_LOW_FR), beta(DEF_BETA), eta_0(DEF_ETA0),
	eta_dec(DEF_ETA_DEC), noiseSD(DEF_NOISE_SD)
{
	hpHost = _hpHost;
	watcher = _hpHost->watcher;
	prefFR = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
	eta_f = DEF_ETAF; 
}

void IPComponent::perform(const float dt, const array &flipped)
{

	array thTerm = abs(mThresh - (neuHost.thresholds)) * hpHost->dThdt
		* hpHost->lambda;

	array chg = (tooFast || prefFR > watcher->nu_E)
		&& !(tooFast || prefFR > watcher->nu_E);

	tooFast = prefFR > watcher->nu_E;

	prefFR_Buff = exp(-prefFR/(beta*lowFR)) * tooFast;

	array wayTooSlow = (prefFR < lowFR) && !tooFast;

	prefFR_Buff += (1 + ((log(1+alpha*(prefFR/lowFR - 1))) / alpha)) * !tooFast * !wayTooSlow;

	prefFR_Buff +=  prefFR/lowFR * wayTooSlow;

	zeta = zeta * !chg + ((randn(dim4(1, neuHost.size), f32) * noiseSD) + 1) * chg;

	eta = flipped * eta_f + !flipped * eta_0;

	prefFR_Buff = prefFR + (eta * dt * thTerm * zeta * prefFR_Buff);

	mThresh = (*(host.thresholds) * dt/hpHost->lambda)
		+ ((1 - dt/hpHost->lambda) * mThresh);

}

void IPComponent::pushBuffers() {	prefFR = prefFR_Buff;	}

array IPComponent::getMeanThreshs() {	return mThresh;	} // AF arrays are COW

array IPComponent::getPrefFRs() {	return prefFR;	}

