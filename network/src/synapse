#include <math.h>
#include <stdlib.h>
#include "plastic_synapse.h"
#include "plastic_neuron.h"
#include "spikeEvents.h"
#include "mkl.h"

static const double EE_TAU_DELTA = EE_TAU_PLUS - EE_TAU_MINUS;
static const double DEFAULT_STRIDE = 1;
static const double INV_SYM_TAU = 1.0/SYM_TAU;
static const double INV_EE_TAU_P = 1.0/EE_TAU_PLUS;
static const double INV_EE_TAU_M = 1.0/EE_TAU_MINUS;
//static const double INV_EI_TAU_P = 1.0/EI_TAU_PLUS;
//static const double INV_EI_TAU_M = 1.0/EI_TAU_MINUS;
static const double EE_WP = EE_W_PLUS;
static const double NEG_INV_WT_SOFT = -1.0/WT_SOFTENING;
static const double NEG_EI_WM = -EI_W_MINUS;
static const double ETA = L_RATE;
static const double NEG_ONE = -1.0;
static const unsigned int CLEAR_MASK = 0x00000001;



// Src should be
void constructSynapses(Neuron *src, Neuron *tar, int tarIndex) {
	unsigned int size;
	if (src == tar) {
		size = src->size - 1;
	} else {
		size = src->size;
	}
	//if (src->outgoing == NULL) {
	//	src->outgoing = (**SingleSynapse) malloc(size * sizeof(*SingleSynapse));
	//} else {
	//	src->outgoing = (**SingleSynapse) realloc(outgoing, startInd + *(tar->size));
	//}

	//for (int i = 0; i < num_src; i++) {
	//	int startInd = src->outDeg[i]


	//}

	for (int j = 0; j < tar->size; j++) {
		Synapse * s = malloc(size * sizeof(Synapse*));
		s->w = malloc(size * sizeof(double));
		s->dw = malloc(size * sizeof(double));
		s->lastEvent = malloc(size * sizeof(double));
		s->lastSpkArrTime = malloc(size * sizeof(double));
		s->estFR_src = malloc(size * sizeof(double));
		s->U = malloc(size * sizeof(double));
		s->u = malloc(size * sizeof(double));
		s->D = malloc(size * sizeof(double));
		s->F = malloc(size * sizeof(double));
		s->R = malloc(size * sizeof(double));
		s->spkArrived = malloc(size * sizeof(unsigned int));
		s->size = size;
	}


}

FanOut * constructFanOut(unsigned int src_id, unsigned int* tar_inds, unsigned int size) {
	FanOut * syn = malloc(sizeof(*FanOut));
	syn->src_id = src_id;
	syn->size = size;
	syn->spkArrived = (unsigned int*)calloc(size, sizeof(unsigned int));
	syn->tar_id = tar_inds;
	syn->index = (unsigned int*)calloc(size, sizeof(unsigned int)); // TBD upon net construction
	syn->dly_ind_rem = (unsigned int*)calloc(2 * size, sizeof(unsigned int)); // Interleaved, TBD " "
	//syn->estFR_src = (double*)calloc(size, sizeof(double));
	return syn;
}

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
//
//
//		START STDP CODE
//
//
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////



// Only called to perform STDP on spk arrivals, meaning in the EX/Ex
// case, that (Hebbian) STDP can only cause LTD   
void stdp_ArriveT_ExSrc_ExTar(	double * w,
								double * dw,
								double * lastEvent,
								double * estFR_src,
								double estFR_tar,
								double tar_last_spkT,
								int n,
								double _time)
{
	// Increment w, based on time since last dw change and dw
	updateWt(w, dw, lastEvent, n);

	// Find new dw (*everything* in function beyond here)
	double delta_t = -(_time - tar_last_spkT);

	// Change WM and tauM based on symmetric firing 
	estFR_tar = ((estFR_tar > 50) ? 50 : estFR_tar) - 100;
	#pragma unroll
	for(int i = 0; i < n; i++) {
		estFR_src[i] = ((estFR_src[i] > 50) ? 50 : estFR_src[i]) + estFR_tar;
	}
	dscal(&n, &INV_SYM_TAU, estFR_src, &DEFAULT_STRIDE);
	vdExp(n, estFR_src, estFR_src);
	double w_delta = -EE_W_PLUS - EE_W_MINUS;
	double * wm = malloc(n * sizeof(double));
	dcopy(&n, estFR_src, &DEFAULT_STRIDE, wm, &DEFAULT_STRIDE);
	dscal(&n, &w_delta, wm, &DEFAULT_STRIDE);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		wm[i] = -1*(wm[i] + EE_W_MINUS); // makes dw negative
	}
	double * tm = malloc(n * sizeof(double));
	dcopy(&n, estFR_src, &DEFAULT_STRIDE, tm, &DEFAULT_STRIDE);
	dscal(&n, &EE_TAU_DELTA, tm, &DEFAULT_STRIDE);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		tm[i] = tm[i] + EE_TAU_MINUS;
	}
	vdInv(n, tm, tm);
	dscal(&n, &delta_t, tm, &DEFAULT_STRIDE);
	vdExp(n, tm, tm);
	vdSub(n, tm, estFR_src, tm);
	vdMul(n, tm, wm, dw);
	free(wm);
	free(tm);
	wm = NULL;
	tm = NULL;
}

/**
 * Writes to w and dw.
 */
void stdp_tarSpk_ExSrc_ExTar(	double * w,
								double * dw,
								double * lastEvent,
								double * estFR_src,
								double estFR_tar,
								double * last_spk_Arr,
								int n,
								double _time)
{
	// Increment w, based on time since last dw change and dw
	updateWt(w, dw, lastEvent, n);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		lastEvent[i] = _time;
	}

	// Find new dw (*everything* in function beyond here)
	double t_post = _time; // Triggered by target Spike
	double * delta = malloc(n * sizeof(double));
	#pragma unroll
	for (int i = 0; i < n; i++) {
		delta[i] = last_spk_Arr[i] - t_post;
	}

	// Evaluate the value to subtract in relation to src/tar Firing Rate
	estFR_tar = ((estFR_tar > 50) ? 50 : estFR_tar) - 100;
	#pragma unroll
	for(int i = 0; i < n; i++) {
		estFR_src[i] = ((estFR_src[i] > 50) ? 50 : estFR_src[i]) + estFR_tar;
	}
	dscal(&n, &INV_SYM_TAU, estFR_src, &DEFAULT_STRIDE);
	vdExp(n, estFR_src, estFR_src);
	
	// Perform the main exponential & multiply and subtract value from above
	dscal(&n, &INV_EE_TAU_P, delta, &DEFAULT_STRIDE);
	vdExp(n, delta, delta);
	dscal(&n, &EE_WP, delta, &DEFAULT_STRIDE);
	vdSub(n, delta, estFR_src, delta);

	// Weight softening
	dcopy(&n, w, &DEFAULT_STRIDE, dw, &DEFAULT_STRIDE);
	dscal(&n, &NEG_INV_WT_SOFT, dw, &DEFAULT_STRIDE);
	vdExp(n, dw, dw);
	
	// Multiply delta, the main STDP change by weight sofenting (dw) and
	// write back to dw
	vdMul(n, delta, dw, dw);

	free(delta);
	delta = NULL;
}



// Because Ex->In connections are anti-Hebbian, and this is only called for
// spike arrivals at an inhibitory target, only LTP can occur.
void stdp_ArriveT_ExSrc_InTar(	double * w,
								double * dw,
								double * lastEvent,
								double tar_last_spkT,
								int n,
								double _time)
{
	updateWt(w, dw, lastEvent, n);

	// Find new dw
	double delta_t = tar_last_spkT - _time;
	double delta_w = EI_W_MINUS * exp(delta_t / EI_TAU_PLUS);
	dcopy(&n, w, &DEFAULT_STRIDE, dw, &DEFAULT_STRIDE);
	dscal(&n, &NEG_INV_WT_SOFT, dw, &DEFAULT_STRIDE);
	vdExp(n, dw, dw);
	dscal(&n, &delta_w, dw, &DEFAULT_STRIDE);
}

// 
 void stdp_tarSpk_ExSrc_InTar(	double * w,
								double * dw,
								double * lastEvent,
								double * last_spk_Arr,
								int n,
								double _time)
{
	updateWt(w, dw, lastEvent, n);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		lastEvent[i] = _time;
	}
	// Find new dw
	dcopy(&n, last_spk_Arr, &DEFAULT_STRIDE, dw, &DEFAULT_STRIDE);
	addScalarToVector(n, dw, - _time);
	dscal(&n, &INV_EE_TAU_M, dw, &DEFAULT_STRIDE);
	vdExp(n, dw, dw);
	dscal(&n, &NEG_EI_WM, dw, &DEFAULT_STRIDE);
	addScalarToVector(n, dw, .25);
}


void stdp_ArriveT_InSrc(double * w,
						double * dw,
						double * lastEvent,
						double tar_last_spkT,
						int n,
						double _time)
{
	updateWt(w, dw, lastEvent, n);

	// Find new dw
	double delta_t = tar_last_spkT - _time;
	double delta_w = mexican_hat(delta_t);
	if (delta_w > 0) {
		dcopy(&n, w, &DEFAULT_STRIDE, dw, &DEFAULT_STRIDE);
		dscal(&n, &NEG_INV_WT_SOFT, dw, &DEFAULT_STRIDE);
		vdExp(n, dw, dw);
		dscal(&n, &delta_w, dw, &DEFAULT_STRIDE);
	} else {
		#pragma unroll
		for (int i = 0; i < n; i++) {
			dw[i] = delta_w;
		}
	}
}

void stdp_tarSpk_InSrc(	double * w,
						double * dw,
						double * lastEvent,
						double * last_spk_Arr,
						int n,
						double _time)
{
	updateWt(w, dw, lastEvent, n);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		lastEvent[i] = _time;
	}
	// Find new dw
	dcopy(&n, last_spk_Arr, &DEFAULT_STRIDE, dw, &DEFAULT_STRIDE);
	addScalarToVector(n, dw, -_time);
	mexican_hat_vec(n, dw);
	double * temp = malloc(n * sizeof(double));
	#pragma unroll
	for (int i = 0; i < n; i++) {
		temp[i] = (double)((dw[i] > 0) & CLEAR_MASK);
	}
	vdMul(n, temp, w, temp);
	dscal(&n, &NEG_INV_WT_SOFT, temp, &DEFAULT_STRIDE);
	vdMul(n, temp, dw, dw);

	free(temp);
	temp = NULL;
}

// Updates w based on dw (* L_RATE) and the time since the last event
inline void updateWt(	double * w,
						double * dw,
						double * lastEvent,
						int n,
						double _time) 
{
	double * temp = (double *) malloc(n * sizeof(double));
	// Increment w, based on time since last dw change and dw
	#pragma unroll
	for (int i = 0; i < n; i++) {
		temp[i] = _time - lastEvent[i];
	}
	vdMul(n, temp, dw, dw);
	daxpy(&n, &ETA, dw, &DEFAULT_STRIDE, w, &DEFAULT_STRIDE);
	free(temp);
	temp = NULL;
	// Now performed in "copyBackValues"
	//#pragma unroll 
	//for (int i = 0; i < n; i++) {
	//	lastEvent[i] = time;
	//}
}

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
//
//
//	END STDP CODE
//
//
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

double UDF(	double * w,
			double * last_spk_Arr,
			double * u,
			double * U,
			double * D,
			double * F,
			double * R,
			int n,
			double _time)
{
	addScalarToVector(n, last_spk_Arr, -_time);
	// last_spk_Arr is Negative ISI past here
	
	// Facilitation
	double * temp = malloc(n * sizeof(double));
	vdDiv(n, last_spk_Arr, F, temp);
	vdExp(n, temp, temp);
	double * temp2 = malloc(n * sizeof(double));
	dcopy(&n, U, &DEFAULT_STRIDE, temp2, &DEFAULT_STRIDE);
	dscal(n, &NEG_ONE, temp2, &DEFAULT_STRIDE);
	addScalarToVector(n, temp2, 1);
	vdMul(n, temp, temp2, temp);
	vdMul(n, temp, u, temp);
	vdAdd(n, temp, U, u);

	// Depression (re-using temp and temp2)
	vdDiv(n, last_spk_Arr, D, temp);
	vdExp(n, temp, temp);
	vdMul(n, u, R, temp2);
	vdSub(n, R, temp2, temp2);
	addScalarToVector(n, temp2, -1);
	vdMul(n, temp, temp2, R);
	addScalarToVector(n, R, 1);

	// Calculate PSRs
	vdMul(n, R, u, temp);
	vdMul(n, w, temp, temp);
	dscal(n, 10, temp, &DEFAULT_STRIDE);

	// Sum them
	double su = dasum(&n, temp, &DEFAULT_STRIDE);

	free(temp);
	free(temp2);
	temp = NULL;
	temp2 = NULL;

	return su;
}
