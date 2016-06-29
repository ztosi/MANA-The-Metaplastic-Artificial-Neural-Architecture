#include <math.h>
#include <stdlib.h>
#include "netparams.h"
#include "plastic_synapse.h"
#include "plastic_neuron.h"
#include "spikeEvents.h"
#include "mkl.h"

static const double EX_TIME_DEC = -TIME_STEP / E_PSR_TAU;
static const double IN_TIME_DEC = -TIME_STEP/I_PSR_TAU;
static const double MEM_RESIST_VAR = M_RESIST;
static const double EX_TS_OV_TC = TIME_STEP/EXCITE_TAU;
static const double IN_TS_OV_TC = TIME_STEP/INHIB_TAU;
static const double ADAPT_COUPLING = -0.1;
static const double NEG_ONE = -1;
static const double INV_TAU_A = 1/TAU_A;
static const double T_STEP = TIME_STEP;
static const double NEG_T_STEP = -TIME_STEP;
static const double NEG_EFR_TIME_C = -TIME_STEP/TAU_A;
static const double ONE_THOUSAND = 1000;
static double IP_CONST = IP_INIT;
static double HP_CONST = HP_INIT;
static const unsigned int CLEAR_MASK = 0x80000000;

static const int DEFAULT_STRIDE = 1;


Neuron * constructNeurons(int excite, int size, int* init_id) {
	Neuron * n = malloc(size * sizeof(Neuron*));
	n->I_e = malloc(size * sizeof(double));
	n->I_i = malloc(size * sizeof(double));
	n->V_mem = malloc(size * sizeof(double));
	n->lastSpkTime = malloc(size * sizeof(double));
	n->threshold = malloc(size * sizeof(double));
	n->adapt = malloc(size * sizeof(double));
	n->prefFR = malloc(size * sizeof(double));
	n->estFR_unScale = malloc(size * sizeof(double));
	n->estFR_scale = malloc(size * sizeof(double));
	n->id = malloc(size * sizeof(unsigned int));
	n->spkCnt = malloc(size * sizeof(unsigned int));
	n->exInDeg = malloc(size * sizeof(unsigned int));
	n->inInDeg = malloc(size * sizeof(unsigned int));
	n->outDeg = malloc(size * sizeof(unsigned int));
	n->spk = malloc(size * sizeof(unsigned int));
	n->tripped = malloc(size * sizeof(unsigned int));
	n->size = &size;
	n->excite = &excite;

	for (int i = 0; i < size; i++) {
		n->id[i] = (*init_id)++;
	}

	return n;
}

//Neuron * constructNeuronsFromNG(int excite) {

//}


inline void annealIPHP() 
{
	HP_CONST += TIME_STEP * HP_COOL_RATE * (HP_FINAL - HP_CONST);
	IP_CONST += TIME_STEP * IP_COOL_RATE * (IP_FINAL - IP_CONST);
}

// Processes incoming events to neurons and acts on incoming synapses only. THIS IS EXTREMELY IMPORTANT.
// This method is **NOT THREAD-SAFE** and must be executed in all threads synchronously before any
// actions are performed on outgoing synapses!
void inwardExNeuronUpdate(Neuron* n, int * forceFire, double * noise, double net_time) 
{
	// Ensure that all triggered neurons have total fan-ins equal to their
	// saturation value 
	normalizeExIncoming(n);

	// Identify incoming excitatory spike events
	for (int i = 0; i < *(n->size); i++) {
		#pragma unroll
		for (int j = 0; j < n->exInDeg[i]; j++) {
			if (n->exIncoming[i]->spkArrived[i] == 1) {
				addSpkArrEvent(n->exArriveEvts[i], i, n->exIncoming[i]);
			}
		}
		#pragma unroll
		for (int j = 0; j < n->inInDeg[i]; j++) {
			if (n->inIncoming[i]->spkArrived[i] == 1) {
				addSpkArrEvent(n->inArriveEvts[i], i, n->inIncoming[i]);
			}
		}
	}

	for (int i = 0; i < *(n->size); i++) {
		if (n->exArriveEvts[i].size != 0) {
			stdp_ArriveT_ExSrc_ExTar(n->exArriveEvts[i]->w,
			 	n->exArriveEvts[i]->dw, n->exArriveEvts[i]->lastEvent,
			 	n->exArriveEvts[i]->estFR_src, n->estFR_scale[i],
			 	n->lastSpkTime[i], n->exArriveEvts[i].size, net_time);
			n->I_e[i] += UDF(n->exArriveEvts[i]->w, n->exArriveEvts[i]->last_spk_Arr,
				n->exArriveEvts[i]->u, n->exArriveEvts[i]->U,
				n->exArriveEvts[i]->D, n->exArriveEvts[i]->F,
				n->exArriveEvts[i]->R, n->exArriveEvts[i].size, net_time);
			copyBackValues(n->exArriveEvts[i], n->exIncoming[i], net_time);
		}
		if (n->inArriveEvts[i].size != 0) {
			stdp_ArriveT_InSrc(n->inArriveEvts[i]->w,
			 	n->inArriveEvts[i]->dw, n->inArriveEvts[i]->lastEvent,
			 	n->lastSpkTime[i], n->inArriveEvts[i]->size, net_time);
			n->I_i[i] += UDF(n->inArriveEvts[i]->w, n->inArriveEvts[i]->last_spk_Arr,
				n->inArriveEvts[i]->u, n->inArriveEvts[i]->U,
				n->inArriveEvts[i]->D, n->inArriveEvts[i]->F,
				n->inArriveEvts[i]->R, n->inArriveEvts[i].size, net_time);
			copyBackValues(n->inArriveEvts[i], n->inIncoming[i], net_time);
		}
	}

	double * temp = malloc(*(n->size) * sizeof(double));
	vdSub(*(n->size), n->I_e, n->I_i, temp);
	daxpy(n->size, &EX_TIME_DEC, n->I_e, &DEFAULT_STRIDE, n->I_e, &DEFAULT_STRIDE);
	daxpy(n->size, &IN_TIME_DEC, n->I_i, &DEFAULT_STRIDE, n->I_i, &DEFAULT_STRIDE);
	vdAdd(*(n->size), temp, noise, temp);
	daxpy(n->size, &ADAPT_COUPLING, n->adapt, &DEFAULT_STRIDE, temp, &DEFAULT_STRIDE);
	double * temp2 = malloc(*(n->size) * sizeof(double));
	dcopy(n->size, n->V_mem, &DEFAULT_STRIDE, temp2, &DEFAULT_STRIDE);
	dscal(n->size, &NEG_ONE, temp2, &DEFAULT_STRIDE);
	addScalarToVector(*(n->size), temp2, V_REST);
	daxpy(n->size, &MEM_RESIST_VAR, temp, &DEFAULT_STRIDE, temp2, &DEFAULT_STRIDE);
	daxpy(n->size, &EX_TS_OV_TC, temp2, &DEFAULT_STRIDE, n->V_mem, &DEFAULT_STRIDE);
	free(temp);
	free(temp2);
	temp = NULL;
	temp2 = NULL;

	#pragma unroll
	for (int i = 0; i < n; i++) {
		if ((n->V_mem[i] >= n->threshold[i] && ((net_time-n->lastSpkTime[i]) > EXCITE_REFP)) 
			|| forceFire[i]) {
			
			n->spk[i] = 1;
			n->adapt[i] += 1;
			n->spkCnt[i] +=1;
			n->lastSpkTime[i] = net_time;
			n->V_mem = V_RESET;
			stdp_tarSpk_ExSrc_ExTar(n->exIncoming[i]->w, n->exIncoming[i]->dw,
				n->exIncoming[i]->lastEvent, n->exIncoming[i]->estFR_src,
				n->estFR_scale[i], n->exIncoming[i]->lastSpkArrTime,
				n->exInDeg[i], net_time);
			stdp_tarSpk_InSrc(n->inIncoming[i]->w, n->inIncoming[i]->dw,
				n->inIncoming[i]->lastEvent, n->inIncoming[i]->lastSpkArrTime,
				n->inInDeg[i], net_time);

		} else {
			n->spk[i] = 0;
		}
	}
}


void inwardInNeuronUpdate(Neuron* n, int * forceFire, double * noise, double net_time) {
		// Ensure that all triggered neurons have total fan-ins equal to their
	// saturation value 
	normalizeExIncoming(n);

	// Identify incoming excitatory spike events
	for (int i = 0; i < *(n->size); i++) {
		#pragma unroll
		for (int j = 0; j < n->exInDeg[i]; j++) {
			if (n->exIncoming[i]->spkArrived[i] == 1) {
				addSpkArrEvent(n->exArriveEvts[i], i, n->exIncoming[i]);
			}
		}
		#pragma unroll
		for (int j = 0; j < n->inInDeg[i]; j++) {
			if (n->inIncoming[i]->spkArrived[i] == 1) {
				addSpkArrEvent(n->inArriveEvts[i], i, n->inIncoming[i]);
			}
		}
	}

	for (int i = 0; i < *(n->size); i++) {
		if (n->exArriveEvts[i].size != 0) {
			stdp_ArriveT_ExSrc_InTar(n->exArriveEvts[i]->w,
			 	n->exArriveEvts[i]->dw, n->exArriveEvts[i]->lastEvent,
			 	n->lastSpkTime[i], n->exArriveEvts[i].size, net_time);
			n->I_e[i] += UDF(n->exArriveEvts[i]->w, n->exArriveEvts[i]->last_spk_Arr,
				n->exArriveEvts[i]->u, n->exArriveEvts[i]->U,
				n->exArriveEvts[i]->D, n->exArriveEvts[i]->F,
				n->exArriveEvts[i]->R, n->exArriveEvts[i].size, net_time);
			copyBackValues(n->exArriveEvts[i], n->exIncoming[i], net_time);
		}
		if (n->inArriveEvts[i].size != 0) {
			stdp_ArriveT_InSrc(n->inArriveEvts[i]->w,
			 	n->inArriveEvts[i]->dw, n->inArriveEvts[i]->lastEvent,
			 	n->lastSpkTime[i], n->inArriveEvts[i].size, net_time);
			n->I_i[i] += UDF(n->inArriveEvts[i]->w, n->inArriveEvts[i]->last_spk_Arr,
				n->inArriveEvts[i]->u, n->inArriveEvts[i]->U,
				n->inArriveEvts[i]->D, n->inArriveEvts[i]->F,
				n->inArriveEvts[i]->R, n->inArriveEvts[i].size, net_time);
			copyBackValues(n->inArriveEvts[i], n->inIncoming[i], net_time);
		}
	}

	double * temp = malloc(*(n->size) * sizeof(double));
	vdSub(*(n->size), n->I_e, n->I_i, temp);
	daxpy(n->size, &EX_TIME_DEC, n->I_e, &DEFAULT_STRIDE, n->I_e, &DEFAULT_STRIDE);
	daxpy(n->size, &IN_TIME_DEC, n->I_i, &DEFAULT_STRIDE, n->I_i, &DEFAULT_STRIDE);
	vdAdd(*(n->size), temp, noise, temp);
	daxpy(n->size, &ADAPT_COUPLING, n->adapt, &DEFAULT_STRIDE, temp, &DEFAULT_STRIDE);
	double * temp2 = malloc(*(n->size) * sizeof(double));
	dcopy(n->size, n->V_mem, &DEFAULT_STRIDE, temp2, &DEFAULT_STRIDE);
	dscal(n->size, &NEG_ONE, temp2, &DEFAULT_STRIDE);
	addScalarToVector(*(n->size), temp2, V_REST);
	daxpy(n->size, &MEM_RESIST_VAR, temp, &DEFAULT_STRIDE, temp2, &DEFAULT_STRIDE);
	daxpy(n->size, &IN_TS_OV_TC, temp2, &DEFAULT_STRIDE, n->V_mem, &DEFAULT_STRIDE);
	free(temp);
	free(temp2);
	temp = NULL;
	temp2 = NULL;

	#pragma unroll
	for (int i = 0; i < n; i++) {
		if ((n->V_mem[i] >= n->threshold[i] && ((net_time-n->lastSpkTime[i]) > INHIB_REFP)) 
			|| forceFire[i]) {
			n->spk[i] = 1;
			n->adapt[i] += 1;
			n->spkCnt[i] +=1;
			n->lastSpkTime[i] = net_time;
			n->V_mem = V_RESET;
			stdp_tarSpk_ExSrc_ExTar(n->exIncoming[i]->w, n->exIncoming[i]->dw,
				n->exIncoming[i]->lastEvent, n->exIncoming[i]->estFR_src,
				n->estFR_scale[i], n->exIncoming[i]->lastSpkArrTime,
				n->exInDeg[i], net_time);
			stdp_tarSpk_InSrc(n->inIncoming[i]->w, n->inIncoming[i]->dw,
				n->inIncoming[i]->lastEvent, n->inIncoming[i]->lastSpkArrTime,
				n->inInDeg[i], net_time);

		} else {
			n->spk[i] = 0;
		}
	}
}

void normalizeExIncoming(Neuron* n) {

	// TODO: More efficient implementation of this...
	for (int i = 0; i < *(n->size); ++i)
	{
		double inSum = dasum(n->exIncoming[i]->size, n->exIncoming[i]->w, &DEFAULT_STRIDE);
		double satVal = ((n->prefFR[i] + 1) / (n->estFR_scale[i] + 1)) * n->prefFR[i] * SAT_A + SAT_B;
		if (n->tripped[i] || inSum > satVal) {
			n->tripped[i] = 1;
			if (*(n->exIncoming[i]->size) > 4) {
				for(int j = 0; j < *(n->exIncoming[i]->size); j+=4) {
					n->exIncoming[i]->w[j] = satVal * n->exIncoming[i]->w[j] / inSum;
					n->exIncoming[i]->w[j+1] = satVal * n->exIncoming[i]->w[j+1] / inSum;
					n->exIncoming[i]->w[j+2] = satVal * n->exIncoming[i]->w[j+2] / inSum;
					n->exIncoming[i]->w[j+3] = satVal * n->exIncoming[i]->w[j+3] / inSum;
				}
				for (int j = 4 * (int) (*(n->exIncoming[i]->size)/4); j < n->exIncoming[i]->size; j++) {
					n->exIncoming[i]->w[j] = satVal * n->exIncoming[i]->w[j] / inSum;
				}
			} else {
				for (int j = 0; j < *(n->exIncoming[i]->size); j++) {
					n->exIncoming[i]->w[j] = satVal * n->exIncoming[i]->w[j] / inSum;
				}
			}
		}
	}

}

void outwardUpdateNeuron(Neuron * n, double * noise) {
	updateSpkTrain(n);
	neuronalPlasticity(n, noise);
}

// Execute neuronal plasticity changes on a chunk of neurons (called by a worker thread)
// for the neurons its responsible for updating
void neuronalPlasticity(Neuron* n, double * noise) {

	//Calculate the estimated firing rate
	double * efrun_temp = malloc(*(n->size) * sizeof(double));
	dcopy(n->size, n->estFR_unScale, &DEFAULT_STRIDE, efrun_temp, &DEFAULT_STRIDE);
	dscal(n->size, &NEG_ONE, efrun_temp, &DEFAULT_STRIDE);
	daxpy(n->size, &INV_TAU_A, n->adapt, &DEFAULT_STRIDE, efrun_temp, &DEFAULT_STRIDE); 
	daxpy(n->size, &T_STEP, efrun_temp, &DEFAULT_STRIDE, n->estFR_unScale, &DEFAULT_STRIDE);
	dcopy(n->size, n->estFR_unScale, &DEFAULT_STRIDE, n->estFR_scale, &DEFAULT_STRIDE);
	dscal(n->size, &ONE_THOUSAND, n->estFR_scale, DEFAULT_STRIDE);
	daxpy(n->size, &NEG_EFR_TIME_C, n->adapt, &DEFAULT_STRIDE, n->adapt, &DEFAULT_STRIDE);
	free(efrun_temp);
	efrun_temp = NULL;

	// Update the threshold based on the estimated firing rate
	double * delta_fr = malloc(*(n->size) * sizeof(double));
	vsSub(*(n->size), n->estFR_scale, n->prefFR, delta_fr);
	double * fac = malloc(*(n->size) * sizeof(double));
	#pragma unroll
	for(int i = 0; i < *(n->size); i++) {
		fac[i] = n->estFR_scale[i] > n->prefFR[i] ? n->estFR_scale[i] + 1E-6
			: n->prefFR[i] + 1E-6;
	}
	dscal(n->size, &HP_CONST, fac);
	vdDiv(*(n->size), delta_fr, fac, fac);
	vdExp(*(n->size), fac, fac);
	#pragma unroll
	for(int i = 0; i < *(n->size); i++) {
		fac[i] = fac[i] - 1;
	}
	vdMul(*(n->size), n->threshold, fac, fac);
	daxpy(n->size, &T_STEP, fac, &DEFAULT_STRIDE, n->threshold, &DEFAULT_STRIDE);
	free(fac);
	fac = NULL;

	// Perform intrinsic plasticity (preferred firing rate update)
	// TODO: find more efficient implementation
	#pragma unroll
	for (int i = 0; i < *(n->size); i++) {
		delta_fr[i] = (abs(delta_fr[i]) > (0.05 * n->prefFR[i])) ? delta_fr[i] : 0;
		if (delta_fr[i] > 0) {
			delta_fr[i] = C_FP * fastexp(-n->prefFR[i] / (BETA * LOW_FR));
		} else if (delta_fr[i] < 0) {
			if (n->prefFR[i] <= LOW_FR) {
				delta_fr = -C_FD * (n->prefFR[i] / LOW_FR);
			} else {
				delta_fr = -C_FD *  (1 + (fastlog(1 + (ALPHA
				* ((n->prefFR[i] / LOW_FR) - 1))) / ALPHA));
			}
		}
	}
	vdMul(*(n->size), delta_fr, noise, delta_fr);
	double IP_TS_C = IP_CONST * TIME_STEP;
	daxpy(n->size, &IP_TS_C, delta_fr, &DEFAULT_STRIDE, n->prefFR, &DEFAULT_STRIDE);

	free(delta_fr);
	delta_fr = NULL;
}

inline void updateSpkTrain(Neuron *n) {
	for (int i = 0; i < *(n->size); i++) {
		n->spkTrain[i][7] = (n->spkTrain[i][7] << 1) | ((n->spkTrain[i][6] & CLEAR_MASK) >> 31);
		n->spkTrain[i][6] = (n->spkTrain[i][6] << 1) | ((n->spkTrain[i][5] & CLEAR_MASK) >> 31);
		n->spkTrain[i][5] = (n->spkTrain[i][5] << 1) | ((n->spkTrain[i][4] & CLEAR_MASK) >> 31);
		n->spkTrain[i][4] = (n->spkTrain[i][4] << 1) | ((n->spkTrain[i][3] & CLEAR_MASK) >> 31);
		n->spkTrain[i][3] = (n->spkTrain[i][3] << 1) | ((n->spkTrain[i][2] & CLEAR_MASK) >> 31);
		n->spkTrain[i][2] = (n->spkTrain[i][2] << 1) | ((n->spkTrain[i][1] & CLEAR_MASK) >> 31);
		n->spkTrain[i][1] = (n->spkTrain[i][1] << 1) | ((n->spkTrain[i][0] & CLEAR_MASK) >> 31);
		n->spkTrain[i][0] = (n->spkTrain[i][0] << 1) | n->spk[i];
		#pragma unroll
		unsigned int k = 0;
		for (int j = 0; j < n->outDeg[i]; j++) {
			n->outgoing[i].spkArrived[j] = (n->spkTrain[i][n->outgoing[i].dly_ind_rem[k]]
				>> n->outgoing[i].dly_ind_rem[k+1]) & CLEAR_MASK;
			k+=2;
		}
	}
}
