#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include <time.h>
#include <gsl/gsl_rng.h>
#include <omp.h>
#include "fastonebigheader.h"
#include "netparams.h"
#include "netcomponents.h"
#include "positions.h"
#include "gauss.h"
#include "mkl.h"

static const unsigned int SPK_MASK = 1;
static const unsigned int CLEAR_MASK = 0x80000000;
static const double tau[2] = {30, 20};
static const double ref[2] = {3, 2};
static const double STDP_W_PLUS[2][2] = {{5, 5}, {6, 5}} ;
static const double STDP_W_MINUS[2][2] = {{-1, -1}, {-1.5, -1}};
static const double STDP_TAU_PLUS[2][2] = {{20, 20}, {20, 20}} ;
static const double STDP_TAU_MINUS[2][2] = {{100, 100}, {40, 100}};
static const double STDP_SYM_TAU[2][2] = {{10, 10}, {5, 10}};
static const double MH_FIRST_TERM = MH_A * (2.0 / (sqrt(3 * MH_SIGMA) * sqrt(sqrt(PI))));
static const double MH_SIGMA_SQ = MH_SIGMA * MH_SIGMA;

static double time;

static double ** v_mem_Noise;
static double ** IP_Noise;
static short unsigned int noise_index = 0; 

static double HP_CONST = 1E5;
static double IP_CONST = .01;
static double _time;
static gsl_rng *r;

static unsigned int ID_VAL = 0;


static Neuron * constructNeuron(int excite, int initialIncoming) 
{
	Neuron *n;
	//printf("Started Neuron: %i\n", id);
	if ((n = malloc(sizeof *n)) != NULL) {
		n->id = assignID();
		n->excite = excite;
		//Default values below
		n->spk = 0;
		n->lastSpkTime = 0;
		n->spkCnt = 0;
		n->spkTrain = malloc(8*sizeof(int));
		for (int i = 0; i < 8; i++) {
			n->spkTrain = 0;
		}
		n->V_mem = V_RESET;
		n->threshold = INIT_THRESH;
		n->adapt = 0;
		n->prefFR = PFR_INIT;
		n->estFR_unScale = n->prefFR / 1000;
		n->estFR_scale = n->prefFR;
		n->ei = malloc(2*sizeof(double));
		n->ei[0] = 0;
		n->ei[1] = 0;
		// Will become NUM_NEURONS after initialization of synapses
		n->inDeg = 0; // ... then however many are left after pruning.
		//static Synapse* syns[NUM_NEURONS];
		Synapse **incoming = malloc((initialIncoming) * sizeof(Synapse*));
		n->incoming = incoming;
		n->input = 0;
	}
	//printf("Generated Neuron: %i\n", id);
	return n;
}

static Synapse * constructSynapse(Neuron *src, Neuron *tar) 
{
	Synapse *s;
	if ((s = malloc(sizeof *s)) != NULL) {
		s->src = src;
		s->tar = tar;
		s->dly = 0;
		s->remain = 0;
		s->wt = INIT_WT;
		s->tar->incoming[s->tar->inDeg++] = s;
	}
	return s;
	//free(s);
	//s = NULL;
}

inline void annealIPHP() 
{
	HP_CONST += TIME_STEP * HP_COOL_RATE * (HP_FINAL - HP_CONST);
	IP_CONST += TIME_STEP * IP_COOL_RATE * (IP_FINAL - IP_CONST);
}

static inline void calcEstFR(Neuron *n) 
{
	double tauA = TAUA_NUMER / ((n->prefFR) + 1);
	double adapt_ = n->adapt - (TIME_STEP * n->adapt / tauA);
	n->estFR_unScale += TIME_STEP * ((n->adapt/tauA) - n->estFR_unScale);
	n->estFR_scale = n->estFR_unScale * 1000;
	n->adapt = adapt_;
}

static inline void updateThresh(Neuron *n)
 {


	double fac = n->estFR_scale > n->prefFR ? n->estFR_scale + 1E-6
		: n->prefFR + 1E-6;
		//if (n->id == 1) printf("%f\n", exp(  (n->estFR_scale - n->prefFR)/ (fac * HP_CONST)    )  );
	n->threshold += TIME_STEP * n->threshold * (fastexp((n->estFR_scale
		- n->prefFR) / (fac * HP_CONST)) - 1);
}

static inline void updatePFR(Neuron *n) 
{
	if (n->estFR_scale > n->prefFR) {
		n->prefFR += TIME_STEP * C_FP * IP_CONST * fastexp(-n->prefFR / (BETA * LOW_FR)) * (1 + gsl_ran_gaussian_ziggurat(r, 0.05));
	} else {
		if (n->prefFR <= LOW_FR) {
			n->prefFR -= TIME_STEP * C_FD * IP_CONST * (n->prefFR / LOW_FR) * (1 + gsl_ran_gaussian_ziggurat(r, 0.05));
		} else {
			n->prefFR -= TIME_STEP * C_FD * IP_CONST * (1 + (fastlog(1 + (ALPHA
				* ((n->prefFR / LOW_FR) - 1))) / ALPHA)) * (1 + gsl_ran_gaussian_ziggurat(r, 0.05));
		}

		n->prefFR *= n->prefFR > 0;
	}
}

static inline void updateTHandPFR(Neuron *n) {
	if ((n->estFR_scale < (n->prefFR - (n->prefFR * HP_TOLERANCE)))
		|| (n->estFR_scale > (n->prefFR + (n->prefFR * HP_TOLERANCE)) ) ) {
		updateThresh(n);
		updatePFR(n);
	}
}



///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
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
								int n)
{
	// Increment w, based on time since last dw change and dw
	updateWt(w, dw, lastEvent, n);

	// Find new dw (*everything* in function beyond here)
	double delta_t = -(time - tar_last_spkT);

	// Change WM and tauM based on symmetric firing 
	estFR_tar = ((estFR_tar > 50) ? 50 : estFR_tar) - 100;
	#pragma unroll
	for(int i = 0; i < n; i++) {
		estFR_src[i] = ((estFR_src[i] > 50) ? 50 : estFR_src[i]) + estFR_tar;
	}
	dscal(n, 1/SYM_TAU, estFR_src, 1);
	vdExp(n, estFR_src, estFR_src);
	double w_delta = -STDP_W_PLUS[0][0] - STDP_W_MINUS[0][0];
	double * wm = malloc(n * sizeof(double));
	memcpy(wm, estFR_src, n);
	dscal(n, w_delta, wm, 1);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		wm[i] = -1*(wm[i] + STDP_W_MINUS[0][0]); // makes dw negative
	}
	double * tm = malloc(n * sizeof(double));
	memcpy(tm, estFR_src, n);
	double tau_delta = STDP_TAU_PLUS[0][0] - STDP_TAU_MINUS[0][0];
	dscal(n, tau_delta, tm, 1);
	#pragma unroll
	for (int i = 0; i < n; i++) {
		tm[i] = tm[i] + STDP_TAU_MINUS[0][0];
	}
	vdInv(n, tm, tm);
	dscal(n, delta_t, tm, 1);
	vdExp(n, tm, tm);
	vdSub(n, tm, estFR_src, tm);
	vdMul(n, tm, wm, dw);
	free(wm);
	free(tm);
	wm = NULL;
	tm = NULL;
}

void stdp_tarSpk_ExSrc_ExTar(	double * w,
								double * dw,
								double * lastEvent,
								double * estFR_src,
								double estFR_tar,
								double * last_spk_Arr,
								int n)
{
	// Increment w, based on time since last dw change and dw
	updateWt(w, dw, lastEvent, n);

	// Find new dw (*everything* in function beyond here)
	double t_post = time; // Triggered by target Spike
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
	dscal(n, 1.0/SYM_TAU, estFR_src, 1);
	vdExp(n, estFR_src, estFR_src);
	
	// Perform the main exponential & multiply and subtract value from above
	dscal(n, 1.0/STDP_TAU_PLUS[0][0], delta, 1);
	vdExp(n, delta, delta);
	dscal(n, STDP_W_PLUS[0][0], delta, 1);
	vdSub(n, delta, estFR_src, delta);

	// Weight softening
	memcpy(dw, w, n);
	dscal(n, -1/WT_SOFTENING, dw, 1);
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
								int n)
{
	updateWt(w, dw, lastEvent, n);

	// Find new dw
	double delta_t = tar_last_spkT - time;
	double delta_w = STDP_W_PLUS[0][1] * exp(delta_t / STDP_TAU_PLUS[0][1]);
	memcpy(dw, w, n);
	dscal(n, -1/WT_SOFTENING, dw, 1);
	vdExp(n, dw, dw);
	dscal(n, delta_w, dw, 1);
}

// 
 void stdp_tarSpk_ExSrc_InTar(	double * w,
								double * dw,
								double * lastEvent,
								double * last_spk_Arr,
								int n)
{
	updateWt(w, dw, lastEvent, n);
	
	// Find new dw
	memcpy(dw, last_spk_Arr, n);
	addScalarToVector(n, dw, -time);
	dscal(n, 1/STDP_TAU_MINUS[0][1], dw, 1);
	vdExp(n, dw, dw);
	dscal(n, -STDP_W_MINUS[0][1], dw, 1);
	addScalarToVector(n, dw, .25);
}


void stdp_ArriveT_InSrc(	double * w,
							   	double * dw,
								double * lastEvent,
								double tar_last_spkT,
								int n)
{
	updateWt(w, dw, lastEvent, n);

	// Find new dw
	double delta_t = tar_last_spkT - time;
	double delta_w = mexican_hat(delta_t);
	if (delta_w > 0) {
		memcpy(dw, w, n);
		dscal(n, -1/WT_SOFTENING, dw, 1);
		vdExp(n, dw, dw);
		dscal(n, delta_w, dw, 1);
	} else {
		memcpy(dw, delta, n);
	}
}

static void stdp_tarSpk_InSrc(	double * w,
							   	double * dw,
								double * lastEvent,
								double * last_spk_Arr,
								int n)
{
	updateWt(w, dw, lastEvent, n);

	// Find new dw
	memcpy(dw, last_spk_Arr, n);
	addScalarToVector(n, dw, -time);
	mexican_hat_vec(n, dw);
	for (int i = 0; i < n; i++) {
		if (dw[i] > 0) {
			dw[i] = dw[i] * exp(-w[i]/WT_SOFTENING);
		}
	}	
}

// Updates w based on dw (* L_RATE) and the time since the last event, then
// writes back to lastEvent the current time.
inline void updateWt(double * w,
							double * dw,
							double * lastEvent,
							int n) 
{
	// Increment w, based on time since last dw change and dw
	#pragma unroll
	for (int i = 0; i < n; i++) {
		lastEvent[i] = time - lastEvent[i];
	}
	vdMul(n, lastEvent, dw, dw);
	daxpy(n, L_RATE, dw, 1, w, 1);
	// Now performed in "copyBackValues" which is called whenever this is called
	//#pragma unroll 
	//for (int i = 0; i < n; i++) {
	//	lastEvent[i] = time;
	//}
}

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
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
			int n)
{
	addScalarToVector(n, last_spk_Arr, -time); // Negative ISI
	
	// Facilitation
	double * temp = malloc(n * sizeof(double));
	vdDiv(n, last_spk_Arr, F, temp);
	vdExp(n, temp, temp);
	double * temp2 = malloc(n * sizeof(double));
	memcpy(temp2, U, n);
	dscal(n, -1, temp2, 1);
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
	dscal(n, 10, temp, 1);

	// Sum them
	double su = dasum(n, temp, 1);

	free(temp);
	free(temp2);
	temp = NULL;
	temp2 = NULL;

	return su;
}


static inline void addScalarToVector(int n, double * vec, double scalar) {
	#pragma unroll
	for (int i = 0; i < n; i++) {
		vec[i] = vec[i] + scalar;
	}
}

static inline double mexican_hat(double x) {
	return MH_FIRST_TERM * (1 - ((x*x)/MH_SIGMA_SQ)) * exp(-(x*x) / (2 * MH_SIGMA_SQ));
}

static inline void mexican_hat_vec(int n, double * x) {
	double * x2 = malloc(n * sizeof(double));
	vdSqr(n, x, x2);
	dscal(n, -1/MH_SIGMA, x2, 1);
	memcpy(x, x2, n);
	dscal(n, 0.5, x, 1);
	vdExp(n, x, x);
	addScalarToVector(n, x2, 1);
	vdMul(n, x, x2, x);
	dscal(n, MH_FIRST_TERM, x, 1);
	free(x2);
	x2 = NULL;
}








void setUpRNG_ENV(void) 
{
	const gsl_rng_type * T;
	gsl_rng_env_setup();
	T = gsl_rng_taus;
	r = gsl_rng_alloc(T);
}

int assignID() {
	return ID_VAL++;
}

int main() 
{
	printf("%s\n", "Begin");

	// Start the random number generator.
	setUpRNG_ENV();

	
}




