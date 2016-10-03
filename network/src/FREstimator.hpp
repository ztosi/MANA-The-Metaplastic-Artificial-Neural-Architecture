#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "HPComponent.h"
#include "IPComponent.h"

#ifndef FRESTIMATOR_H_
#define FRESTIMATOR_H_

#define DEF_NUMER 1E4f
#define DEF_STATIC_TAU 2E4f

using namespace af;

class FREstimator
{

	public:

		FREstimator(	const array &_neuHost,
						const float _initVal,
						const float _tau	);

		virtual void estimate(const float dt);

	protected:

		AIFNeuron &neuHost;

		array epsilon;
		array nu_eps;
		array nu_E;

		const float tau;

	friend class IPComponent;
	friend class HPComponent;
	friend class DataRecorder;
};

// Same as FREstimator except takes in an IPComponent
// and uses target firing rates
class DynFREstimator : public FREstimator
{

	public:

		DynFREstimator(	const array &_neuHost,
						const IPComponent &_ipHost,
						const float _initVal,
						const float _tau	);

		void estimate(const float dt);

	private:

		array &ipHost;
		array pfrs;

	friend class IPComponent;
	friend class HPComponent;
	friend class DataRecorder;
};


#endif // FR_ESTIMATOR_H_