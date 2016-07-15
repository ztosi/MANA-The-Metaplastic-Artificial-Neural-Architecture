#include <arrayfire.h>
#include <list>
#include <math.h>

#include "netparams.h"

#ifndef SYNAPSE_H_
#define SYNAPSE_H_

using namespace af;
using namespace std;

class Synapse 
{
	public:

		static const float ednom = (float) 1-exp(-1);
		static const float mx_wt_sq = MAX_WT * MAX_WT;

		// All 2D arrays out of both
		// laziness and because on GPUs
		// more efficient, and in the
		// beginning they're full 
		// anyway.
		Neuron *src;
		Neuron *tar;
		list<uint32_t> delay;

		// Dynamic, but actual values stored
		// elsewhere
		list<*float> *I_dest;
		list<*float> *w;
		list<*float> *dwdt;

		// Static
		vector<float> *U;
		vector<float> *D;
		vector<float> *F;

		// Dynamic, but stored here
		vector<float> *lu; // last update
		vector<float> *R;
		vector<float> *u;

		static void weightChange(array *w, array *dw)
		{
			dw *= (1-af::exp((w*w/mx_wt_sq)-1))/ednom;
			af::replace(dw, af::abs(dw) <= 1.0f, 1);
			w += dw;
		}




}




#endif // SYNAPSE_H_