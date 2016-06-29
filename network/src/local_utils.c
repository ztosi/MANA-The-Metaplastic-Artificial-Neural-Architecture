#include <math.h>
#include <gsl/gsl_rng.h>
#include "local_utils.h"
#include "mkl.h"
#include "gauss.h"



const double MH_FIRST_TERM = MH_A * (2.0 / (sqrt(3 * MH_SIGMA) * sqrt(sqrt(PI))));
const double MH_SIGMA_SQ = MH_SIGMA * MH_SIGMA;
gsl_rng *r;

inline void addScalarToVector(int n, double * vec, double scalar) {
	#pragma unroll
	for (int i = 0; i < n; i++) {
		vec[i] = vec[i] + scalar;
	}
}

inline double mexican_hat(double x) {
	return MH_FIRST_TERM * (1 - ((x*x)/MH_SIGMA_SQ)) * exp(-(x*x) / (2 * MH_SIGMA_SQ));
}

inline void mexican_hat_vec(int n, double * x) {
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