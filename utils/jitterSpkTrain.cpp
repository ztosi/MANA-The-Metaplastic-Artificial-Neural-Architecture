#include "mex.h"
#include <cstdint>
#include <random>
#include <vector>
#include <cstring>
#include <iostream>

template <class T>
void jitterSwap(T *data, int numel, uint32_t sd)
{
	std::vector<uint32_t> nzInds;
	for (uint32_t i = 0; i < numel; i++)
	{
		if (data[i] != 0)
		{
			nzInds.push_back(i);

		}
	}
	int *jitters = (int*) malloc(nzInds.size() * sizeof(int));
	std::default_random_engine rgen;
	std::normal_distribution<float> randn(0, sd);
	for (uint32_t i = 0; i < nzInds.size(); i++)
	{
		jitters[i] = (int) randn(rgen);
	}
	for (uint32_t i = 0; i < nzInds.size(); i++)
	{
		int swapInd = nzInds[i] + jitters[i];
		if (swapInd < 0) {
			swapInd = 0;
		} else if (swapInd >= numel) {
			swapInd = numel - 1;
		}
		T holder = data[nzInds[i]];
		data[nzInds[i]] = data[swapInd];
		data[swapInd] = holder;
	}
	free(jitters);
	jitters = NULL;
}

void mexFunction(int nlhs, mxArray *plhs[],
                 int nrhs, const mxArray *prhs[])
{
	const mxArray *spkTrain = prhs[0];
	uint32_t sd = (uint32_t) mxGetScalar(prhs[1]);
	int numel = (int) mxGetNumberOfElements(spkTrain);

	switch(mxGetClassID(spkTrain))
	{
		case mxINT8_CLASS : {	
								plhs[0] = mxCreateNumericArray(1, &numel, mxINT8_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(int8_t));
								jitterSwap<int8_t>((int8_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxUINT8_CLASS : {
								plhs[0] = mxCreateNumericArray(1, &numel, mxUINT8_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(uint8_t));
								jitterSwap<uint8_t>((uint8_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxINT16_CLASS : {	
								plhs[0] = mxCreateNumericArray(1, &numel, mxINT16_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(int16_t));
								jitterSwap<int16_t>((int16_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxUINT16_CLASS : {
								plhs[0] = mxCreateNumericArray(1, &numel, mxUINT16_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(uint16_t));
								jitterSwap<uint16_t>((uint16_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxINT32_CLASS : {	
								plhs[0] = mxCreateNumericArray(1, &numel, mxINT32_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(int32_t));
								jitterSwap<int32_t>((int32_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxUINT32_CLASS : {
								plhs[0] = mxCreateNumericArray(1, &numel, mxUINT32_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(uint32_t));
								jitterSwap<uint32_t>((uint32_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxINT64_CLASS : {	
								plhs[0] = mxCreateNumericArray(1, &numel, mxINT64_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(int64_t));
								jitterSwap<int64_t>((int64_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxUINT64_CLASS : {
								plhs[0] = mxCreateNumericArray(1, &numel, mxUINT64_CLASS, mxREAL);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(uint64_t));
								jitterSwap<uint64_t>((uint64_t*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		case mxLOGICAL_CLASS : {
								plhs[0] = mxCreateLogicalArray(1, &numel);
								std::memcpy(mxGetData(plhs[0]), mxGetData(spkTrain), numel * sizeof(char));
								jitterSwap<char>((char*)mxGetData(plhs[0]), numel, sd);
								break;
							}
		default : break;				
	}

}