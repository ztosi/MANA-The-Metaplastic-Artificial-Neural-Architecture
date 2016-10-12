#include <arrayfire.h>
#include "Neuron.hpp"

#ifndef SYNACTOR_H_
#define SYNACTOR_H_

#define DEF_INH_MXMU 1.4f
#define DEF_EXC_MXMU 0.9f
#define DEF_OMEGA_A 5.0f
#define DEF_OMEGA_B 100.0f
#define DEF_RHO 5.0f

using namespace af;

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

//class HpSynScaler : public SynActor 
//{

//	public:
		
//		HPComponent &hpHost;
//
//		HpSynScaler(	const GenericNeuron &_neuHost,
//						const HPComponent &_hpHost	); //hp
//		HpSynScaler(	const GenericNeuron &_neuHost,
//						const HPComponent &_hpHost,
//						const float _rho	); //hp
//		~HpSynScaler();
//		void perform();
//
//	private:
//
//		array thresh_e;
//		array thresh_i;
//
//		float rho;

//	friend class DataRecorder;

//};

class SynNormalizer : public SynActor 
{

	public:

		SynNormalizer(	const GenericNeuron &_neuHost);
		SynNormalizer(	const GenericNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
						const float _rho	);
		SynNormalizer(	const GenericNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
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

		const float omega_a;
		const float omega_b;
		const float rho;
		const float exc_maxMean;
		const float inh_maxMean;

		bool allExcFlipped = 0;
		bool allInhFlipped = 0;


	friend class DataRecorder;

};

class SynNormSimple : public SynActor
{
	// TODO;
};

#endif // SYNACTORS_H_