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

#define DEF_INH_TRIGGER 1.5
#define DEF_EXC_TRIGGER 1.1


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
		AIFNeuron &src;
		AIFNeuron &tar; 
		SynMatrices &synComp; // Interfaces with host.tarHost
		
		HPComponent* hpComp;
		IPComponent* ipComp;
		UDFPlasticity* UDFComp;
		STDP* stdp;


		SOModule(	const Network &host,
					const uint32_t srcSz,
					const uint32_t tarSz,
					const bool srcExc,
					const bool tarExc,
					const float sparsity);
		
		SOModule (	const Network &host
					const uint32_t stSize,
					const bool exc,
					const float sparsity);
		
		SOModule(	const Network &host,
					const AIFNeuron &src,
					const AIFNeuron &tar,
					const SynMatrices &synComp,
					const HPComponent* hpComp,
					const IPComponent* ipComp
					const UDFPlasticity* UDFComp,
					const STDP* stdp);

		void runForward(const uint32_t t, const float dt);


};

#endif // SOMODULE_H_