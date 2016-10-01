#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "SynMatrices.h"

#ifndef STDP_H_
#define STDP_H_


#define DEF_HEBB 1f
#define DEF_EE_W_PLUS 5.6f
#define DEF_EE_W_MINUS 0.9f
#define DEF_EI_W_PLUS 5.3f
#define DEF_EI_W_MINUS 1f
#define DEF_IE_W_PLUS 1.6f
#define DEF_IE_W_MINUS 1.65f
#define DEF_II_W_PLUS 1.2f
#define DEF_II_W_MINUS 1.4f

#define DEF_EE_TAU_P 25f
#define DEF_EE_TAU_M 100f
#define DEF_EI_TAU_P 20f
#define DEF_EI_TAU_M 100f

#define DEF_IE_SIG 24f
#define DEF_II_SIG 12f 

#define DEF_A 30f
#define DEF_INIT_ETA 2.5E-7f


class STDP 
{

	public:
		//SynMatrices &host;
		float eta;
		STDP(const bool srcPol, const bool tarPol);
		~STDP();
		virtual void postTrigger(const uint32_t simTime)=0;
		virtual array preTrigger(	const uint32_t simTime,
									const uint32_t lastPostSpk,
									const array lastArr	)=0;
};

class StandardSTDP : public STDP 
{

	public:

		float w_p;
		float w_m;
		float tau_p;
		float tau_m;
		bool hebbian;

		StandardSTDP(const bool srcPol, const bool tarPol);
		~StandardSTDP();
		void postTrigger(const uint32_t simTime);
		array preTrigger(	const uint32_t simTime,
							const uint32_t lastPostSpk,
							const array lastArr	);
};

class mexicanHatSTDP : public STDP
{

	public:

		float a;
		float sigma;
		mexicanHatSTDP(const bool srcPol, const bool tarPol);
		~mexicanHatSTDP();
		void postTrigger(const uint32_t simTime, af::spks);
		array preTrigger(const uint32_t simTime, af::array &spksAtDly);
};

#endif // SOMODULE_H_