#include <arrayfire.h>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "FREstimator.h"


IPComponent::IPComponent(const AIFNeuron &hostNeurons, const HPComponent* hpCoop)
	: host(hostNeurons), tooFast(constant(0, dim4(1, hostNeurons.size), f32)),
	zeta(constant(1, dim4(1, hostNeurons.size), f32)),
	eta(constant(DEF_ETA0, dim4(1, hostNeurons.size), f32)), minPFR(DEF_MIN_PFR),
	normFactor(DEF_NRM_FAC), lowFR(DEF_LOW_FR), beta(DEF_BETA), eta_0(DEF_ETA0),
	eta_dec(DEF_ETA_DEC), noiseSD(DEF_NOISE_SD)
{
	hpCooperator = hpCoop;
	watcher = hpCooperator->watcher;
	prefFR = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
	prefFR_Buff = new array(constant(DEF_LOW_FR, dim4(1, hostNeurons.size), f32));
	eta_f = DEF_ETAF; 
}

void IPComponent::perform(const float dt)
{
	array thTerm = abs(hpCoop->meanTh - *(host.thresholds)) * hpCoop->dThdt
		* hpCoop->lambda;

	array chg = (tooFast || prefFR > watcher->nu_E)
		&& !(tooFast || prefFR > watcher->nu_E);

	tooFast = prefFR > watcher->nu_E;

	*prefFR_Buff = exp(-*prefFR/(beta*lowFR)) * tooFast;

	array wayTooSlow = (*prefFR < lowFR) && !tooFast;

	*prefFR_Buff += (1 + ((log(1+alpha*(*prefFR/lowFR - 1))) / alpha)) * !tooFast * !wayTooSlow;

	*prefFR_Buff +=  *prefFR/lowFR * wayTooSlow;

	zeta = zeta * !chg + ((randn(dim4(1, hostNeurons.size), f32) * noiseSD) + 1) * chg;

	*prefFR_Buff = *prefFR + (eta * dt * thTerm * zeta * prefFR_Buff);

}

void IPComponent::pushBuffers()
{
	array* holder = prefFR;
	prefFR = prefFR_Buff;
	prefFR_Buff = holder;
}