#include <arrayfire.h>
#include <cstdint>

#ifndef UDFPLASTICITY_H_
#define UDFPLASTICITY_H_

#define DEF_SCALE_FAC 10
#define EE_RU_MN 0.5
#define EE_RU_SD 0.25
#define EE_RD_MN 1100 //ms
#define EE_RD_SD 550 //ms
#define EE_RF_MN 50 // ms
#define EE_RF_SD 25 //ms
#define EI_RU_MN 0.05
#define EI_RU_SD 0.025
#define EI_RD_MN 125 //ms
#define EI_RD_SD 62.5 //ms
#define EI_RF_MN 1200 // ms
#define EI_RF_SD 600 //ms
#define IE_U_MN 0.25
#define IE_RU_SD 0.125
#define IE_RD_MN 700 //ms
#define IE_RD_SD 350 //ms
#define IE_RF_MN 20 // ms
#define IE_RF_SD 10 //ms
#define II_RU_MN 0.32
#define II_RU_SD 0.16
#define II_RD_MN 144 //ms
#define II_RD_SD 72 //ms
#define II_RF_MN 60 // ms
#define II_RF_SD 30 //ms


class UDFPlasticity {

	public:

		SynMatrices &host;

		af::array &U;
		af::array &D;
		af::array &F;
		af::array &u;
		af::array &R;

		float scaleFactor;

		UDFPlasticity(SynMatrices &hostSyns);
		void perform(const uint32_t time, const float timeStep);

};

#endif // UDFPLASTICITY_H_