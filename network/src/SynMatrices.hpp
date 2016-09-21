#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include <math.h>

#ifndef SYNMATRICES_H_
#define SYNMATRICES_H_

#define MAX_WT 20f
#define START_WT 0.00001f
#define P1 -0.0001593f
#define P2 0.001427f
#define P3 -0.0161f
#define P4 1.019f

using namespace af;

class SynMatrices {

	public:

		AIFNeuron &srcHost;
		AIFNeuron &tarHost;
		Spk_Delay_Mngr &manager;


		std::vector<array> wt_And_dw; // column major
		std::vecotr<array> lastUp;
		std::vector<array> lastArrT;
		



		std::vector<array> indices;
		std::vector<array> indicesActual;
		std::vector<array> ptrs;




		std::vector<array> dlyChange;
		std::vector<array> masks;

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
		array dampen(const array duration, const array initVal, const array dv);
		array dampen(const array initVal, const array dv);


};

#endif // SYNMATRICES_H_