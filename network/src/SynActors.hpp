#include <arrayfire.h>
#include "AIFNeuron.h"
#include "SynMatrices.h"
#include "HPComponent.h"

#ifndef SYNACTOR_H_
#define SYNACTOR_H_

#define DEF_INH_TRIGGER 1.4
#define DEF_EXC_TRIGGER 0.9

using namespace af;

class SynActor 
{

	public:
		AIFNeuron &neuHost;
		
		array getFullFlip() { return fullFlip; }

	protected:

		array fullFlip;


	friend class DataRecorder;

};

//class HpSynScaler : public SynActor 
//{

//	public:
		
//		HPComponent &hpHost;
//
//		HpSynScaler(	const AIFNeuron &_neuHost,
//						const HPComponent &_hpHost	); //hp
//		HpSynScaler(	const AIFNeuron &_neuHost,
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

		SynNormalizer(const AIFNeuron &_neuHost);
		SynNormalizer(	const AIFNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
						const float _rho	);
		SynNormalizer(	const AIFNeuron &_neuHost,
						const float _omega_a,
						const float _omega_b,
						const float e_maxMean,
						const float i_maxMean,
						const float _rho	);

		void perform();

	private:

		array eFlip;
		array iFlip;
		array sValExc;
		array sValInh;
		array thExcF;
		array thInhF;

		const float omega_a;
		const float omega_b;
		const float rho;
		const float e_maxMean;
		const float i_maxMean;


	friend class DataRecorder;

};

class SynNormSimple : public SynActor

#endif // SYNACTORS_H_