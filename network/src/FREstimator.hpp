#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"

#ifndef FRESTIMATOR_H_
#define FRESTIMATOR_H_

#define DEF_NUMER 1E4f

class FREstimator {

	public:

		af::array epsilon;
		af::array nu_eps;
		af::array nu_E;
		af::array* pfrs;

		float numer;

		FREstimator(const array* _pfrs, const uint32_t size);

		af::array estimate(const float dt);

};


#endif // FR_ESTIMATOR_H_