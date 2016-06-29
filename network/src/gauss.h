#include <gsl/gsl_rng.h>

#ifndef GAUSS_H_
#define GAUSS_H_

static unsigned long gsl_rng_uint32 (gsl_rng *r);

double gsl_ran_gaussian_ziggurat (gsl_rng *r, double sigma);

#endif // GAUSS_H_