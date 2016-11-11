#include <arrayfire.h>
#include <cstdint>
#include <math.h>
#include "Network.hpp"
#include "Neuron.hpp"
#include "SynMatrices.hpp"

#ifndef STDP_H_
#define STDP_H_


const float DEF_HEBB = 1.0;
const float DEF_EE_W_PLUS = 5.6;
const float DEF_EE_W_MINUS = 0.9;
const float DEF_EI_W_PLUS = 5.3;
const float DEF_EI_W_MINUS = 1;
const float DEF_IE_W_PLUS = 1.6;
const float DEF_IE_W_MINUS = 1.65;
const float DEF_II_W_PLUS = 1.2;
const float DEF_II_W_MINUS = 1.4;

const float DEF_EE_TAU_P = 25.0;
const float DEF_EE_TAU_M = 100.0;
const float DEF_EI_TAU_P = 20.0;
const float DEF_EI_TAU_M = 100.0;

const float DEF_IE_SIG = 24.0;
const float DEF_II_SIG = 12.0; 

const float DEF_A = 30.0;
const float DEF_INIT_ETA = 2.5E-7;


class Network;
class GenericNeuron;
class DataRecorder;

class STDP 
{

	public:
		const Network &host;
		const GenericNeuron::Polarity srcPol;
		const GenericNeuron::Polarity tarPol;

		//SynMatrices &host;
		virtual void postTrigger(	af::array &neg_deltas	)=0;
		virtual void preTrigger(	af::array &neg_deltas	)=0;
	protected:

		float eta;


	friend class DataRecorder;
};

class StandardSTDP : public STDP 
{

	public:

		StandardSTDP(	const Network &_host,
						const GenericNeuron::Polarity _srcPol,
						const GenericNeuron::Polarity _tarPol,
						const float _eta);

		StandardSTDP(	const Network &_host,
						const GenericNeuron::Polarity _srcPol,
						const GenericNeuron::Polarity _tarPol,
						const float _eta,
						const bool _hebbian);

		StandardSTDP(	const Network &_host,
						const GenericNeuron::Polarity _srcPol,
						const GenericNeuron::Polarity _tarPol,
						const float _eta,
						const bool _hebbian,
						const float _w_p,
						const float _w_m,
						const float _tau_p
						const float _tau_m	);

		//SynMatrices &host;
		void postTrigger(	af::array &neg_deltas	);
		void preTrigger(	af::array &neg_deltas	);

	private:

		bool hebbian;
		float w_p;
		float w_m;
		float tau_p;
		float tau_m;


	friend class DataRecorder;
};

class MexicanHatSTDP : public STDP
{

	public:

		MexicanHatSTDP(	const Network &_host,
						const GenericNeuron::Polarity _srcPol,
		 				const GenericNeuron::Polarity _tarPol,
		 				const float _eta 	);

		MexicanHatSTDP(	const Network &_host,
						const GenericNeuron::Polarity _srcPol,
		 				const GenericNeuron::Polarity _tarPol,
		 				const float _eta,
		 				const float _a,
		 				const float _sigma 	);


		void postTrigger(	af::array &neg_deltas	);
		void preTrigger(	af::array &neg_deltas	);

		void setSigma(const float _s) 
		{
			sigma = _s;
			// Pre-compute terms of the mexican-hat wavelet for speed...
			sigma_sq = _s * _s;
			nrmTerm = 2.0f/((float) sqrt(3.0f*_s) * pi4thRt);
		}
		float getSigma(){ return sigma; }
		void setA(const float _a) { a = _a }
		float getA() { return a; }

	private:

		float a;
		float sigma;
		float sigma_sq;
		static const float pi4thRt = (float) pow(3.1415, 0.25);
		float nrmTerm;

	friend class DataRecorder;
};

#endif // STDP_H_