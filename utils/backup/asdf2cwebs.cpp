#include "mex.h"
#include <iostream>
#include <cstdint>
#include <vector>
#include <cstring>

class SparseBitArray {

	public:
		uint64_t *bitArr;
		uint32_t *bitIndices;
		uint32_t fullSze;
		uint32_t sze;
		uint32_t disCnt_sze;
		uint32_t *pathData;
		uint32_t pathLength;
		SparseBitArray () {}
		SparseBitArray (const mxArray 	*spkTimes,
						double 			binSize,
						uint32_t 		max_bins,
						uint32_t 		*pData,
						uint32_t		pLength );
		SparseBitArray* tracePaths(	SparseBitArray compare,
								 	uint32_t delay,
								 	uint32_t *offsets,
								 	uint32_t noOffsets );

};

SparseBitArray::SparseBitArray (const mxArray 	*spkTimes,
								double 			binSize,
								uint32_t 		max_bins,
								uint32_t 		*pData,
								uint32_t		pLength )
{
		
	this->pathData = pData;
	this->pathLength = pLength;
	this->fullSze = max_bins/64 + (max_bins%64 != 0);
	//std::cout << "Made it 1" << '\n';
	uint32_t numSpks = mxGetN(spkTimes);
	double *spks = mxGetPr(spkTimes);
	std::cout << "Checking " << (spks != NULL) << " Num Spikes " << numSpks << '\n';
	
	if (numSpks == 0) {
		sze = 0;
		return;
	}

	std::cout << "Made it 2" << '\n';
	uint32_t *blockInds = new uint32_t[numSpks];
	if (blockInds == NULL) std::cout << "blockInds NULL" << '\n';
	uint8_t *bitLoc = new uint8_t[numSpks];
	if (bitLoc == NULL) std::cout << "bitLoc NULL" << '\n';

	std::cout << "Made it 3" << '\n';
	uint32_t t = (uint32_t) (spks[0] / binSize);
	blockInds[0] = t/64;
	bitLoc[0] = t%64;
	uint32_t unique = 1;
	std::vector<uint32_t> discontinuities;
	char cont = 0x0;
	uint32_t blookey = 0;
	discontinuities.push_back(0);
	for (uint32_t j = 1; j < numSpks; j++)
	{
		uint32_t t = (uint32_t) (spks[j] / binSize);
		blockInds[j] = t/64;
		bitLoc[j] = t%64;
		unique += blockInds[j] != blockInds[j-1] ? 1 : 0;
		if (blockInds[j] - blockInds[j-1] > 1) {
			discontinuities.push_back(j-1);
			discontinuities.push_back(j);
		}
	}
	discontinuities.push_back(numSpks);
	std::cout << "Made it 4" << '\n';

	this->sze = 1+unique+(discontinuities.size()/2);
	std::cout << "UNIQUE " << this->sze << '\n';

	this->bitArr = new uint64_t[this->sze]; 
	this->bitIndices = new uint32_t[discontinuities.size()];
	this->disCnt_sze = discontinuities.size();

	uint32_t k = 0;
	for(uint32_t i = 0; i < discontinuities.size(); i+=2) 
	{
		bitIndices[i] = blockInds[discontinuities[i]];
		bitIndices[i+1] = blockInds[discontinuities[i+1]];


		uint32_t index = 0;
		for(uint32_t j = discontinuities[i]; j <= discontinuities[i+1]; j++)
		{
			index = blockInds[j]+k;
			bitArr[index] |=  0x1ull << (63 - bitLoc[j]);

		}
		k++;
		if (index+1 > sze) {
			std::cout << index+1 << "\t\t" << sze << "\t\t" << i << "\t\t" << discontinuities.size() << '\n';
			break;
		}
		bitArr[index+1] = 0x0ull; // Add padding
	}
	discontinuities.clear();

/*
	uint32_t j = 0;
	for (uint32_t i = 0; i < unique; i++)
	{
		bitIndices[i] = blockInds[j]; // Retrieve first spike time in units of 64
		while (j < numSpks && bitIndices[i] == blockInds[j] )
		{
			bitArr[i] |= 0x1ull << (63 - bitLoc[j]);
			//std::cout << j << " ";
			++j;
		}
	}*/
	std::cout << '\n' << "Made it 5" << '\n';
	//delete[] bitLoc; 
	//delete[] blockInds;
	std::cout << "Made it 6" << '\n';
}

SparseBitArray * SparseBitArray::tracePaths(SparseBitArray 	compare,
								 			uint32_t	 	delay,
								 			uint32_t 		*offsets,
								 			uint32_t		noOffsets )
{
/*	if (delay > 63) {
		// ERROR
		return NULL;
	}
	for (int i = 0; i < noOffsets; i++) {
		if (delay + offsets[i] > 63) {
			//ERROR
			return NULL;
		}
	}*/

	SparseBitArray *forward = new SparseBitArray();
	std::vector<uint64_t> bitVec;
	std::vector<uint32_t> indicesVec;
	uint32_t j = 0;
	uint64_t *local_BA = new uint64_t[this->sze];
	std::memcpy(local_BA, this->bitArr, this->sze);




/*	for (uint32_t i = 0; i < this->sze; i++)
	{
		while(compare->bitIndices[j] < this->bitIndices[i] && j < compare->sze)
		{
			j++;
		}
		if (j >= compare->sze) break;
		if ()

	}*/
	return forward;
}

class BitRaster {
	public:
		SparseBitArray **raster;
		uint32_t rows;
		uint32_t cols;
		uint32_t num8Bytes;
		uint32_t avalanche;
		BitRaster(const mxArray *asdf) {
			rows = mxGetM(asdf);
			if (rows < 2) {
				//Error
			}
			double *binSize = mxGetPr(mxGetCell(asdf, rows-2));
			double *data = mxGetPr(mxGetCell(asdf, rows-1));
			std::cout << rows << '\n';
			std::cout << binSize[0] << '\n';
			std::cout << data[0] << " " << data[1] << '\n';

			cols = data[1];
			rows = rows - 2;
			raster = (SparseBitArray *) malloc(rows * sizeof(SparseBitArray*));
			uint32_t total_bytes = 0;
			for(uint32_t i = 0; i < rows; i++) {
				std::cout << i << '\n';
				uint32_t path = i;
				SparseBitArray *sba = new SparseBitArray(mxGetCell(asdf, i),
					binSize[0], cols, &path, 1);
				std::cout << "Neuron " << i << '\n';
				raster[i] = sba;
				total_bytes += (sba->sze * 8) + (sba->disCnt_sze*4) + 20;
				//std::cout << "Made it 8" << '\n';
			}
			for (int i = 0; i < raster[0].sze; i++) {
				std::cout << raster[0].bitArr[i] << "\t\t\t\t\t" << raster[0].bitIndices[i] << '\n'; 
			}
			std::cout << "Total size of ASDF file: " << sizeof(asdf) << " bytes" << '\n'
					  << "Total size of BitRaster(TM): " << total_bytes << " bytes" << '\n';
		}

/*		uint32_t* compareBitArrays( BitRaster *rast,
								  uint32_t a, uint32_t b,
								  uint32_t noOffsets,
								  uint32_t *offsets) {
			uint64_t *focus = rast->raster[a];
			uint64_t *intersects = (uint64_t *)calloc(sizeof(rast->raster[b]));
			uint64_t *comp = (uint64_t *)calloc(sizeof(rast->raster[b]));
			std::memcpy(comp, rast->raster[b], sizeof(rast->raster[b]));
			for (uint32_t i = 0; i < noOffsets; i++) {
				#pragma unroll
				for (uint32_t j = 0; j < rast->num8Bytes-1; j++) {
					intersects[j] = intersects[j] | // Accumulate over all offsets
						(focus[j] // compare to focus
							& (comp[j] << offsets[i] // offset current bits
								| comp[j+1] >> 64-offsets[i])); // combine with shifted bits from next 64-int
				}
				intersects[rast->num8Bytes-1] |= comp[rast->num8Bytes-1] << offsets[i]; //clean up last entry
			}

			for (uint32_t i = 0; j < rast->num8Bytes; j++) {

			}

		}*/


};



/* The gateway function */
void mexFunction(int nlhs, mxArray *plhs[],
                 int nrhs, const mxArray *prhs[])
{


	BitRaster *bob = new BitRaster(prhs[0]);

}