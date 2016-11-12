#include <arrayfire.h>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/SynActors.hpp"

MANA_SynNormalizer::MANA_SynNormalizer(	ThresholdedNeuron &_neuHost	)
	: MANA_SynNormalizer(_neuHost, DEF_OMEGA_A, DEF_OMEGA_B, DEF_OMEGA_C, DEF_RHO) {};

MANA_SynNormalizer::MANA_SynNormalizer(	ThresholdedNeuron &_neuHost,
								const float _omega_a, 
								const float _omega_b,
                                const float _omega_c,
								const float _rho    )
	: SynActor(_neuHost), omega_a(_omega_a), omega_b(_omega_b),
    omega_c(constant(_omega_c, dim4(_neuHost.size, 1), f32)), rho(_rho),
	excFlip(constant(0, dim4(_neuHost.size, 1), b8)),
	inhFlip(constant(0, dim4(_neuHost.size, 1), b8)),
	sValExc(constant(0, dim4(_neuHost.size, 1), f32)),
	sValInh(constant(0, dim4(_neuHost.size, 1), f32)),
    thExc(_neuHost.thresholds.copy()),
	thInh(_neuHost.thresholds.copy()),
    meanTh(_neuHost.thresholds.copy()){}

void MANA_SynNormalizer::perform(   const array &prefFRs,
                                    const float lambda  )
{
    
    ThresholdedNeuron* localHost = dynamic_cast<ThresholdedNeuron*>(&neuHost);
    meanTh = (localHost->thresholds * (neuHost.netHost.dt/lambda)) 
        + (meanTh * (1 -(neuHost.netHost.dt/lambda)));
    if (!allExcFlipped) {
        thExc = thExc * excFlip + meanTh * !excFlip;
    }  
    if (!allInhFlipped) {
        thInh = thInh * inhFlip + meanTh * !inhFlip;
    }   
    array excSynScale = exp((thExc - localHost->thresholds)/rho);   
    array inhSynScale = exp((localHost->thresholds - thInh)/rho);  
    calcSatVals(prefFRs, excSynScale, inhSynScale);
    normalize(excSynScale, inhSynScale);
    
}

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
void MANA_SynNormalizer::calcSatVals(const array &prefFRs,
								const array &_excSynScale,
								const array &_inhSynScale	)
{
    // TODO: AUDIT ME!!!
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

// TODO: Decide where to calculate exc & inh Syn Scales
void MANA_SynNormalizer::normalize(	const array &_excSynScale,
                                    const array &_inhSynScale	)
{
	uint32_t numExc = neuHost.incoExcSyns.size();
	uint32_t numInh = neuHost.incoInhSyns.size();

	array excSums = constant(0, neuHost.size, 1, f32);
	array inhSums = constant(0, neuHost.size, 1, f32);

	// Normalize Excitatory incoming synapses
	for(uint32_t i = 0; i < numExc; i++)
	{
		SynMatrices* syns = neuHost.incoExcSyns[i];
		array su = scanByKey(syns->ijInds->col(1), syns->wt_And_dw->col(0), AF_BINARY_ADD);
        array ends = syns->tarStartFin->col(1);
		su = su(ends);
		excSums += su;

	}	
	excFlip = (excSums >= sValExc) || excFlip;
	array sVals = excFlip * _excSynScale * sValExc/excSums;

	for(uint32_t i = 0; i < numExc; i++)
	{
		SynMatrices* syns = neuHost.incoExcSyns[i];
		array fac = !excFlip;
        array j_inds = syns->ijInds->col(1);
		fac = fac(j_inds);
		fac += sVals;
		syns->wt_And_dw->col(0) = syns->wt_And_dw->col(0) * fac;
	}

		// Normalize Excitatory incoming synapses
	for(uint32_t i = 0; i < numInh; i++)
	{
		SynMatrices* syns = neuHost.incoInhSyns[i];
		array su = scanByKey(syns->ijInds->col(1), syns->wt_And_dw->col(0), AF_BINARY_ADD);
        array ends = syns->tarStartFin->col(1);
		su = su(ends);
		inhSums += su;

	}	
	inhFlip = (inhSums >= sValInh) || inhFlip;
	sVals = inhFlip * _inhSynScale * sValInh/inhSums;

	for(uint32_t i = 0; i < numInh; i++)
	{
		SynMatrices* syns = neuHost.incoInhSyns[i];
		array fac = !inhFlip;
        array j_inds = syns->ijInds->col(1);
		fac = fac(j_inds);
		fac += sVals;
		syns->wt_And_dw->col(0) = syns->wt_And_dw->col(0) * fac;
	}

}