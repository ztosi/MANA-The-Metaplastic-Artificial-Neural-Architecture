#include <arrayfire.h>
#include <cstring>
#include <cstdint>
#include <stdio.h>
#include <vector>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/Utils.hpp"
#include "../include/Network.hpp"

using namespace af;

AIFNeuron::AIFNeuron(	const Network &_host,
			const uint32_t _size,
			const Polarity _pol,
		 	const Position  minPos,
		 	const Position  maxPos ) : host(_host),
		 			online(constant(1, dim4(size, 1), b8)),
					spks(constant(0, dim4(size, 1), b8)),
					I_e(constant(0, dim4(size, 1), f32)),
					I_i(constant(0, dim4(size, 1), f32)),
					I_bg(constant(DEF_I_BG, dim4(size, 1), f32)),
					Cm(_pol == Polarity::EXC ?
						constant(DEF_CM_EXC, dim4(size, 1), f32) :
						constant(DEF_CM_INH, dim4(size, 1), f32)),
					V_mem(constant(DEF_V_rest, dim4(size, 1), f32)),
					V_buff(constant(DEF_V_rest, dim4(size, 1), f32)),
					w(constant(DEF_V_rest, dim4(size, 1), f32)),
					w_buff(constant(DEF_V_rest, dim4(size, 1), f32)),
					thresholds(constant(INIT_THRESH, dim4(size, 1), f32)),
					lastSpkTime(constant(DEF_V_rest, dim4(size, 1), u32)),
					lst_buff(constant(DEF_V_rest, dim4(size, 1), u32)),
					refP(_pol == Polarity::EXC ?
							(uint32_t)(DEF_E_REF/_host.dt) :
							(uint32_t)(DEF_I_REF/_host.dt)),
					adpt(_pol == Polarity::EXC ?
							DEF_E_ADPT :DEF_I_ADPT)

{
	exInDegs = new uint32_t[size];
	inInDegs = new uint32_t[size];
	outDegs = new uint32_t[size];

	x = new float[size];
	y = new float[size];
	z = new float[size];

	array X = ((maxPos.x-minPos.x)*randu(dim4(size, 1)))+minPos.x;
	array Y = ((maxPos.y-minPos.y)*randu(dim4(size, 1)))+minPos.y;
	array Z = ((maxPos.z-minPos.z)*randu(dim4(size, 1)))+minPos.z;

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
}


void AIFNeuron::runForward()
{

	uint32_t t = (uint32_t)(host.getTime());
	float dt = host.dt;

	online = t > (lastSpkTime + refP); // disable refractory

	V_buff = (V_mem) + (dt * (( (V_rest-(V_mem)) // membrane leak
		+ I_e - I_i // Scaled synapse currents
		 + I_bg + randn(dim4(size, 1), f32)*noiseSD 
		 	- w) / Cm)
			* online); // Only non refractory

	// determine who will spike on next time-step
	spks = V_buff > thresholds; 

	// calculate adaptation
	w_buff = w + (dt * (-(w)/tauW + (spks) * (adpt)));

	// calculate the new last spike times
	lst_buff = (lastSpkTime * !spks) + (t * spks);

	I_e -= dt * I_e/eDecay;
	I_i -= dt * I_i/iDecay;

}

void AIFNeuron::pushBuffers() {
	V_mem = V_buff;
	w = w_buff;
	lastSpkTime = lst_buff;
	spkHistory = af::shift(spkHistory, size);
	spkHistory(seq(size)) = spks;
}

Position* GenericNeuron::getPositions()
{
	Position* p = new Position[size];
	for (uint32_t i=0; i < size; i++)
	{
		p[i] = Position(x[i], y[i], z[i]);
	}
	return p;
}

Position GenericNeuron::getPosition(uint32_t index)
{
	return Position(x[index], y[index], z[index]);
}

