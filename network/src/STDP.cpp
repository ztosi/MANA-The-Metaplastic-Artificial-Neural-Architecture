#include <arrayfire.h>
#include <cstdint>
#include "SynMatrices.h"


StandardSTDP::StandardSTDP(const bool srcPol, const bool tarPol)
{
	hebb = DEF_HEBB;
	if (srcPol && tarPol) {
		w_p = DEF_EE_W_PLUS;
		w_m = DEF_EE_W_MINUS;
		tau_p = DEF_EE_TAU_P;
		tau_m = DEF_EE_TAU_M;
	} else if (srcPol && !tarPol) {
		w_p = DEF_EI_W_PLUS;
		w_m = DEF_EI_W_MINUS;
		tau_p = DEF_EI_TAU_P;
		tau_m = DEF_EI_TAU_M;
	} else if (!srcPol && tarPol) {
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

array StandardSTDP::postTrigger(const array &lastPostSpk, const array &lastArr)
{
	if (hebb) {
		return eta * af::exp((lastArr - lastPostSpk)/tau_p) * w_p;
	} else {
		return -eta * af::exp((lastArr - lastPostSpk)/tau_m) * w_m;
	}
}

array StandardSTDP::preTriggerHebb(	const uint32_t lastPostSpk,
									const array &lastArr	)
{
	return -eta * af::exp((lastPostSpk - lastArr)/tau_m) * w_m;
}

array StandardSTDP::preTriggerAntiHebb(	const uint32_t lastPostSpk,
										const array &lastArr	)
{
	return eta * af::exp((lastPostSpk - lastArr)/tau_p) * w_p;	
}