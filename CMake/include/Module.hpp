#include <arrayfire.h>
#include <cstdint>
#include "Neuron.hpp"
#include "SynMatrices.hpp"
#include "HPComponent.hpp"
#include "IPComponent.hpp"
#include "SynActors.hpp"
#include "UDFPlasticity.hpp"
#include "Utils.hpp"

#ifndef MODULE_H_
#define MODULE_H_

const float DEF_IE_RATIO = 0.2;

using namespace af;

class Network; // Forward declaration

class Module
{

	public:

		const Network &netHost;
		const uint32_t size;
		const uint32_t numExc;
		const uint32_t numInh;
        Position minPos;
        Position maxPos;
        bool isUpdateComplete() { return updateComplete; }
        virtual void iterate_one()=0;
        virtual void push_buffers() = 0;

    protected:
    
		Module(	const Network &_netHost,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos	);

		Module (	const Network &_netHost,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos, 
						const float _ieRatio	);
		
		GenericNeuron *excNeuGrp = NULL;
		GenericNeuron *inhNeuGrp = NULL;
		std::vector<SynMatrices*> synGrps; 
        bool updateComplete;

	friend class DataRecorder;	
};


class SORN_Module : public Module
{
	// TODO

	friend class DataRecorder;
};

const float DEF_START_LAMB = 1E4;
const float DEF_END_LAMB = 1E5;
const float LAMB_DEC = 1E-5;

class MANA_Module : public Module
{
public:
    // TODO: More constructors, specify STDP, etc.
    static MANA_Module* buildMANA_Module(const Network& _host,
        const uint32_t _size,
        const Position _minPos,
        const Position _maxPos,
        const float _ieRatio);

    static MANA_Module*
    buildMANA_Module(const Network& _host, const uint32_t _size, const Position _minPos, const Position _maxPos);

    ~MANA_Module();

    void iterate_one();
    void runForward(uint32_t numIters);
    void setSynModFreq(uint32_t _iter_Interval);
    void setSynModFreq(double _time_Interval);
    void push_buffers();

private:
    MANA_Module(const Network& _host, const uint32_t _size, const Position _minPos, const Position _maxPos);

    MANA_Module(const Network& _host,
        const uint32_t _size,
        const Position _minPos,
        const Position _maxPos,
        const float _ieRatio);

    HPComponent* hpExc;
    HPComponent* hpInh;

    IPComponent* ipExc;
    IPComponent* ipInh;

    MANA_SynNormalizer* sNrmExc;
    MANA_SynNormalizer* sNrmInh;

    UDFPlasticity* eeUDF;
    UDFPlasticity* eiUDF;
    UDFPlasticity* ieUDF;
    UDFPlasticity* iiUDF;

    float lambda;

    friend class DataRecorder;
};

#endif // MODULE_H_
