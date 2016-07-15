#include <arrayfire.h>
#include "neuron.h"




af::array neuron::updateAct(Network *root, Neuron *neurons)
{

	// Zeros out dV/dt for neurons in their refractory periods
	af::array online = (neurons->lastSpkTime + neurons->refP) < root->simTime;
	
	af::array dVmdt = ((neurons->V_rest - neurons->V_mem) + neurons->I_e
		+ neurons->I_i - neurons->w + neurons->ibg) / neurons->mCap;
	dVmdt = online * dVmdt * root->timeStep;

	af::array dwdt = (neurons->V_rest - neurons->V_mem) - neurons->w / neurons->tau_w;
	dwdt = dwdt * root->timeStep;

	neurons->V_mem += dVmdt;
	neurons->w += dwdt;

	af::array spks = neuron->V_mem > neuron->thresholds;

	neurons->V_mem = (neurons->V_mem * !spks) + (neurons->V_reset * spks);
	neurons->lastSpkTime = (neurons->lastSpkTime * !spks) + (root->simTime * spks);

	return spks;

}


void neuron::intrinsicPlasticity(Network *root, Neuron *neurons)
{

}

void neuron::homeostaticPlasticty(Network *root, Neuron *neurons)
{

}


void neuron::synapticNormalization(Network *root, Synapses *synG,
	Neurons *neurons, uint32_t nGroups)
{


	af::array sum = 
	gfor(seq i, neurons->size)
	{

	}
}

