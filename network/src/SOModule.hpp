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
class SOModule {

	public:

		Network &host;
		AIFNeuron &excNeuGrp;
		AIFNeuron &inhNeuGrp;

		static SOModule* buildSOModule(	const Network &host,
										const uint32_t size,
										const Position minPos,
										const Position maxPos	);
		static SOModule* buildSOModule(	const Network &host,
										const uint32_t size,
										const Position minPos,
										const Position maxPos,
										const float ieRatio	);


		af::array iterateOne();
		af::array runForward(uint32_t numIters);

	private:
		
		SynMatrices* synGrps;
		HPComponent* hpComp;
		IPComponent* ipComp;
		UDFPlasticity* UDFComp;
		STDP* stdp;
		HpSynScaler* synScale;
		SynNormalizer* norman;

		SOModule(	const Network &host,
					const uint32_t size,
					const Position minPos,
					const Position maxPos	);
		SOModule (	const Network &host,
					const uint32_t size, 
					const Position minPos,
					const Position maxPos,
					const float ieRatio 	);



};

#endif // SOMODULE_H_