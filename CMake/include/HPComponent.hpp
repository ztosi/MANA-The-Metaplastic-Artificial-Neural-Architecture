#include <arrayfire.h>
#ifndef HPCOMPONENT_H_
#define HPCOMPONENT_H_

using namespace af;

const float DEF_LAMBDA0 = 1E4;
const float DEF_LAMBDAF = 1E5;
const float DEF_LAM_D = 5E-6;
const float DEF_PFR = 1;
const float DEF_NORM_FAC = 0.001;

class AIFNeuron;

class HPComponent {

	public:

		HPComponent( 	AIFNeuron &_neuHost );
		HPComponent( 	AIFNeuron &_neuHost,
						//const FREstimator* _watcher,
						const float _lambda_0,
						const float _lambda_f,
						const float _lambda_dec,
						const float _normFac	);

		void perform(const array &pfrs);
		//void perform(const float dt, array tooFast);
		void pushBuffers(const array &hpOn);

	private:

		AIFNeuron &neuHost;
		//FREstimator* watcher;

		array dThdt;
		//af::array &exTh;
		//af::array &inTh;
        float lambda;
		float lambda_f;

		const float lambda_0;
		const float lambda_dec;
		const float normFac;

	friend class DataRecorder;
    
};


#endif // HPCOMPONENT_H_
