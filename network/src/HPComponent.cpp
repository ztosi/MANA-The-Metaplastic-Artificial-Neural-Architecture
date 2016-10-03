#include <arrayfire.h>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "FREstimator.h"

using namespace af;


////////////////////////////////////////////////////////////////////////////
// Homeostatic platicity 
////////////////////////////////////////////////////////////////////////////
HPComponent::HPComponent(	const AIFNeuron &_neuHost,
							const FREstimator* _watcher)

	: neuHost(_neuHost), lambda_0(DEF_LAMBDA0), lambda_f(DEF_LAMBDAF),
	normFac(DEF_NORM_FAC)
{
	watcher = _watcher;
	dThdt = constant(0, dim4(_neuHost.size, 1), f32);
}

HPComponent::HPComponent(	const AIFNeuron &_neuHost,
							const FREstimator* _watcher,
							const float _lambda_0,
							const float _lambda_f,
							const float _normFac	)

	: neuHost(_neuHost), lambda_0(_lambda_0), lambda_f(_lambda_f),
	normFac(_normFac)
{
	watcher = _watcher;
	dThdt = constant(0, dim4(_neuHost.size, 1), f32);
}

void HPComponent::perform(const float dt, const array pfrs) 
{
	dThdt = dt * af::log((watcher->nu_E + normFac)/(pfrs + normFac));
}

//void HPComponent::perform(const float dt, array tooFast)
//{
	//array ratio = ((watcher->nu_E + NORM_FAC)/(*pfrs + NORM_FAC) - 1) * tooFast
	//	+ (1 - (*pfrs + NORM_FAC)/(watcher->nu_E + NORM_FAC)) * !tooFast;
	//ratio = 2/(1 + exp(-ratio)) - 1;
	//dThdt = dt * ratio * (0.25 * randn(dim4(1, hostNeu.size), f32) + 1) / lambda;

//	threshBuff = (host.thresholds) + dThdt;
//}
		
void HPComponent::pushBuffers()
{
	neuHost.thresholds += dThdt;
}