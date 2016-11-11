#include <arrayfire.h>
#include <cstdint>
#include <vector>

#ifndef SYNMATRICES_H_
#define SYNMATRICES_H_

using namespace af;

const float MAX_WT = 20.0;
const float START_WT = 0.000001;
// Parameters for quartic fit to
// exponential weight restraint function
const float  P1 = -0.0001593;
const float  P2 = 0.001427;
const float  P3 = -0.0161;
const float  P4 = 1.019;

class Network;
class GenericNeuron;
class UDFPlasticity;
class STDP;
class Position;

#include "Neuron.hpp"

class SynMatrices {

	public:

		const Network &netHost;
		const GenericNeuron &srcHost;
		const GenericNeuron &tarHost;

		// All column major

		array* wt_And_dw;
		array* lastUp;
		array* lastArr;
		array* srcDlyInds;
		array* ijInds;
		array* tarStartFin; // change so that col 2 is just ends... in degree really isn't used much


		
		array* dlyArr;
		uint32_t dlyRange;

		const uint32_t maxDly;
		const uint32_t minDly;

		const uint32_t maxCap;

		Polarity srcPol;
		Polarity tarPol;

		static SynMatrices* connectNeurons(	GenericNeuron &_src,
											GenericNeuron &_tar,
											const STDP* _splas,
											const uint32_t _minDly,
											const uint32_t _maxDly,
											const bool useUDF );

		static uint32_t** calcDelayMat(	const GenericNeuron* src,
										const GenericNeuron* tar,
										const uint32_t _maxDly,
										const uint32_t _minDly	);

		static uint32_t calcDelay(	const Position &p1,
									const Position &p2,
									const uint32_t _maxDly,
                                    const float MAX_DIST );

		void propagateSelective();

		static array dampen(	const array &duration,
								const array &initVal,
								const array &dv	);

		static array dInteg(const array &val);

		bool isUsingUDF() { return usingUDF; }

		void setUsingUDF(const bool _usingUDF) { usingUDF = _usingUDF; }

		uint32_t getSize() { return size; }

	private:

		//float MAX_DIST;
		SynMatrices(const GenericNeuron &_src,
			 		const GenericNeuron &_tar, 
			 		const STDP *_splas,
			 		const uint32_t _maxDly,
			 		const uint32_t _minDly);
		STDP *splas;
		UDFPlasticity* udf;
		bool usingUDF;
		uint32_t size;

		friend class DataRecorder;
		friend class SynGrowthManager;

};

#endif // SYNMATRICES_H_
