#include "mex.h"
#include <iostream>
#include <cstdint>

class SparseBitArray {

	public:
		uint64_t *bitArr;
		uint32_t *bitIndices;
		uint32_t fullSze;
		uint32_t sze;
		uint32_t *pathData;
		uint32_t pathLength;
		SparseBitArray (const mxArray 	*spkTimes,
						double 			binSize,
						uint32_t 		max_bins,
						uint32_t 		*pData,
						uint32_t		pLength );

};

SparseBitArray::SparseBitArray (
	const mxArray 	*spkTimes,
	double 			binSize,
	uint32_t 		max_bins,
	uint32_t 		*pData,
	uint32_t		pLength )
{
		
	pathData = pData;
	pLength = pLength;
	fullSze = max_bins/64 + (max_bins%64 != 0);

	uint32_t numSpks = mxGetM(spkTimes);
	double *spks = mxGetPr(spkTimes);
	
	uint32_t *blockInds = (uint32_t *) calloc(numSpks, sizeof(uint32_t));
	uint8_t *bitLoc = (uint8_t *) calloc(numSpks, sizeof(uint8_t));

	uint32_t t = (uint32_t) (spks[0] / binSize);
	blockInds[0] = t/64;
	bitLoc[0] = t%64;
	uint32_t unique = 1;
	for (uint32_t j = 1; j < numSpks; j++)
	{
		uint32_t t = (uint32_t) (spks[j] / binSize);
		blockInds[j] = t/64;
		bitLoc[j] = t%64;
		unique += blockInds[j] != blockInds[j-1];
	}

	sze = unique;

	bitArr = (uint64_t *) calloc(unique, sizeof(uint64_t));
	bitIndices = (uint32_t *) calloc(unique, sizeof(uint32_t));

	uint32_t j = 0;
	for (uint32_t i = 0; i < unique; i++)
	{
		bitIndices[i] = blockInds[j]; // Retrieve first spike time in units of 64
		while ( bitIndices[i] == blockInds[j] )
		{
			bitArr[i] |= 0x1ull << (63 - bitLoc[j]);
			j++;
		}
	}
	free(bitLoc);
	free(blockInds);
	free(spks);
}

class BitRaster {
	public:
		SparseBitArray *raster;
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
			cols = data[1];
			rows = rows - 2;
			raster = (SparseBitArray*) calloc(rows, sizeof(SparseBitArray*));
			for(uint32_t i = 0; i < rows; i++) {
				std::cout << i << '\n';
				double *spks = mxGetPr(mxGetCell(asdf, i));
				uint32_t path = i;
				SparseBitArray sba(mxGetCell(asdf, i),
					binSize[0], cols, &path, 1);
				raster[i] = sba;
			}
			std::cout << "Total size of ASDF file: " << sizeof asdf << " bytes" << '\n'
					  << "Total size of BitRaster(TM): " << sizeof raster << " bytes" << '\n';
			free(data);
			free(binSize);
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


	BitRaster bob(prhs[0]);

}