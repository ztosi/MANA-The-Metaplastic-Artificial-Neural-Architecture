#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "SynMatrices.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "SynActors.h"
#include "UDFPlasticity.h"
#include "Network.h"

#ifndef SOMODULE_H_
#define SOMODULE_H_

#define DEF_INH_TRIGGER 1.4f
#define DEF_EXC_TRIGGER 0.9f
#define DEF_IE_RATIO 0.2f

using namespace af;

////////////////////////////////////////////////////////////////////////////
// Self-Organizing Module Base Class
// An SOModule contains a source set of neurons, a target set of neurons,
// and a set of synapses connecting them. It also contains all the required
// classes to carry out the full set of self-organizing functions aside
// from synaptic pruning or growth. A module can be created from 
// 1 or 2 existing sets of neurons, attached to an extant Neu->syn->neu
// tuple or used to create an entire complete neu->syn->neu tuple with
// all the necessary plasticity components.
////////////////////////////////////////////////////////////////////////////

class Module
{

	public:

		Network &host;
		const uint32_t size;
		const uint32_t numExc;
		const uint32_t numInh;

		FREstimator getExcWatcher()
		{
			return *watcherExc;
		}
		FREstimator getExcWatcher()
		{
			return *watcherInh;
		}

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
		FREstimator* watcherExc;
		FREstimator* watcherInh;

	friend class DataRecorder;	
};


class SORN_Module 
{
	// TODO

	friend class DataRecorder;
};

class MANA_Module
{
		// TODO: More constructors, specify STDP, etc.

		MANA_Module(	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos	);

		MANA_Module (	const Network &_host,
						const uint32_t _size,
						const Position _minPos,
						const Position _maxPos, 
						const float _ieRatio	);
		~MANA_Module();

		void iterateOne();
		void runForward(uint32_t numIters);
		void setSynModFreq(uint32_t _iter_Interval);
		void setSynModFreq(double _time_Interval);

	private:

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


	friend class DataRecorder;

};

#endif // SOMODULE_H_