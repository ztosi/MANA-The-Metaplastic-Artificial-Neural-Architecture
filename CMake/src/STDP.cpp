#include <arrayfire.h>
#include <cstdint>
#include "../include/SynMatrices.hpp"
#include "../include/STDP.hpp"
#include "../include/Network.hpp"
#include "../include/Neuron.hpp"

using namespace af;

StandardSTDP::StandardSTDP(     const Polarity _srcPol,
                                const Polarity _tarPol,
                                const float _eta,
                                const bool _hebbian	)
	: STDP(_srcPol,_tarPol,_eta), hebbian(_hebbian)
{
	if (_srcPol && _tarPol) {
		w_p = DEF_EE_W_PLUS;
		w_m = DEF_EE_W_MINUS;
		tau_p = DEF_EE_TAU_P;
		tau_m = DEF_EE_TAU_M;
	} else if (_srcPol && !_tarPol) {
		w_p = DEF_EI_W_PLUS;
		w_m = DEF_EI_W_MINUS;
		tau_p = DEF_EI_TAU_P;
		tau_m = DEF_EI_TAU_M;
	} else if (!_srcPol && _tarPol) {
		w_p = DEF_IE_W_PLUS;
		w_m = DEF_IE_W_MINUS;
		tau_p = DEF_IE_TAU_P;
		tau_m = DEF_IE_TAU_M;
	} else {
		w_p = DEF_II_W_PLUS;
		w_m = DEF_II_W_MINUS;
		tau_p = DEF_II_TAU_P;
		tau_m = DEF_II_TAU_M;
	}
}

StandardSTDP::StandardSTDP(     const Polarity _srcPol,
                                const Polarity _tarPol,
                                const float _eta,
                                const bool _hebbian,
                                const float _w_p,
                                const float _w_m,
                                const float _tau_p,
                                const float _tau_m	)
	: STDP(_srcPol, _tarPol, _eta), hebbian(_hebbian)
{
	w_p = _w_p;
	w_m = _w_m;
	tau_p = _tau_p;
	tau_m = _tau_m;
}

void StandardSTDP::postTrigger(	array &neg_deltas	)
{
	if (hebbian) {
		neg_deltas = eta * af::exp(neg_deltas/tau_p) * w_p;
	} else {
		neg_deltas = -eta * af::exp(neg_deltas/tau_m) * w_m;
	}
}

void StandardSTDP::preTrigger(	array &neg_deltas	)
{
	if (hebbian) {
		neg_deltas = -eta * af::exp(neg_deltas/tau_m) * w_m;
	} else {
		neg_deltas = eta * af::exp(neg_deltas/tau_p) * w_p;
	}
}

MexicanHatSTDP::MexicanHatSTDP(     const Polarity _srcPol,
                                    const Polarity _tarPol,
                                    const float _eta,
                                    const float _a, 
                                    const float _sigma	)
	: STDP(_srcPol, _tarPol, _eta), a(_a)
{
	setSigma(_sigma);
}

void MexicanHatSTDP::postTrigger(	array &neg_deltas	)
{
	neg_deltas *= neg_deltas;
	neg_deltas = eta * a * nrmTerm * (1 - (neg_deltas/sigma_sq)) * exp(-neg_deltas/(2*sigma_sq)); 
}

void MexicanHatSTDP::preTrigger(	array &neg_deltas	)
{
	postTrigger(neg_deltas); // Mexican hat is symmetric...
}
