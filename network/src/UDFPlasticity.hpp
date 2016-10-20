#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include <random>
#include "Spk_Delay_MNGR.h"
#include "SynMatrices.h"

#ifndef UDFPLASTICITY_H_
#define UDFPLASTICITY_H_

#define DEF_SCALE_FAC 10f
#define EE_U_MN 0.5f
#define EE_U_SD 0.25f
#define EE_D_MN 1100f //ms
#define EE_D_SD 550f //ms
#define EE_F_MN 50f // ms
#define EE_F_SD 25f //ms
#define EI_U_MN 0.05f
#define EI_U_SD 0.025f
#define EI_D_MN 125f //ms
#define EI_D_SD 62.5f //ms
#define EI_F_MN 1200f // ms
#define EI_F_SD 600f //ms
#define IE_U_MN 0.25f
#define IE_U_SD 0.125f
#define IE_D_MN 700f //ms
#define IE_D_SD 350f //ms
#define IE_F_MN 20f // ms
#define IE_F_SD 10f //ms
#define II_U_MN 0.32f
#define II_U_SD 0.16f
#define II_D_MN 144f //ms
#define II_D_SD 72f //ms
#define II_F_MN 60f // ms
#define II_F_SD 30f //ms

using namespace af;

class UDFPlasticity {

	public:

		SynMatrices &host;

		//std::vector<array> U;
		//std::vector<array> D;
		//std::vector<array> F;
		//std::vector<array> u;
		//std::vector<array> R;

		std::vector<array> UDFuR; // row major

		float scaleFactor;

		UDFPlasticity(SynMatrices &hostSyns);
		array perform(	const array udfur,
						const array tdiff,
						const array wts	);

		static float* generateUDFVals(	const GenericNeuron::Polarity srcPol,
										const GenericNeuron::Polarity tarPol	);

	private:
		static std::default_random_engine gen;
		static std::normal_distribution<float> normDist(0, 1);

};

#endif // UDFPLASTICITY_H_