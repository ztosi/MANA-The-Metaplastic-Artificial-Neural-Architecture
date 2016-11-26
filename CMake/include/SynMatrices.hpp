#include <arrayfire.h>
#include <cstdint>
#include <vector>
#include "Neuron.hpp"

#ifndef SYNMATRICES_H_
#define SYNMATRICES_H_

const float MAX_WT = 20.0;
const float START_WT = 0.000001;
// Parameters for quartic fit to
// exponential weight restraint function
const float  P1 = -0.0001593;
const float  P2 = 0.001427;
const float  P3 = -0.0161;
const float  P4 = 1.019;

using namespace af;

class Network;
class UDFPlasticity;
class STDP;
class Position;

class SynMatrices {

	public:

		const Network &netHost;
		GenericNeuron &srcHost;
		GenericNeuron &tarHost;

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

		Polarity srcPol;
		Polarity tarPol;

        static SynMatrices* connectNeurons(     GenericNeuron &_src,
                                                GenericNeuron &_tar,
                                                STDP* _splas,
                                                const uint32_t _minDly,
                                                const uint32_t _maxDly,
                                                const float initDensity,
                                                const bool useUDF   );

        static void calcDelayMat(   const GenericNeuron* src,
                                    const GenericNeuron* tar,
                                    const uint32_t _maxDly,
                                    const uint32_t _minDly,
                                    uint32_t* dlys  );

        static uint32_t calcDelay(  const Position &p1,
                                    const Position &p2,
                                    const uint32_t _maxDly,
                                    const float _maxDist    );

		void propagate();

		static array dampen(	const array &duration,
								const array &initVal,
								const array &dv	);

		static array dInteg(const array &val);

		bool isUsingUDF() const { return usingUDF; }

		void setUsingUDF(const bool _usingUDF) { usingUDF = _usingUDF; }

		uint32_t getSize() const { return size; }

	private:

		//float MAX_DIST;
		SynMatrices(GenericNeuron &_src,
			 		GenericNeuron &_tar, 
			 		STDP *_splas,
			 		const uint32_t _maxDly,
			 		const uint32_t _minDly,
                    const float initDensity);
		STDP *splas;
		UDFPlasticity* udf;
		bool usingUDF;
		uint32_t size;

		friend class DataRecorder;
		friend class SynGrowthManager;

};

#endif // SYNMATRICES_H_
