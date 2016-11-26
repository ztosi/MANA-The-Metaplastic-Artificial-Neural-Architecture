#include <arrayfire.h>
#include "Neuron.hpp"

#ifndef SYNACTOR_H_
#define SYNACTOR_H_

using namespace af;

//const float DEF_INH_MXMU = 1.4;
//const float DEF_EXC_MXMU = 0.9;
const float DEF_OMEGA_A = 300;
const float DEF_OMEGA_B = 0.1;
const float DEF_OMEGA_C = 100;
const float DEF_RHO = 5.0;

class DataRecorder;

class SynActor 
{
	// TODO: get rid of this unless I can find compelling 
	// subclasses other than SynNormalizer

	public:
		array getFullFlip() { return fullFlip; }

    protected:
        GenericNeuron &neuHost;
        SynActor(GenericNeuron & _neuHost) : neuHost(_neuHost) {}
		array fullFlip;
		bool allFlipped = 0;


	friend class DataRecorder;

};

class MANA_SynNormalizer : public SynActor 
{

	public:
		MANA_SynNormalizer(	ThresholdedNeuron &_neuHost);
		MANA_SynNormalizer(	ThresholdedNeuron &_neuHost,
                            const float _omega_a,
                            const float _omega_b,
                            const float _omega_c,
                            const float _rho	);
		//SynNormalizer(	const GenericNeuron &_neuHost,
		//				const float _omega_a,
		//				const float _omega_b,
		//				const array &_omega_c,
		//				const float _rho,
		//				const float _exc_maxMean,
		//				const float _inh_maxMean	);

        void perform(   const array &prefFRs,
                        const float lambda  );

	private:

		array excFlip;
		array inhFlip;
		array sValExc;
		array sValInh;
        array meanTh;
		array thExc;
		array thInh;
		array omega_c;

		float omega_a;
		float omega_b;
		float rho;

		bool allExcFlipped = false;
		bool allInhFlipped = false;
        bool allFlipped = false;

        void calcSatVals(   const array &prefFRs,
                            const array &_excSynScale,
                            const array &_inhSynScale	    );
                            
        void normalize(     const array &_excSynScale,
                            const array &_inhSynScale	    );

	friend class DataRecorder;

};

class SynNormSimple : public SynActor
{
	// TODO;
};

#endif // SYNACTORS_H_