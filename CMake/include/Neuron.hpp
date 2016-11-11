#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <cstring>

#ifndef NEURON_H_
#define NEURON_H_

using namespace af;

const float  DEF_V_rest = -70.0; //mV
const float  DEF_V_reset = -55.0; //mV
const float  INIT_THRESH = -50.0; //mV
const float  DEF_E_DEC = 3.0; //ms
const float  DEF_I_DEC = 6.0; //ms
const float  E_CM_MAX = 28.0;
const float  E_CM_MIN = 24.0;
const float  I_CM_MAX = 26.0;
const float  I_CM_MIN = 18.0;
const float  DEF_E_REF = 3.0;
const float  DEF_I_REF = 2.0;
const float  DEF_E_ADPT = 15.0;
const float  DEF_I_ADPT = 10.0;
const float  DEF_TAU_W = 144.0;
const float  DEF_I_BG = 18.0;
const float  DEF_CM_EXC = 30.0;
const float  DEF_CM_INH = 20.0;
const float  DEF_NSD = 0.1;

class SynMatrices;
class Position;
class Network;

enum Polarity : uint8_t { INH=0, EXC=1 };

class GenericNeuron 
{

	public:
		const Network &host;

		const uint32_t size;
		const Polarity pol;

		array spks; 
		array spkHistory;
		array lastSpkTime;
		array lst_buff;
		array thresholds;
		array I_bg;
		array I_e;
		array I_i;
		array epsilon; // pre-estimate
		array nu_hat; // estimate
		float tauEps;

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
		virtual void runForward()=0;
		virtual void pushBuffers()=0;
		void updateEstFR(const array &targetFRs);

		Position* getPositions();
		Position getPosition(uint32_t index);

		virtual ~GenericNeuron()=0;

		void addIncExcSyn(const SynMatrices &syn);
		void addIncInhSyn(const SynMatrices &syn);

	private:
		GenericNeuron();

};

class AIFNeuron : public GenericNeuron
{

	public: 

		array V_mem;
		array V_buff;
		array w;
		array w_buff;
		array online;
		array Cm;

		const float iDecay;
		const float eDecay;
		const float V_rest;
		const float V_reset;
		const float tauW;
		const float noiseSD;
		uint32_t refP;
		float adpt;

		//TODO: give option to lay out in lattice: more efficient
		AIFNeuron(	const Network &_host,
					const uint32_t _size, 
					const Polarity _pol,
				 	const Position minPos,
				 	const Position maxPos );

		~AIFNeuron();

		void runForward();
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
						const std::string &filename );

	private:

		uint32_t** spkScript;
		uint32_t* indexPtrs;



};

#endif // NEURON_H_
