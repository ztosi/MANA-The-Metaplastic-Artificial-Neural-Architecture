#include <arrayfire.h>
#include "AIFNeuron.h"
#include "SynMatrices.h"
#include "HPComponent.h"

#ifndef SynActors_H_
#define SynActors_H_

#define DEF_INH_TRIGGER 1.5
#define DEF_EXC_TRIGGER 1.1

class HpSynScaler {

	public:

		SynMatrices &synHost; // Interfaces with host.tarHost
		HPComponent &hpHost;

		af::array &thresh_e;
		af::array &thresh_i;

		float rho;

		HpSynScaler(const SynMatrices &hostSyns); //hp
		~HpSynScaler();
		void perform();

};

class SynNormalizer {

	public:

		AIFNeuron &neuHost;

		af::array e_triggered;
		af::array i_triggered;

		float e_trigger;
		float i_trigger;


		SynNormalizer(const AIFNeuron &hostNeu);

		void perform();

};

#endif // SYNACTORS_H_