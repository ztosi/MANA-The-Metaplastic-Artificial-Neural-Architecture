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

		const bool srcPol;
		const bool tarPol;

		//SynMatrices &host;
		STDP(const bool srcPol, const bool tarPol);
		virtual void postTrigger(const uint32_t simTime)=0;
		virtual array preTrigger(	const uint32_t simTime,
									const uint32_t lastPostSpk,
									const array lastArr	)=0;
	private:

		float eta;

	friend class DataRecorder;
};

class StandardSTDP : public STDP 
{

	public:

		StandardSTDP(	const bool _srcPol,
						const bool _tarPol,
						const bool _hebbian);

		StandardSTDP(	const bool _srcPol,
						const bool _tarPol,
						const bool _hebbian,
						const float _w_p,
						const float _w_m,
						const float _tau_p
						const float _tau_m	);

		virtual void postTrigger(const uint32_t simTime);
		virtual array preTrigger(	const uint32_t simTime,
							const uint32_t lastPostSpk,
							const array lastArr	);

	private:

		bool hebbian;
		float w_p;
		float w_m;
		float tau_p;
		float tau_m;


	friend class DataRecorder;
};

class SymStdSTDP : public StandardSTDP
{



};

class MexicanHatSTDP : public STDP
{

	public:

		float a;
		float sigma;
		MexicanHatSTDP(	const bool _srcPol,
		 				const bool _tarPol 	);

		MexicanHatSTDP(	const bool _srcPol,
		 				const bool _tarPol,
		 				const float _a,
		 				const float _sigma 	);


		void postTrigger(const uint32_t simTime, af::spks);
		array preTrigger(const uint32_t simTime, af::array &spksAtDly);

	friend class DataRecorder;
};

#endif // SOMODULE_H_