#include <arrayfire.h>
#include "Neuron.hpp"
#include "SynMatrices.hpp"

SynNormalizer::SynNormalizer(	const GenericNeuron &_neuHost	)
	: SynNormalizer(_neuHost, DEF_OMEGA_A, DEF_OMEGA_B, DEF_RHO,
		DEF_EXC_MXMU, DEF_INH_MXMU) {};

SynNormalizer::SynNormalizer(	const GenericNeuron &_neuHost,
								const float _omega_a, 
								const float _omega_b,
								const float _rho	)
	: SynNormalizer(_neuHost, _omega_a, _omega_b, _rho,
		DEF_EXC_MXMU, DEF_INH_MXMU) {};

SynNormalizer::SynNormalizer(	const GenericNeuron &_neuHost,
								const float _omega_a, 
								const float _omega_b,
								const float _rho,
								const float _exc_maxMean,
								const float _inh_maxMean	)
	: neuHost(_neuHost), omega_a(_omega_a), omega_b(_omega_b), rho(_rho),
	exc_maxMean(_exc_maxMean), inh_maxMean(_inh_maxMean),
	fullFlip(constant(0, dim4(_neuHost.size, 1), b8)),
	excFlip(constant(0, dim4(_neuHost.size, 1), b8)),
	inhFlip(constant(0, dim4(_neuHost.size, 1), b8)),
	sValExc(constant(0, dim4(_neuHost.size, 1), f32)),
	sValInh(constant(0, dim4(_neuHost.size, 1), f32)) {}


// Calculates the appropriate saturation values from the
// preferred firing rates of the neurons. Does not also
// factor in threshold-dependent synaptic scaling (i.e. 
// synaptic scaling as it relates to firing rate homeostasis).
// Also checks if all exc and inh have already flipped,
// (the sum of thein incoming exc/inh synapses has met or
// surpassed the saturation value [or max mean] at one 
// time in the past), but does not test which elements
// should be flipped. Returns after doing noting if
// all saturation values have been appropriately calculated
// (everyone flipped).
void SynNormalizer::calcSatVals( 	const array &prefFRs	)
{
	if (!allFlipped)
	{
		if (!allExcFlipped)
		{
			sValExc = (sValExc * excFlip)
				+ (!excFlip * (omega_a * prefFRs + omega_b));
			bool* aef = allTrue(excFlip).host<bool>();
			allExcFlipped = *aef;	
		}
		if (!allInhFlipped)
		{
			sValInh = (sValInh * inhFlip)
				+ (!inhFlip * (omega_a * prefFRs + omega_b));
			bool* aif = allTrue(inhFlip).host<bool>();
			allInhFlipped = *aif;	
		}
		allFlipped = allExcFlipped && allInhFlipped;
	}
}

void SynNormalizer::perform(	const array &thExc,
								const array &thInh	)
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