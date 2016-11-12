#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <cstring>
#include "Network.hpp"
#include "Utils.hpp"

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
const float  DEF_TAU_EPS = 10000;

class SynMatrices;
class SynGrowthManager;

enum Polarity : uint8_t { INH=0, EXC=1, NONE=2 };

class GenericNeuron 
{

	public:
		const Network &netHost;

		const uint32_t size;
		const Polarity pol;

		array spks; 
		array spkHistory;
		array lastSpkTime;
		array lst_buff;
        array I_e;
        array I_i;

		array epsilon; // pre-estimate
		array nu_hat; // estimate
		float tauEps;
                        
        std::vector<SynMatrices*> incoExcSyns;
		std::vector<SynMatrices*> incoInhSyns;
                        
		virtual void runForward()=0;
		virtual void pushBuffers()=0;
		void updateEstFR(const array &targetFRs);

		Position* getPositions();
		Position getPosition(uint32_t index);

        uint32_t* getExcInDegs();
        uint32_t* getInhInDegs();
        uint32_t* getOutDegs();

		virtual ~GenericNeuron()
        {
            delete[] x;
            delete[] y;
            delete[] z;
            delete[] exInDegs;
            delete[] inInDegs;
            delete[] outDegs;
        }

		void addIncExcSyn(const SynMatrices &syn);
		void addIncInhSyn(const SynMatrices &syn);
        
    protected:
        
        GenericNeuron(	const Network &_netHost,
                        const uint32_t _size, 
                        const Polarity _pol )
        :   netHost(_netHost),
            spks(constant(0, dim4(_size, 1), b8)),
            lastSpkTime(constant(0, dim4(_size, 1), u32)),
            lst_buff(constant(0, dim4(_size, 1), u32)),
            spkHistory(constant(0,
                dim4(_size*((uint32_t)(_netHost.maxDelay/_netHost.dt)), 1), b8)), 
            epsilon(constant(0, dim4(_size, 1), f32)),
            nu_hat(constant(0, dim4(_size, 1), f32)),
            I_e(constant(0, dim4(_size, 1), f32)),
            I_i(constant(0, dim4(_size, 1), f32)),
            tauEps(DEF_TAU_EPS), size(_size), pol(_pol) {}
                    
        GenericNeuron() : GenericNeuron(Network(), 1, Polarity::NONE) {}
        
        uint32_t* exInDegs; //useful to keep these for pruner
		uint32_t* inInDegs;
		uint32_t* outDegs;
        
        float* x;
		float* y;
		float* z;
        
    friend class DataRecorder;
    friend class SynMatrices;
    friend class SynGrowthManager;

};

class ThresholdedNeuron : public GenericNeuron
{
    public:
        array thresholds;
        ThresholdedNeuron(  const Network &_netHost,
                            const uint32_t _size, 
                            const Polarity _pol,
                            const float _initThresh)
        : GenericNeuron(_netHost, _size, _pol),
        thresholds(constant(_initThresh, dim4(_size, 1), f32)) {}
    
    private:
        ThresholdedNeuron()
        : GenericNeuron(), thresholds(constant(0, dim4(1,1,1,1), f32)) {}
        
    friend class DataRecorder;
    friend class SynMatrices;
    friend class SynGrowthManager;
};

class AIFNeuron : public ThresholdedNeuron
{

	public: 

		array V_mem;
		array V_buff;
		array w;
		array w_buff;
		array online;
		array Cm;
		array I_bg;

		const float iDecay;
		const float eDecay;
		const float V_rest;
		const float V_reset;
		const float tauW;
		const float noiseSD;
        
		uint32_t refP;
		float adpt;

		//TODO: give option to lay out in lattice: more efficient
		AIFNeuron(	const Network &_netHost,
					const uint32_t _size, 
					const Polarity _pol,
				 	const Position minPos,
				 	const Position maxPos );

		void runForward();
		void pushBuffers();

    private: 
        AIFNeuron() 
        : AIFNeuron(Network(), 1, Polarity::NONE,
        Position(), Position()) {}

    friend class DataRecorder;
    friend class SynMatrices;

};

class InputNeuron : public GenericNeuron
{

	public:

		InputNeuron( 	const Network &_netHost,
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



    friend class SynMatrices;
};

#endif // NEURON_H_
