#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"

using namespace af;

FREstimator::FREstimator(	const array &_neuHost,
							const float _initVal,
							const float _tau	)
	: neuHost(_neuHost),
	epsilon(constant(0, dim4(1, _neuHost.size), f32)), 
	nu_eps(constant(_initVal/1000f, dim4(1, _neuHost.size), f32)),
	nu_E(constant(_initVal, dim4(1, _neuHost.size), f32))
{
	tau = _tau;
}

void FREstimator::estimate(const float dt)
{
	epsilon += dt * (-epsilon/tau) + host.spks;
	nu_eps += dt * (epsilon/tau - nu_eps);
	nu_E = nu_eps * 1000;
}

DynFREstimator::DynFREstimator(	const array &_neuHost,
								const IPComponent &_ipHost
								const float _initVal,
								const float _tau	)
	: neuHost(_neuHost), ipHost(_ipHost),
	epsilon(constant(0, dim4(_neuHost.size, 1), f32)), 
	nu_eps(constant(_initVal/1000f, dim4(_neuHost.size, 1), f32)),
	nu_E(constant(_initVal, dim4(_neuHost.size, 1), f32)),
{
	tau = _tau;
	pfrs = constant(_initVal, dim4(_neuHost.size, 1), f32);
}

void FREstimator::estimate(const float dt)
{

	array tauAF = tau/af::sqrt(pfrs);

	epsilon += dt * (-epsilon/tauAF) + host.spks;
	nu_eps += dt * (epsilon/tauAF - nu_eps);
	nu_E = nu_eps * 1000;
}