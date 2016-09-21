#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <math.h>

#ifndef SYNMATRICES_H_
#define SYNMATRICES_H_

#define MAX_WT 20f
#define START_WT 0.00001f

using namespace af;

class SynMatrices {

	public:

		AIFNeuron &srcHost;
		AIFNeuron &tarHost;
		Spk_Delay_Mngr &manager;

		std::vector<array> spWtMat;
		std::vector<array> spdWMat;
		std::vecotr<array> lastUp;
		std::vector<array> lastArrT;

		std::vector<array> result;
		
		uint32_t** dlyMat;

		uint32_t dlyRange;
		uint32_t minDly;

		uint8_t srcPol;
		uint8_t tarPol;

		SynMatrices( const AIFNeuron &src,
					 const AIFNeuron &tar, 
					 const Spk_Delay_Mngr &_manager,
					 const uint32_t _maxDly,
					 const uint32_t _minDly);

		void changeWtsConstrained(); // TODO: Figure out how to make constrained wt Updates less taxing...
		void changeWtsConstrained(const array &delta_W);
		uint32_t** calcDelayMat(const AIFNeuron* src, const AIFNeuron* tar, uint32_t maxDly);
		void propagate(const uint32_t time, const float dt);

};

#endif // SYNMATRICES_H_