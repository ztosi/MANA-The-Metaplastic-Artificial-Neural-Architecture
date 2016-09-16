#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "SynActors.h"

#ifndef SOING_UNIT_H_
#define SOING_UNIT_H_

#define DEF_NUMER 1E4f

class SOing_Unit {

	public:

		AIFNeuron* neurons;
		HPSynScaler* synScale;
		SynNormalizer* synNorm;
		HPComponent* homPlas;
		IPComponent* intPlas;
		FREstimator* watcher;


		float numer;

		FREstimator(const array* _pfrs, const uint32_t size);

		af::array estimate(const float dt);

};


#endif // SOING_UNIT_H_