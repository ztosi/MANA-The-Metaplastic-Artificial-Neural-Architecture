#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "SynMatrices.h"

#ifndef STDP_H_
#define STDP_H_


#define DEF_HEBB 1
#define DEF_EE_W_PLUS 5.6
#define DEF_EE_W_MINUS 0.9
#define DEF_EI_W_PLUS 5.3
#define DEF_EI_W_MINUS 1
#define DEF_IE_W_PLUS 1.6
#define DEF_IE_W_MINUS 1.65
#define DEF_II_W_PLUS 1.2
#define DEF_II_W_MINUS 1.4

#define DEF_EE_TAU_P 25
#define DEF_EE_TAU_M 100
#define DEF_EI_TAU_P 20
#define DEF_EI_TAU_M 100

#define DEF_IE_SIG 24
#define DEF_II_SIG 12 

#define DEF_A 30


class STDP 
{

	public:
		SynMatrices &host;
		float eta;
		STDP(const SynMatrices &host);
		~STDP();
		virtual void postTrigger(const uint32_t simTime)=0;
		virtual void preTrigger(const uint32_t simTime)=0;
};

class standardSTDP : public STDP 
{

	public:

		float w_p;
		float w_m;
		float tau_p;
		float tau_m;
		bool hebbian;

		standardSTDP(const SynMatrices &host);
		~standardSTDP();
		void postTrigger(const uint32_t simTime);
		void preTrigger(const uint32_t simTime);
};

class mexicanHatSTDP : public STDP
{

	public:

		float a;
		float sigma;
		mexicanHatSTDP(const SynMatrices &host);
		~mexicanHatSTDP();
		void postTrigger(const uint32_t simTime, af::spks);
		void preTrigger(const uint32_t simTime, af::array &spksAtDly);
};

#endif // SOMODULE_H_