#include <arrayfire.h>
#include "AIFNeuron.h"
#include "IPComponent.h"
#include "FREstimator.h"

#ifndef HPCOMPONENT_H_
#define HPCOMPONENT_H_

#define DEF_LAMBDA0 1E7f
#define DEF_LAMBDAF 1E5f
//#define DEF_LAM_D 5E-6f
#define DEF_PFR 1f
#define DEF_NORM_FAC 0.001

using namespace af;

class HPComponent {

	public:

		HPComponent( 	const AIFNeuron &_neuHost,
						const FREstimator* _watcher	);
		HPComponent( 	const AIFNeuron &_neuHost,
						const FREstimator* _watcher,
						const float _lambda_0,
						const float _lambda_f,
						const float _normFac	);

		void perform(const array &pfrs);
		//void perform(const float dt, array tooFast);
		void pushBuffers(const array &hpOn);

	private:

		AIFNeuron &neuHost;
		FREstimator* watcher;

		array dThdt;
		//af::array &exTh;
		//af::array &inTh;

		const float lambda_0;
		const float lambda_f;
		const float normFac;


	friend class DataRecorder;
};


#endif // HPCOMPONENT_H_