#include <arrayfire.h>
#include <cstring>
#include <cstdint>
#include <stdio.h>
#include <vector>
#include <iostream>
#include <fstream>
#include "../include/Neuron.hpp"
#include "../include/SynMatrices.hpp"
#include "../include/Utils.hpp"
#include "../include/Network.hpp"

using namespace af;

GenericNeuron::GenericNeuron(   const Network& _netHost,
                                const uint32_t _size,
                                const Polarity _pol,
                                const Position _minPos,
                                const Position _maxPos)
    : netHost(_netHost)
    , spks(constant(0, dim4(_size, 1), b8))
    , lastSpkTime(constant(0, dim4(_size, 1), u32))
    , lst_buff(constant(0, dim4(_size, 1), u32))
    , spkHistory(constant(0, dim4(_size * ((uint32_t)(_netHost.maxDelay / _netHost.dt)), 1), b8))
    , epsilon(constant(0, dim4(_size, 1), f32))
    , nu_hat(constant(0, dim4(_size, 1), f32))
    , I_e(constant(0, dim4(_size, 1), f32))
    , I_i(constant(0, dim4(_size, 1), f32))
    , tauEps(DEF_TAU_EPS)
    , size(_size)
    , pol(_pol)
{
    exInDegs = new uint32_t[size];
    inInDegs = new uint32_t[size];
    outDegs = new uint32_t[size];

    x = new float[size];
    y = new float[size];
    z = new float[size];

    array X = ((_maxPos.getX() - _minPos.getX()) * randu(dim4(size, 1))) + _minPos.getX();
    array Y = ((_maxPos.getY() - _minPos.getY()) * randu(dim4(size, 1))) + _minPos.getY();
    array Z = ((_maxPos.getZ() - _minPos.getZ()) * randu(dim4(size, 1))) + _minPos.getZ();

    X.host(x);
    Y.host(y);
    Z.host(z);
}

AIFNeuron::AIFNeuron(const Network& _netHost,
                     const uint32_t _size,
                     const Polarity _pol,
                     const Position _minPos,
                     const Position _maxPos)

    : ThresholdedNeuron(_netHost, _size, _pol, INIT_THRESH, _minPos, _maxPos)
    , V_mem(constant(DEF_V_rest, dim4(_size, 1), f32))
    , V_buff(constant(DEF_V_rest, dim4(_size, 1), f32))
    , w(constant(DEF_V_rest, dim4(_size, 1), f32))
    , w_buff(constant(DEF_V_rest, dim4(_size, 1), f32))
    , I_bg(constant(DEF_I_BG, dim4(_size, 1), f32))
    , online(constant(1, dim4(_size, 1), b8))
    , Cm(_pol == Polarity::EXC ? constant(DEF_CM_EXC, dim4(_size, 1), f32) : constant(DEF_CM_INH, dim4(_size, 1), f32))
    , refP(_pol == Polarity::EXC ? (uint32_t)(DEF_E_REF / _netHost.dt) : (uint32_t)(DEF_I_REF / _netHost.dt))
    , adpt(_pol == Polarity::EXC ? DEF_E_ADPT : DEF_I_ADPT)
    , iDecay(DEF_I_DEC)
    , eDecay(DEF_E_DEC)
    , V_rest(DEF_V_rest)
    , V_reset(DEF_V_reset)
    , tauW(DEF_TAU_W)
    , noiseSD(DEF_NSD)
{
}

void AIFNeuron::runForward()
{
    updateComplete = false;

    uint32_t t = (uint32_t)(netHost.getTime());
    float dt = netHost.dt;

    online = t > (lastSpkTime + refP); // disable refractory

    V_buff = (V_mem) + (dt * (((V_rest - (V_mem)) // membrane leak
                               +
                               I_e - I_i // Scaled synapse currents
                               +
                               I_bg + randn(dim4(size, 1), f32) * noiseSD - w) /
                              Cm) *
                        online); // Only non refractory

    // determine who will spike on next time-step
    spks = V_buff > thresholds;

    // calculate adaptation
    w_buff = w + (dt * (-(w) / tauW + (spks) * (adpt)));

    // calculate the new last spike times
    lst_buff = (lastSpkTime * !spks) + (t * spks);

    I_e -= dt * I_e / eDecay;
    I_i -= dt * I_i / iDecay;

    updateComplete = true;
}

void AIFNeuron::pushBuffers()
{
    V_mem = V_buff;
    w = w_buff;
    lastSpkTime = lst_buff;
    spkHistory = af::shift(spkHistory, size);
    spkHistory(seq(size)) = spks;
}

Position* GenericNeuron::getPositions()
{
    Position* p = new Position[size];
    for(uint32_t i = 0; i < size; i++) {
        p[i] = Position(x[i], y[i], z[i]);
    }
    return p;
}

Position GenericNeuron::getPosition(uint32_t index)
{
    return Position(x[index], y[index], z[index]);
}

/*
* Produces a defensive copy of the exictatory in-degrees.
*
* deallocate using delete[]
*
* */
uint32_t* GenericNeuron::getExcInDegs()
{
    uint32_t* arr = new uint32_t[size];
    std::memcpy(arr, exInDegs, sizeof(uint32_t) * size);
    return arr;
}

/*
* Produces a defensive copy of the Inhibitory in-degrees.
*
* deallocate using delete[]
*
* */
uint32_t* GenericNeuron::getInhInDegs()
{
    uint32_t* arr = new uint32_t[size];
    std::memcpy(arr, inInDegs, size * sizeof(uint32_t));
    return arr;
}

/*
* Produces a defensive copy of the out-degrees.
*
* deallocate using delete[]
*
* */
uint32_t* GenericNeuron::getOutDegs()
{
    uint32_t* arr = new uint32_t[size];
    std::memcpy(arr, outDegs, size * sizeof(uint32_t));
    return arr;
}

void GenericNeuron::addIncExcSyn(SynMatrices& syn)
{
    incoExcSyns.push_back(&syn);
}

void GenericNeuron::addIncInhSyn(SynMatrices& syn)
{
    incoInhSyns.push_back(&syn);
}

InputNeuron::~InputNeuron()
{
    delete [] trainSizes;
    delete [] indexPtrs;
    for(uint32_t i = 0; i < size; i++)
    {
        free(spkScript[i]);
    }
    free(spkScript);
}

InputNeuron::InputNeuron(const Network& _netHost,
                         const uint32_t _size,
                         const Position _minPos,
                         const Position _maxPos,
                         uint32_t* _trainSizes,
                         uint32_t** _spkScript,
                         const Polarity _pol = Polarity::EXC)
    : GenericNeuron(_netHost, _size, _pol, _minPos, _maxPos)
{
    spkScript = _spkScript;
    trainSizes = _trainSizes;
    indexPtrs = new uint32_t[_size];
}

InputNeuron::InputNeuron(const Network& _netHost,
                         const uint32_t _size,
                         const Position _minPos,
                         const Position _maxPos,
                         const std::string& filename,
                         const Polarity _pol = Polarity::EXC)
    : GenericNeuron(_netHost, _size, _pol, _minPos, _maxPos)
{
    std::ifstream spkIn (filename, std::ifstream::binary);
    spkScript = (uint32_t**)calloc(_size, sizeof(uint32_t*));
    trainSizes = new uint32_t[_size];
    indexPtrs = new uint32_t[_size];
    if(spkIn)
    {
        spkIn.seekg(0, spkIn.end);
        uint32_t len = spkIn.tellg();
        spkIn.seekg(0, spkIn.beg);
        uint32_t j = 0;
        std::vector<uint32_t> tempV;
        for(uint32_t i = 0; i < len/4; i++)
        {
            uint32_t k;
            spkIn >> k;
            if (k == UINT32_MAX) // next neuron flag
            {
                spkScript[j] = (uint32_t*)calloc(tempV.size(), sizeof(uint32_t));
                memcpy(spkScript[j], tempV.data(), tempV.size() * sizeof(uint32_t));
                trainSizes[j] = tempV.size();
                tempV.clear();
                j++;
            } else {
                tempV.push_back(k);
            }
        }
    }
    spkIn.close();
}