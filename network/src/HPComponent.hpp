#include <arrayfire.h>
#include "AIFNeuron.h"
#include "IPComponent.h"
#include "FREstimator.h"

#ifndef HPCOMPONENT_H_
#define HPCOMPONENT_H_

#define DEF_LAMBDA0 1E5f
#define DEF_LAMBDAF 1E5f
#define DEF_LAM_D 5E-6f
#define DEF_PFR 1f
#define NORM_FAC 0.001

class HPComponent {

	public:

		AIFNeuron &host;
		FREstimator* watcher;

		af::array* threshBuff;
		af::array* pfrs;
		af::array meanTh;
		af::array dThdt;
		//af::array &exTh;
		//af::array &inTh;

		float lambda_0;
		float lambda_f;
		float lamDec;
		float lambda;

		HPComponent(const AIFNeuron &hostNeu);
		HPComponent( 	const AIFNeuron &hostNeu,
						const array* _pfrs,
						const FREstimator* _watcher);

		void perform(const float dt);
		void perform(const float dt, array tooFast);
		void pushBuffers();

};


#endif // HPCOMPONENT_H_