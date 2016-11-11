#include <arrayfire.h>
#include "Neuron.hpp"

#ifndef SYNACTOR_H_
#define SYNACTOR_H_

using namespace af;

const float DEF_INH_MXMU = 1.4;
const float DEF_EXC_MXMU = 0.9;
const float DEF_OMEGA_A = 300;
const float DEF_OMEGA_B = 0.1;
const float DEF_RHO = 5.0;

class GenericNeuron;
class DataRecorder;

class SynActor 
{
	// TODO: get rid of this unless I can find compelling 
	// subclasses other than SynNormalizer

	public:
		GenericNeuron &neuHost;
		
		array getFullFlip() { return fullFlip; }

	protected:

		array fullFlip;
		bool allFlipped = 0;


	friend class DataRecorder;

};

class SynNormalizer : public SynActor 
{

	public:

		SynNormalizer(	const GenericNeuron &_neuHost);
		SynNormalizer(	const GenericNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
						const array &_omega_c,
						const float _rho	);
		SynNormalizer(	const GenericNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
						const array &_omega_c,
						const float _rho,
						const float _exc_maxMean,
						const float _inh_maxMean	);

		void calcSatVals(	const array &prefFRs	);
		void perform(	const array &thExc,
						const array &thInh	);

	private:

		array excFlip;
		array iinhFlip;
		array sValExc;
		array sValInh;
		array thEx;
		array thIn;
		array omega_c;

		float omega_a;
		float omega_b;
		float rho;

		bool allExcFlipped = 0;
		bool allInhFlipped = 0;


	friend class DataRecorder;

};

class SynNormSimple : public SynActor
{
	// TODO;
};

#endif // SYNACTORS_H_