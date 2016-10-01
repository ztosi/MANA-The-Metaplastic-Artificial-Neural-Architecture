#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"

using namespace af;

FREstimator::FREstimator(const array* _pfrs, const uint32_t size)
	: epsilon(constant(0, dim4(1, size), f32)), 
		nu_eps(constant(0, dim4(1, size), f32)),
		nu_E(constant(0, dim4(1, size), f32))
{
	pfrs = pfrs;
}

af::array FREstimator::estimate(const float dt)
{
	array tau = numer / sqrt(*pfrs);
	epsilon += dt * (-epsilon/tau) + host.spks;

	nu_eps += dt * (epsilon/tau - nu_eps);
	nu_E = nu_eps * 1000;
	return nu_E; 
}