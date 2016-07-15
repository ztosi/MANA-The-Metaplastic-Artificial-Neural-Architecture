#ifndef NETPARAMS_H_
#define NETPARAMS_H_

//General parameters
#define NUM_NEURONS 5000
#define TIME_STEP 0.1 //ms
#define INHIB_RATIO 0.2 

// Pruner settings
#define PRUNE_CUT 0.01
#define MIN_VALUE 1E-5
#define MIN_P_PROP 0.01
#define P_INTERVAL 50000 // Iterations; 5s w/ 0.1ms TIME_STEP


/****************************************************************/
/*		SYNAPSE PARAMETERS 										*/
/****************************************************************/

// Basic Parameters
#define MAX_WT 20f
#define UDF_GAIN 10f
#define MAX_DLY 200f // In ITERATIONS!!!
#define EXC_TAU 5f //ms
#define INH_TAU 10f //ms

// STDP Parameters
#define INH_LR 2.5E-6f
#define EXC_LR 2.0E-6f

#define EE_W_PLUS 5.5f
#define EE_W_MINUS 0.8f
#define EI_W_PLUS 5.2f
#define EI_W_MINUS 0.8f
#define IE_W_PLUS 1.7f
#define IE_W_MINUS 1.65f
#define II_W_PLUS 1.8f
#define II_W_MINUS 1.2f

#define EE_TAU_PLUS 20f
#define EE_TAU_MINUS 100f
#define EI_TAU_PLUS 25f
#define EI_TAU_MINUS 100f
#define IE_TAU_PLUS 24f
#define IE_TAU_MINUS 24f
#define II_TAU_PLUS 20f
#define II_TAU_MINUS 20f



/****************************************************************/
/*		NEURON PARAMETERS 										*/
/****************************************************************/

//Base
#define E_L -70f //mV
#define V_RESET -55f //mV
#define C_M_E_MAX 30f // pF
#define C_M_E_MIN 20f // pF
#define C_M_I_MAX 25f // pF
#define C_M_I_MIN 15f // pF
#define REF_E_MAX 3.5f //ms
#define REF_E_MIN 2f //ms
#define REF_I_MAX 3f //ms
#define REF_I_MIN 1.5f //ms
#define I_BG 30f // nA
#define THETA_0 -50f // mV; BASE FIRING THRESHOLD

// FIRING RATE PLASTICITY PARAMS //
// FR Estimation
#define TAU_EPSILON 25000f
// Intrinsic Plasticity
#define NU_0 1.0f // "Low" firing rate boundary
#define ALPHA 4.0f
#define BETA 25f // Hz
#define C_PLUS 2.0f 
#define C_MINS 1.0f
#define ETA_0 0.1f
#define ETA_F 1E-7f
#define NU_0_MULTIPLIER 10f
#define SN_SOFT 15.0f
// Homeostatic plasticity
#define LAMBDA_0 1E5f
#define LAMBDA_F 1E4f
#define RAT_MUL 2.0f

// Recovery
#define REC_A 1f 
#define REC_B_E_MAX 2f //nA 
#define REC_B_E_MIN 1.3f //nA
#define REC_B_I_MAX 1.6f //nA
#define REC_B_I_MIN 1.0f //nA
#define TAU_W 250 // ms



#endif // NETPARAMS_H_