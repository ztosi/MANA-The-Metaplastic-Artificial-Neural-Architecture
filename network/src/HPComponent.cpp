#include <arrayfire.h>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "FREstimator.h"

using namespace af;


////////////////////////////////////////////////////////////////////////////
// Homeostatic platicity 
////////////////////////////////////////////////////////////////////////////
HPComponent::HPComponent(const AIFNeuron &hostNeu)
	: host(hostNeu), meanTh(constant(INIT_THRESH, dim4(1, hostNeu.size), f32)),
	dThdt(constant(0, dim4(1, hostNeu.size()), f32))
{
	ipCoperator = NULL;
	*threshBuff = array(dim4(1, hostNeu.size), f32);
	*pfrs = constant(DEF_PFR, dim4(1, hostNeu.size), f32);
	watcher = new FREstimator(pfrs);
	lambda_0 = DEF_LAMBDA0;
	lambda_f = DEF_LAMBDAF;
	lamDec = DEF_LAM_D;
	lambda = DEF_LAMBDA0;
}

HPComponent::HPComponent(	const AIFNeuron &hostNeu,
							const array* _pfrs,
							const FREstimator* _watcher)
	: host(hostNeu), meanTh(constant(INIT_THRESH, dim4(1, hostNeu.size), f32)),
	dThdt(constant(0f, dim4(1, hostNeu.size()), f32))
{
	*threshBuff = array(dim4(1, hostNeu.size), f32);
	pfrs = _pfrs;
	watcher = _watcher;
	lambda_0 = DEF_LAMBDA0;
	lambda_f = DEF_LAMBDAF;
	lamDec = DEF_LAM_D;
	lambda = DEF_LAMBDA0;
}

void HPComponent::perform(const float dt) 
{
	array tooFast = watcher->nu_E > pfrs;
	perform(dt, tooFast);
}

void HPComponent::perform(const float dt, array tooFast)
{
	array ratio = ((watcher->nu_E + NORM_FAC)/(*pfrs + NORM_FAC) - 1) * tooFast
		+ (1 - (*pfrs + NORM_FAC)/(watcher->nu_E + NORM_FAC)) * !tooFast;
	ratio = 2/(1 + exp(-ratio)) - 1;
	dThdt = dt * ratio * (0.25 * randn(dim4(1, hostNeu.size), f32) + 1) / lambda;
	meanTh = meanTh * (1-(dt/lambda)) + *(host.thresholds) * (dt/lambda);
	*threshBuff = *(host.thresholds) + dThdt;
}
		
void HPComponent::pushBuffers()
{
	array* holder = host.thresholds;
	host.thresholds = threshBuff;
	threshBuff = holder;
}