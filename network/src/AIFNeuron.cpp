#include <arrayfire.h>
#include "AIFNeuron.h"
#include "Utils.h"

using namespace af;

AIFNeuron::AIFNeuron(	const Network net,
					const uint32_t size, 
					const uint8_t _polarity,
				 	const Position minPos,
				 	const Position maxPos) : 
	online(constant(1, dim4(1, size), b8)),
	spks(constant(0, dim4(1,size), b8)),
	I_e(constant(0, dim4(1,size), f32)),
	I_i(constant(0, dim4(1,size), f32)),
	I_bg(constant(DEF_I_BG, dim4(1,size), f32)),
	S_e(constant(1, dim4(1,size), f32)),
	S_i(constant(1, dim4(1,size), f32)),
	Cm(polarity == 1 ? constant(DEF_CM_EXC, dim4(1,size), f32)) 
		:  constant(DEF_CM_INH, dim4(1,size), f32))),
	incoSyns(2) // Likely minimum 1 excitatory and 1 inhbitory

{
	V_mem = new array(constant(DEF_V_rest, dim4(1,size), f32));
	V_buff = new array(constant(DEF_V_rest, dim4(1,size), f32));
	w = new array(constant(DEF_V_rest, dim4(1,size), f32));
	w_buff = new array(constant(DEF_V_rest, dim4(1,size), f32));
	thresholds = new array(constant(INIT_THRESH, dim4(1,size), f32));
	lastSpkTime = new array(constant(DEF_V_rest, dim4(1,size), u32));
	lst_buff = new array(constant(DEF_V_rest, dim4(1,size), f32));
	if (_polarity==1) {
		adpt = new array(2*randu(dim4(1,size), f32)+7);
		polarity = 1;
		refP = DEF_E_REF;
	} else {
		adpt = new array(2*randu(dim4(1,size), f32)+6);
		polarity = 0;
		refP = DEF_I_REF;
	}

	exInDegs = new uint32_t[size];
	inInDegs = new uint32_t[size];
	outDegs = new uint32_t[size];

	x = new float[size];
	y = new float[size];
	z = new float[size];

	array X = ((maxPos.x-minPos.x)*randu(dim4(1, size)))+minPos.x;
	array Y = ((maxPos.y-minPos.y)*randu(dim4(1, size)))+minPos.y;
	array X = ((maxPos.z-minPos.z)*randu(dim4(1, size)))+minPos.z;

	X.host(x);
	Y.host(y);
	Z.host(z);

	iDecay = DEF_I_DEC;
	eDecay = DEF_E_DEC;
	noiseSD = DEF_NSD;
	V_rest = DEF_V_rest;
	V_reset = DEF_V_reset;
	tauW = DEF_TAU_W;

}

AIFNeuron::~AIFNeuron()
{
	delete[] exInDegs;
	delete[] inInDegs;
	delete[] outDegs;
	delete[] x;
	delete[] y;
	delete[] z;
	delete V_mem;
	delete V_buff;
	delete w;
	delete w_buff;
	delete thresholds;
	delete lastSpkTime;
	delete lst_buff;
	delete adpt;
	
}


void AIFNeuron::runForward(const uint32_t t, const float dt) {

	online = t > (*lastSpkTime + refP); // disable refractory

	*V_buff = (*V_mem) + (dt * (( (V_rest-(*V_mem)) // membrane leak
		+ I_e*S_e - I_i*S_i // Scaled synapse currents
		 + I_bg + randn(dim4(1, size), f32)*noiseSD 
		 	- (*w)) // Adapt 
			/Cm) // capacitance
			* online); // Only non refractory

	// determine who will spike on next time-step
	spks = *V_buff > *thresholds; 

	// calculate adaptation
	*w_buff = *w + (dt * (-(*w)/tauW + (*spks) * (*adpt)));

	// calculate the new last spike times
	*lst_buff = (*lastSpkTime * !spks) + (t * spks);

	I_e -= dt * I_e/eDecay;
	I_i -= dt * I_i/iDecay;

}

void AIFNeuron::pushBuffers() {
	
	array* holder = V_mem;
	V_mem = V_buff;
	V_buff = holder;

	holder = w;
	w = w_buff;
	w_buff = holder;

	holder = lastSpkTime;
	lastSpkTime = lst_buff;
	lst_buff = holder;

}

Position* AIFNeuron::getPositions()
{
	Positions* p = new Positions[size];
	for (uint32_t i=0; i < size; i++)
	{
		p[i] = Position(x[i], y[i], z[i]);
	}
	return p;
}

Position AIFNeuron::getPosition(uint32_t index)
{
	return Position(x[index], y[index], z[index]);
}

