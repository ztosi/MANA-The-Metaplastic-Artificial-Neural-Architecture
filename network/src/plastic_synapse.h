#ifndef PLASTIC_SYNAPSE_H_
#define PLASTIC_SYNAPSE_H_

// Synapse parameters
#define I_PSR_TAU 6.0
#define E_PSR_TAU 3.0
#define INIT_WT 0.1
#define WT_SOFTENING 20
#define L_RATE 2E-6
#define SYM_TAU 10		// How quickly EE STDP becomes symmetric
#define MH_SIGMA  30 // Mexican hat
#define MH_A 25		// Mexican hat
#define EE_W_PLUS 5
#define EE_W_MINUS 1
#define EI_W_PLUS 3
#define EI_W_MINUS 3
#define EE_TAU_PLUS 25
#define EE_TAU_MINUS 100
#define EI_TAU_PLUS 60
#define EI_TAU_MINUS 60

	typedef struct {
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
		unsigned int * spkArrived;
		int size;
	} Synapse;

	typedef struct {
		unsigned int src_id;
		unsigned int size;
		unsigned int * spkArrived;
		unsigned int * tar_id;
		unsigned int * index;
		unsigned int * dly_ind_rem; //interleaved spkTrain index then remainder
		//double * estFR_src;
		//unsigned int * dly_rem;
	} FanOut;

	extern Synapse * constructSynapses(Neuron *src, Neuron *tar, int size);

	extern FanOut * constructFanOut(unsigned int src_id, unsigned int* tar_inds, unsigned int size);

	// Only called to perform STDP on spk arrivals, meaning in the EX/Ex
	// case, that (Hebbian) STDP can only cause LTD   
	extern void stdp_ArriveT_ExSrc_ExTar(	double * w,
											double * dw,
											double * lastEvent,
											double * estFR_src,
											double estFR_tar,
											double tar_last_spkT,
											int n,
											double _time);

	// Only called to perform STDP on afferent synapses to a neuron
	// that spiked when those afferents and the neuron in question are
	// excitatory. In the Hebbian case this means that this function
	// can only cause LTP
	extern void stdp_tarSpk_ExSrc_ExTar(double * w,
										double * dw,
										double * lastEvent,
										double * estFR_src,
										double estFR_tar,
										double * last_spk_Arr,
										int n,
										double _time);

	// Only called to perform STDP on spk arrivals, when the neuron
	// in question is inhibitory and the arrivals are from excitatory
	// afferents. This STDP window is anti-Hebbian, and thus only LTP
	// can occur
	extern void stdp_ArriveT_ExSrc_InTar(	double * w,
											double * dw,
											double * lastEvent,
											double tar_last_spkT,
											int n,
											double _time);

	// Only called to perform STDP on afferent synapses to a neuron
	// that spiked when those afferents are excitatory and the neuron
	// in question is inhibitory. In the anti-Hebbian case this means
	// that this function can only cause LTD
	extern void stdp_tarSpk_ExSrc_InTar(double * w,
										double * dw,
										double * lastEvent,
										double * last_spk_Arr,
										int n,
										double _time);

	// Called to perform STDP when the afferent neurons are inhibitory,
	// upon spike arrival at the target cell. Currently the Mexican
	// Hat STDP window is used for all synapses originating from an
	// inhibitory neuron and thus there is no difference between
	// calculated delta w's for excitatory or inhibitory targets.
	extern void stdp_ArriveT_InSrc(	double * w,
									double * dw,
									double * lastEvent,
									double tar_last_spkT,
									int n,
									double _time);

	// Called to perform STDP when the afferent neurons are inhibitory,
	// in response to a spike at the target cell. Currently the Mexican
	// Hat STDP window is used for all synapses originating from an
	// inhibitory neuron and thus there is no difference between
	// calculated delta w's for excitatory or inhibitory targets.
	extern void stdp_tarSpk_InSrc(	double * w,
						   			double * dw,
									double * lastEvent,
									double * last_spk_Arr,
									int n,
									double _time);

	// A helpber function that updates the weights of a synapse based on
	// the time since the last dw change and the current dw. In this way
	// weight updates only occur when either a spike arrives at a target
	// cell or the target cell spikes. That is, a synapse's weight is
	// only updated when knowlede of the weight is needed and is done so
	// using dw and time since dw's last change.
	extern void updateWt(	double * w,
							double * dw,
							double * lastEvent,
							int n,
							double _time);

	//Short term Use, Depression, Facilitation (Tsodyks et al. 1998)
	extern double UDF(	double * w,
						double * last_spk_Arr,
						double * u,
						double * U,
						double * D,
						double * F,
						double * R,
						int n,
						double _time);

#endif // PLASTIC_SYNAPSE_H_
