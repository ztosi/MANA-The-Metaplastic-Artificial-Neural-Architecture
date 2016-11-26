#include <arrayfire.h>

#ifndef IPCOMPONENT_H_
#define IPCOMPONENT_H_

using namespace af;

const float DEF_MIN_PFR = 0.01;
const float DEF_LOW_FR = 1.0;
const float DEF_BETA = 25.0;
const float DEF_ALPHA = 5;
const float DEF_ETA0 = 0.01;
const float DEF_ETAF = 1E-6;
const float DEF_ETA_DEC = 5E-6;
const float DEF_NOISE_SD = 0.7;

class HPComponent;
class AIFNeuron;

class IPComponent {

	public:
		
		IPComponent(	AIFNeuron &_neuHost,
						HPComponent* _hpHost	);

		IPComponent(	AIFNeuron &_neuHost,
						HPComponent* _hpHost,
						const float _eta_0,
						const float _eta_f,
						const float _eta_dec	);

		//TODO: Make constructor for the const values not already assignable in a constructor

		//void perform(const float dt, const array &flipped);
		void perform();
		void pushBuffers();
		void calcEstFRsForHost();
		array getPrefFRs();


	private:	
		
		AIFNeuron &neuHost;
		HPComponent* hpHost;
		//FREstimator* watcher; 

		//array tooFast;
		array* prefFR;
		array* prefFR_Buff;
		//array mThresh;
		//array eta;
		float eta;
		//array zeta;

		const float minPFR; //Hz 
		const float lowFR; // Hz
		const float beta;
		const float alpha;
		const float eta_0;
		const float eta_f;
		const float eta_dec;
		const float noiseSD;


	friend class DataRecorder;
	friend class MANA_Module;

};


#endif // IPCOMPONENT_H_
