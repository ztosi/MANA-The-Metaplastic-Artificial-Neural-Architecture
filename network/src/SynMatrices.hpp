#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include "Neuron.hpp"
#include "UDFPlasticity.hpp"
#include "STDP.hpp"
#include "Utils.hpp"


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

		const Network &netHost;
		const GenericNeuron &srcHost;
		const GenericNeuron &tarHost;

		// All column major
		std::vector<array> wt_And_dw; 
		std::vecotr<array> lastUp;
		std::vector<array> lastArrT;

		// Indices of the src neurons each connects from
		std::vector<array> indices;
		// Indices of the src neurons shifted based on
		// their delay (index in the delay spk train)
		std::vector<array> indicesActual;
		
		uint32_t** dlyMat;
		uint32_t dlyRange;

		const uint32_t minDly;

		const uint32_t maxCap;

		GenericNeuron::Polarity srcPol;
		GenericNeuron::Polarity tarPol;

		static SynMatrices* connectNeurons(	GenericNeuron &_src,
											GenericNeuron &_tar,
											const STDP &_splas,
											const uint32_t _minDly,
											const uint32_t _maxDly,
											const bool useUDF );

		static uint32_t** calcDelayMat(	const GenericNeuron* src,
										const GenericNeuron* tar,
										uint32_t _maxDly	);

		static uint32_t calcDelay(	const Position &p1,
									const Position &p2 	);

		void propagateSelective(const uint32_t _time,
								const float dt,
								const UDFPlasticity &udf);

		static array dampen(	const array &duration,
								const array &initVal,
								const array &dv	);

		static array dInteg(const array &val);

		bool isUsingUDF() { return usingUDF; }

		void setUsingUDF(const bool _usingUDF) { usingUDF = _usingUDF; }

	private:

		float MAX_DIST;
		SynMatrices( const GenericNeuron &_src,
			 const GenericNeuron &_tar, 
			 const STDP &_splas,
			 const uint32_t _maxDly,
			 const uint32_t _minDly);

		//std::vector<array> dlyChange;
		std::vector<array> masks;
		std::vector<array> result;
		STDP splas;
		UDFPlasticity* udf;
		bool usingUDF;

		friend class DataRecorder;
		friend class SynGrowthManager;

};

#endif // SYNMATRICES_H_