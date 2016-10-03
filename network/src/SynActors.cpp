#include <arrayfire.h>
#include "AIFNeuron.h"
#include "SynMatrices.h"
#include "HPComponent.h"

HpSynScaler::HpSynScaler(const AIFNeuron &_neuHost)
	: neuHost(_neuHost)
{

}

void HpSynScaler::perform()
{
	*thresh_e = (*thresh_e) * (*neuHost.eFlip)
		+ 
	*(neuHost.S_e) = af
}

SynNormalizer::SynNormalizer(const AIFNeuron &_neuHost)
	: neuHost(_neuHost)
{

}

// TODO: include new theta terms in normalization...
void SynNormalizer::perform()
{
	uint32_t numExc = neuHost.incoExcSyns.size();
	uint32_t numInh = neuHost.incoInhSyns.size();
	array nrmSum = constant(0, dim4(1, neuHost.size), f32);

	// Normalize Excitatory incoming synapses
	for(uint32_t i = 0; i < numExc; i++)
	{
		gfor (seq j, host.tar.size)
		{
			nrmSum(j) += sum(neuHost.incoExcSyns[i].wt_And_dw[j](span, 1)); 
		}
	}	
	e_triggered = (nrmSum >= sValExc) || e_triggered;
	array fac = sValExc / nrmSum;	
	for(uint32_t i = 0; i < numExc; i++)
	{
		gfor (seq j, host.tar.size)
		{
			array wt = neuHost.incoExcSyns[i].wt_And_dw[j](span, 1);
			wt = e_triggered * (wt * fac(j))
				+ !e_triggered * wt;
			neuHost.incoExcSyns[i].wt_And_dw[j](span, 1) = wt; 
		}
	}

	// Normalize Inhibitory incoming synapses
	nrmSum(span) = 0;
	for(uint32_t i = 0; i < numInh; i++)
	{
		gfor (seq j, host.tar.size)
		{
			nrmSum(j) += sum(neuHost.incoInhSyns[i].wt_And_dw[j](span, 1)); 
		}
	}
	i_triggered = (nrmSum >= sValInh) || i_triggered;
	fac = sValInh / nrmSum;	
	for(uint32_t i = 0; i < numInh; i++)
	{
		gfor (seq j, host.tar.size)
		{
			array wt = neuHost.incoInhSyns[i].wt_And_dw[j](span, 1);
			wt = i_triggered * (wt * fac(j))
				+ !i_triggered * wt;
			neuHost.incoInhSyns[i].wt_And_dw[j](span, 1) = wt; 
		}
	}

}