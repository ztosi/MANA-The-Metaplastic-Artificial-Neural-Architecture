#include <arrayfire.h>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "FREstimator.h"

#ifndef IPCOMPONENT_H_
#define IPCOMPONENT_H_

#define DEF_MIN_PFR 0.01

#define DEF_LOW_FR 1
#define DEF_BETA 25
#define DEF_ALPHA 5
#define DEF_ETA0 0.1
#define DEF_ETAF 1E-6
#define DEF_ETA_DEC 2.5E-6
#define DEF_NOISE_SD 0.25

class IPComponent {

	public:

		AIFNeuron host;
		HPComponent* hpCooperator;
		FREstimator* watcher; 

		af::array tooFast;
		af::array* prefFR;
		af::array* prefFR_Buff;
		af::array eta;
		float eta_f;

	private:	
		
		af::array zeta;

		const float minPFR; //Hz 
		const float normFactor;
		const float lowFR; // Hz
		const float beta;
		const float alpha;
		const float eta_0;
		const float eta_dec;
		const float noiseSD;

		IPComponent(const AIFNeuron &hostNeurons,
					const HPComponent* hpCoop);
		// TODO: Make constructor that takes in initial vals for all the const vars


		void perform(const float dt);
		void pushBuffer();
};


#endif // IPCOMPONENT_H_