#include "mex.h"
#include <boost/math/special_functions/binomial.hpp>
#include <stdio.h>

void combF (unsigned int n, unsigned int k, double index, double* spec_perm) {
 	unsigned int rabbit = 0;
 	unsigned int j = 0;
 	while (k > 0) {
 		unsigned int i = 0;
 		double val = 0;
 		double vp = 0;
 		while (val < index) {
 			vp = val;
 			i = i + 1;
 			val +=  boost::math::binomial_coefficient<double>(n-i, k-1);
 		}
 		spec_perm[j] = i + rabbit;
 		index = index - vp;
 		rabbit += i;
 		k -= 1;
 		n -= i;
 		j++;
 	}
 }

 void mexFunction( int Nreturned, mxArray *returned[], int Noperand, const mxArray *operand[] ){

    unsigned int n = mxGetScalar(operand[0]);
    unsigned int k = mxGetScalar(operand[1]);
    unsigned long long int index = mxGetScalar(operand[2]);

    returned[0] = mxCreateDoubleMatrix(1, k, mxREAL);

    double* ret = mxGetPr(returned[0]);
    
    combF(n, k, index, ret);
}