#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include <random>

#ifndef UDFPLASTICITY_H_
#define UDFPLASTICITY_H_

using namespace af;

const float DEF_SCALE_FAC = 10.0;
const float EE_U_MN = 0.5;
const float EE_U_SD = 0.25;
const float EE_D_MN = 1100.0; //ms
const float EE_D_SD = 550.0; //ms
const float EE_F_MN = 50.0; // ms
const float EE_F_SD = 25.0; //ms
const float EI_U_MN = 0.05;
const float EI_U_SD = 0.025;
const float EI_D_MN = 125.0; //ms
const float EI_D_SD = 62.5; //ms
const float EI_F_MN = 1200.0; // ms
const float EI_F_SD = 600.0; //ms
const float IE_U_MN = 0.25;
const float IE_U_SD = 0.125;
const float IE_D_MN = 700.0; //ms
const float IE_D_SD = 350.0; //ms
const float IE_F_MN = 20.0; // ms
const float IE_F_SD = 10.0; //ms
const float II_U_MN = 0.32;
const float II_U_SD = 0.16;
const float II_D_MN = 144.0; //ms
const float II_D_SD = 72.0; //ms
const float II_F_MN = 60.0; // ms
const float II_F_SD = 30.0; //ms

class GenericNeuron;
class SynMatrices;

#include "Neuron.hpp"

class UDFPlasticity {

	public:

		SynMatrices &synHost;

		array FUuRD; // column major

		float scaleFactor;

		static UDFPlasticity* instantiateUDF(const SynMatrices &_synHost);

		array perform(	const array &ISIs,
						const array &wts	);

		static float* generateUDFVals(	const Polarity srcPol,
										const Polarity tarPol	);

	private:
		float U_mean;
		float U_std;
		float D_mean;
		float D_std;
		float F_mean;
		float F_std;
		static std::default_random_engine gen;
		static std::normal_distribution<float> normDist;

		UDFPlasticity(const SynMatrices &_synHost);

};

#endif // UDFPLASTICITY_H_
