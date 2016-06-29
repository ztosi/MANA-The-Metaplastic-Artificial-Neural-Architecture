#include "spkEvents.h"
#ifndef NETCOMPONENTS_H_
#define NETCOMPONENTS_H_	

	struct Neurons;
	struct Synapses;
	typedef struct Neurons Neuron;
	typedef struct Synapses Synapse;


	struct Neuron {
		unsigned int size;
		unsigned int id;
		double * I_e;
		double * I_i; 
		double * V_mem;
		double * lastSpkTime;
		unsigned int * spkCnt;
		double * threshold;
		double * adapt;
		double * prefFR;
		double * estFR_unScale;
		double * estFR_scale;
		unsigned int excite : 1; // 0 excite or 1 inhib
		unsigned int * spk : 1;
		unsigned int * input : 1;
		unsigned int * tripped : 1;
		unsigned int * exInDeg;
		unsigned int * inInDeg;
		unsigned int * outDeg;

		Synapse * exIncoming;
		Synapse * inIncoming;
		Synapse * outgoing;

		SpkArriveEvents * exArriveEvts;
		SpkArriveEvents * inArriveEvts;

	};

	struct Synapse {
		double * w;
		double * dw;
		double * lastEvent;
		double * lastSpkArrTime;
		double * estFR_src;
		double * U;
		double * u;
		double * D;
		double * F;
		double * R;
		unsigned char * spkArrived;
		unsigned char * dly_ind;
		unsigned char * dly_remain;
		Neuron *tar;
		Neuron *src;
	};

	struct SpkEvents {
		Synapse* events;
		int t;
	};


	static Neuron * constructNeuron(int excite, int initialIncoming);
	static Synapse * constructSynapse(Neuron *src, Neuron *tar);

	static inline void annealIPHP();

	static inline void calcEstFR(Neuron *n);

	static inline void updateThresh(Neuron *n);

	static inline void updatePFR(Neuron *n);

	static inline void updateTHandPFR(Neuron *n);

	inline void updateNeuron(Neuron *n, const double sim_time, const int forceFire);

	void synchronizeNeurons(Neuron **n, int tasksize);

	static inline void updateLocalSpkTrain(Neuron *n);

	inline void updateSynase(Synapse *s, const double sim_time);

	static inline void symmetricSTDP(Synapse *s);

	inline void updateTask(Neuron **n, const int tasksize, const double sim_time);

	int assignID();

#endif // NETCOMPONENTS_H_