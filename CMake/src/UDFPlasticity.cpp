#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include <cmath>
#include <random>
#include "../include/UDFPlasticity.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/Neuron.hpp"

UDFPlasticity::UDFPlasticity(const SynMatrices &_synHost)
	: synHost(_synHost), scaleFactor(DEF_SCALE_FAC),
	FUuRD(constant(0, _synHost.getSize(), 5, f32))
{


	if (_synHost.srcHost.pol) {
		if (_synHost.tarHost.pol) { 	// EE
			U_mean = EE_U_MN;
			U_std = EE_U_SD;
			D_mean = EE_D_MN;
			D_std = EE_D_SD;
			F_mean = EE_F_MN;
			F_std = EE_F_SD;
		} else { 						// EI
			U_mean = EI_U_MN;
			U_std = EI_U_SD;
			D_mean = EI_D_MN;
			D_std = EI_D_SD;
			F_mean = EI_F_MN;
			F_std = EI_F_SD;
		}
	} else {
		if (_synHost.tarHost.pol) { 	// IE
			U_mean = IE_U_MN;
			U_std = IE_U_SD;
			D_mean = IE_D_MN;
			D_std = IE_D_SD;
			F_mean = IE_F_MN;
			F_std = IE_F_SD;
		} else { 						// II
			U_mean = II_U_MN;
			U_std = II_U_SD;
			D_mean = II_D_MN;
			D_std = II_D_SD;
			F_mean = II_F_MN;
			F_std = II_F_SD;
		}
	}
}

UDFPlasticity* UDFPlasticity::instantiateUDF(const SynMatrices &_synHost)
{
	UDFPlasticity* udf = new UDFPlasticity(_synHost);
	uint32_t sz = _synHost.getSize();
	udf->FUuRD(span, 0) =  abs((udf->F_std*randn(sz, f32))+udf->F_mean);
	udf->FUuRD(span, 1) =  abs((udf->U_std*randn(sz, f32))+udf->U_mean);
	udf->FUuRD(span, 4) =  abs((udf->D_std*randn(sz, f32))+udf->D_mean);
	udf->FUuRD(span, 3) = 1; // Initial value
	return udf;
}


array UDFPlasticity::perform(	const array &ISIs,
								const array &wts	)
{
	FUuRD.col(2) = FUuRD.col(1) + FUuRD.col(2)*(1-FUuRD.col(1)) * exp(ISIs/FUuRD.col(0));
	FUuRD.col(3) = 1 + (FUuRD.col(3) - (FUuRD.col(2)*FUuRD.col(3)) - 1) * exp(ISIs/FUuRD.col(4));
	return FUuRD.col(2) * FUuRD.col(3) * wts * scaleFactor;
}

float* UDFPlasticity::generateUDFVals(	const Polarity srcPol,
										const Polarity tarPol	)
{
	float* udfs = new float[5];
	float U_mean;
	float U_std;
	float D_mean;
	float D_std;
	float F_mean;
	float F_std;
	if (srcPol == 1) {
		if (tarPol == 1) { 	// EE
			U_mean = EE_U_MN;
			U_std = EE_U_SD;
			D_mean = EE_D_MN;
			D_std = EE_D_SD;
			F_mean = EE_F_MN;
			F_std = EE_F_SD;
		} else { 						// EI
			U_mean = EI_U_MN;
			U_std = EI_U_SD;
			D_mean = EI_D_MN;
			D_std = EI_D_SD;
			F_mean = EI_F_MN;
			F_std = EI_F_SD;
		}
	} else {
		if (tarPol == 1) { 	// IE
			U_mean = IE_U_MN;
			U_std = IE_U_SD;
			D_mean = IE_D_MN;
			D_std = IE_D_SD;
			F_mean = IE_F_MN;
			F_std = IE_F_SD;
		} else { 						// II
			U_mean = II_U_MN;
			U_std = II_U_SD;
			D_mean = II_D_MN;
			D_std = II_D_SD;
			F_mean = II_F_MN;
			F_std = II_F_SD;
		}
	}
	udfs[0] = std::abs(U_mean + (normDist(gen)*U_std));
	udfs[1] = std::abs(D_mean + (normDist(gen)*D_std));
	udfs[2] = std::abs(F_mean + (normDist(gen)*F_std));
	udfs[3] = 0;
	udfs[0] = 1;

	return udfs;

}
