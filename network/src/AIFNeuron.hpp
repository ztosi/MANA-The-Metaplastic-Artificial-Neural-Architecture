#include <arrayfire.h>
#include <cstdint>
#include <vector>

#ifndef AIFNEURON_H_
#define AIFNEURON_H_

#define DEF_V_rest -70f //mV
#define DEF_V_reset -55f //mV
#define INIT_THRESH -50f //mV
#define DEF_E_DEC 3f //ms
#define DEF_I_DEC 6f //ms
#define E_CM_MAX 28f
#define E_CM_MIN 24f
#define I_CM_MAX 26f
#define I_CM_MIN 18f
#define DEF_E_REF 3f
#define DEF_I_REF 6f
#define DEF_TAU_W 144f
#define DEF_I_BG 18f
#define DEF_CM_EXC 30f
#define DEF_CM_INH 20f
#define DEF_NSD 0.1f


class AIFNeuron {

	public:

		af::array* V_mem;
		af::array* V_buff;
		af::array* w;
		af::array* w_buff;
		af::array* thresholds;

		af::array* lastSpkTime;
		af::array* lst_buff;
		af::array* adpt;

		af::array online;
		af::array spks; // No need to buffer; copied by SynMat
		af::array I_e;
		af::array I_i;
		af::array I_bg;
		af::array S_e;
		af::array S_i;
		af::array Cm;

		std::vector<SynMatrices> incoSyns;

		uint32_t* exInDegs; //useful to keep these for pruner
		uint32_t* inInDegs;
		uint32_t* outDegs;

		float iDecay;
		float eDecay;
		float V_rest;
		float V_reset;
		float tauW;
		float noiseSD;
		//uint32_t network_id;
		uint32_t refP;

		float* x;
		float* y;
		float* z;

		uint32_t size;
		int excite : 1;

		//AIFNeuron(const Network net, const uint32_t size, const int polarity);
		AIFNeuron(	const Network net,
					const uint32_t size, 
					const int polarity,
				 	const float xmin,
				 	const float xmax,
				 	const float ymin,
				 	const float ymax,
				 	const float zmin,
				 	const float zmax);
		~AIFNeuron();

		void runForward(const uint32_t t, const float dt);
		void pushBuffers();

};

#endif // AIFNEURON_H_