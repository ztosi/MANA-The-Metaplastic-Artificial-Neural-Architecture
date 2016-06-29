#ifndef NETPARAMS_H_
#define NETPARAMS_H_

//General parameters
#define NUM_NEURONS 5000
#define NUM_INPUTS 50
#define TIME_STEP 0.5 
#define INHIB_RATIO 0.2
#define INPUT_RATE 20 
#define MAX_DELAY 16
#define MAX_DELAY_ITERS ((int) (MAX_DELAY / TIME_STEP)) 

// Pruner settings
#define PRUNE_PROB 0.05
#define MIN_VALUE 1E-5

#endif // NETPARAMS_H_