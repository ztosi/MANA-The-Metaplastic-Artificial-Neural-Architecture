#include <arrayfire.h>

#ifndef PLASTIC_NEURON_H_
#define PLASTIC_NEURON_H_

// Neuron parameters
#define M_RESIST 1 
#define V_REST 0 
#define V_RESET 13.5 
#define I_BG 13.5 
#define INIT_THRESH 15 
#define INHIB_TAU 20 
#define EXCITE_TAU 30 
#define INHIB_REFP 2 
#define EXCITE_REFP 3 
#define SAT_A 10
#define SAT_B 20


// Homeostatic and intrinsic plasticity parameters
#define HP_FINAL 1E5
#define HP_COOL_RATE 1E-5
#define HP_TOLERANCE 0.05
#define IP_FINAL 1E-7
#define IP_COOL_RATE 1E-5
#define HP_INIT 1E7
#define IP_INIT .1
#define PFR_INIT 1
#define C_FP 2
#define C_FD 1
#define LOW_FR 1
#define ALPHA 2
#define BETA 200
#define TAU_A 1E4

	class Neuron {

	public:

		af::array * refP;
		af::array * V_mem;
		af::array * adapt;
		af::array * lastSpkTime;
		af::array * I_e;
		af::array * I_i;

		af::array * thresholds;
		af::array * eiConst;
		af::array * eConst;

		af::array * prefFR;
		af::array * estFR_unScale;
		af::array * estFR_scale;

		float * V_rest;
		float * V_reset;

		unsigned int * spkCnt;
		unsigned int * exInDeg;
		unsigned int * inInDeg;
		unsigned int * outDeg;
		unsigned int * spk;
		unsigned int * tripped;
		unsigned int ** spkTrain;
		unsigned int * network_id;

		Synapse * exIncoming;
		Synapse * inIncoming;
		FanOut * outgoing;

		SpkArriveEvents * exArriveEvts;
		SpkArriveEvents * inArriveEvts;

		unsigned int size;
		int excite : 1;

	};


#endif // PLASTIC_NEURON_H_