#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <cstring>
#include "Utils.h"


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

using namespace af;

class GenericNeuron 
{

	enum Polarity : uint8_t { INH=0, EXC=1 };

	public:
		const Network &host;

		const uint32_t size;
		const Polarity pol;

		array spks; 
		array spkHistory;
		array I_bg;

		std::vector<SynMatrices> incoExcSyns;
		std::vector<SynMatrices> incoInhSyns;

		uint32_t* exInDegs; //useful to keep these for pruner
		uint32_t* inInDegs;
		uint32_t* outDegs;

		float* x;
		float* y;
		float* z;

		GenericNeuron(	const Network &_host,
								const uint32_t _size, 
								const Polarity _pol,
				 				const Position minPos,
				 				const Position maxPos );
		virtual void runForward(const uint32_t t, const float dt)=0;
		virtual void pushBuffers()=0;
		Position* getPositions();
		Position getPosition(uint32_t index);

};

class AIFNeuron : public GenericNeuron
{

	public: 

		array V_mem;
		array V_buff;
		array w;
		array w_buff;
		
		array thresholds;
		array lastSpkTime;
		array lst_buff;
		array adpt;
		array online;

		array I_e;
		array I_i;
		array Cm;

		const float iDecay;
		const float eDecay;
		const float V_rest;
		const float V_reset;
		const float tauW;
		const float noiseSD;
		//uint32_t network_id;
		uint32_t refP;

		//TODO: give option to lay out in lattice: more efficient
		AIFNeuron(	const Network &_host,
					const uint32_t _size, 
					const Polarity _pol,
				 	const Position minPos,
				 	const Position maxPos );
		~AIFNeuron();

		void runForward(const uint32_t t, const float dt);
		void pushBuffers();

		friend class DataRecorder;

};

class InputNeuron : public GenericNeuron
{

	public:

		InputNeuron( 	const Network &_host,
						const uint32_t _size,
						const Polarity _pol,
						const Position minPos,
						const Position maxPos,
						const uint32_t** _spkScript);
		InputNeuron( 	const Network &_host,
						const uint32_t _size,
						const Polarity _pol,
						const Position minPos,
						const Position maxPos,
						const String &filename );

	private:

		uint32_t** spkScript;
		uint32_t* indexPtrs;



};

#endif // AIFNEURON_H_