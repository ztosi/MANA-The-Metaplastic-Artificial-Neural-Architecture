#include <arrayfire.h>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"

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
void SynNormalizer::calcSatVals(const array &prefFRs,
								const array &_excSynScale,
								const array &_inhSynScale	)
{
	if (!allFlipped)
	{
		if (!allExcFlipped)
		{
			sValExc = (sValExc * excFlip)
				+ (!excFlip * (_excSynScale*omega_a/(1+exp(-omega_b*prefFRs)) - omega_c) );
			bool* aef = allTrue(excFlip).host<bool>();
			allExcFlipped = *aef;	
		}
		if (!allInhFlipped)
		{
			sValInh = (sValInh * inhFlip)
				+ (!inhFlip *  (_inhSynScale*omega_a/(1+exp(-omega_b*prefFRs)) - omega_c));
			bool* aif = allTrue(inhFlip).host<bool>();
			allInhFlipped = *aif;	
		}
		allFlipped = allExcFlipped && allInhFlipped;
	}
}

void SynNormalizer::perform(	const array &_excSynScale,
								const array &_inhSynScale	)
{
	uint32_t numExc = neuHost.incoExcSyns.getSize();
	uint32_t numInh = neuHost.incoInhSyns.getSize();

	array excSums = constant(0, neuHost.size, 1, f32);
	array inhSums = constant(0, neuHost.size, 1, f32);

	// Normalize Excitatory incoming synapses
	for(uint32_t i = 0; i < numExc; i++)
	{
		SynMatrices* syns = &(neuHost.incoExcSyns[i]);
		array su = scanByKey(syns->ijInds.col(1), wt_And_dw.col(0), AF_BINARY_ADD);
		su = su(syns->tarStartFin.col(1));
		excSums += su;

	}	
	e_triggered = (excSums >= sValExc) || e_triggered;
	array sVals = e_triggered * _excSynScale * sValExc/excSums;

	for(uint32_t i = 0; i < numExc; i++)
	{
		SynMatrices* syns = &(neuHost.incoExcSyns[i]);
		array fac = !e_triggered;
		fac = fac(*(syns->ijInds).col(1));
		fac += sVals;
		*(syns->wt_And_dw).col(0) = *(syns->wt_And_dw).col(0) * fac;
	}

		// Normalize Excitatory incoming synapses
	for(uint32_t i = 0; i < numInh; i++)
	{
		SynMatrices* syns = &(neuHost.incoInhSyns[i]);
		array su = scanByKey(syns->ijInds.col(1), wt_And_dw.col(0), AF_BINARY_ADD);
		su = su(syns->tarStartFin.col(1));
		inhSums += su;

	}	
	i_triggered = (inhSums >= sValInh) || i_triggered;
	sVals = i_triggered * _inhSynScale * sValInh/inhSums;

	for(uint32_t i = 0; i < numInh; i++)
	{
		SynMatrices* syns = &(neuHost.incoInhSyns[i]);
		array fac = !i_triggered;
		fac = fac(*(syns->ijInds).col(1));
		fac += sVals;
		*(syns->wt_And_dw).col(0) = *(syns->wt_And_dw).col(0) * fac;
	}

}