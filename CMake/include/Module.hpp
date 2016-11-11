#include <arrayfire.h>
#include <cstdint>

#ifndef MODULE_H_
#define MODULE_H_

const float DEF_INH_TRIGGER 1.4;
const float DEF_EXC_TRIGGER 0.9;
const float DEF_IE_RATIO 0.2;

using namespace af;

class GenericNeuron;
class AIFNeuron;
class SynMatrices;
class HPComponent;
class IPComponent;
class SynActors;
class UDFPlasticity;
class Network;

class Module
{

	public:

		Network &host;
		const uint32_t size;
		const uint32_t numExc;
		const uint32_t numInh;

		Module(	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos	);

		Module (	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos, 
						const float _ieRatio	);

	protected:
		
		AIFNeuron &excNeuGrp;
		AIFNeuron &inhNeuGrp;
		std::vector<SynMatrices*> synGrps; 

	friend class DataRecorder;	
};


class SORN_Module 
{
	// TODO

	friend class DataRecorder;
};

const float DEF_START_LAMB = 1E4;
const float DEF_END_LAMB = 1E5;

class MANA_Module
{
		// TODO: More constructors, specify STDP, etc.
		static MANA_Module* buildMANA_Module(	const Network &_host,
												const uint32_t _size,
												const Position _minPos,
												const Position _maxPos,
												const float _dt		);

		~MANA_Module();

		void iterateOne();
		void runForward(uint32_t numIters);
		void setSynModFreq(uint32_t _iter_Interval);
		void setSynModFreq(double _time_Interval);

	private:

		MANA_Module(	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos	);

		MANA_Module (	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos, 
						const float _ieRatio	);

		HPComponent* hpExc;
		HPComponent* hpInh;

		IPComponent* ipExc;
		IPComponent* ipInh;

		SynNormalizer* sNrmExc;
		SynNormalizer* sNrmInh;

		UDFPlasticity* eeUDF;
		UDFPlasticity* eiUDF;
		UDFPlasticity* ieUDF;
		UDFPlasticity* iiUDF;

		array meanThresh;

		float lambda;


	friend class DataRecorder;

};

#endif // MODULE_H_
