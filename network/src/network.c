#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include <gsl/gsl_rng.h>
#include <omp.h>
#include "positions.h"
#include "gauss.h"
#include "mkl.h"

//General parameters
#define NUM_NEURONS 5000
#define NUM_INPUTS 50
#define TIME_STEP 0.1 
#define INHIB_RATIO 0.2
#define INPUT_RATE 20 
#define MAX_DELAY 16
#define MAX_DELAY_ITERS ((int) (MAX_DELAY / TIME_STEP)) 

// Pruner settings
#define PRUNE_PROB 0.05
#define MIN_VALUE 1E-5


 