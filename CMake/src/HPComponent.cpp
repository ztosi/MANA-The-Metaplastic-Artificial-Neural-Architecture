#include <arrayfire.h>
#include "../include/Neuron.hpp"
#include "../include/HPComponent.hpp"

using namespace af;


////////////////////////////////////////////////////////////////////////////
// Homeostatic platicity 
////////////////////////////////////////////////////////////////////////////
HPComponent::HPComponent(   AIFNeuron &_neuHost )

	: neuHost(_neuHost), lambda_0(DEF_LAMBDA0), lambda_f(DEF_LAMBDAF),
	normFac(DEF_NORM_FAC), lambda(DEF_LAMBDA0), lambda_dec(DEF_LAM_D)
{
	dThdt = constant(0, dim4(_neuHost.size, 1), f32);
}

HPComponent::HPComponent(	AIFNeuron &_neuHost,
							const float _lambda_0,
							const float _lambda_f,
							const float _lambda_dec,
							const float _normFac	)

	: neuHost(_neuHost), lambda_0(_lambda_0), lambda_f(_lambda_f),
	normFac(_normFac), lambda(_lambda_0), lambda_dec(_lambda_dec)
{
	dThdt = constant(0, dim4(_neuHost.size, 1), f32);
}

void HPComponent::perform(const array &pfrs) 
{
	dThdt = neuHost.netHost.dt * 
		af::log((neuHost.nu_hat + normFac)/(pfrs + normFac))/lambda;
	lambda -= neuHost.netHost.dt * (lambda - lambda_f) * lambda_dec;
}

//void HPComponent::perform(const float dt, array tooFast)
//{
	//array ratio = ((watcher->nu_E + NORM_FAC)/(*pfrs + NORM_FAC) - 1) * tooFast
	//	+ (1 - (*pfrs + NORM_FAC)/(watcher->nu_E + NORM_FAC)) * !tooFast;
	//ratio = 2/(1 + exp(-ratio)) - 1;
	//dThdt = dt * ratio * (0.25 * randn(dim4(1, hostNeu.size), f32) + 1) / lambda;

//	threshBuff = (host.thresholds) + dThdt;
//}
		
void HPComponent::pushBuffers(const array &hpOn)
{
	neuHost.thresholds += dThdt * hpOn;
}

void HPComponent::pushBuffers()
{
    neuHost.thresholds += dThdt;
}
