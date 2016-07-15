#include "mex.h"
#include <iostream>
#include <cstdint>
#include <vector>
#include <cstring>
#include "asdf2avlets.h"

/**
    asdf2avlets.cpp
    Purpose:Converts an asdf (another spike data format) file, which contains spike trains
    into a bit array for the purpose of optimized detection of causal paths. This is
    currently set up to work as a Matlab MEX file taking in an asdf file and a matrix
    of synaptic transmission delays between neurons. Causal paths can be put together
    to find Causal Webs (credit to Rashid Williams-Garcia).

    @author Zach Tosi
    @version 0.1 6/28/2016 
*/

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

	uint32_t numSpks = mxGetN(spkTimes);
	double *spks = mxGetPr(spkTimes);

	std::vector<uint64_t> bitLoc;
	std::vector<uint32_t> blockInds;
	uint32_t t = (uint32_t) (spks[0] / binSize);
	blockInds.push_back(t/64);
	bitLoc.push_back(0x1ull << (63 - t%64));
	int k = 0;
	for (uint32_t j = 1; j < numSpks; j++)
	{
		t = (uint32_t) (spks[j] / binSize);
		uint32_t blk64 = t/64;
		uint32_t mod64 = t%64;
		if (blk64 != blockInds[k]) {
			blockInds.push_back(blk64);
			bitLoc.push_back(0x0ull);
			k++;
		}
		bitLoc[k] |= 0x1ull << (63 - mod64);
	}

	sze = bitLoc.size();
	bitArr = (uint64_t*)calloc(bitLoc.size(), sizeof(uint64_t));
	bitIndices = (uint32_t*)calloc(blockInds.size(), sizeof(uint32_t));

	for (uint32_t i = 0; i < sze; i++) {
		bitArr[i] = bitLoc[i];
		bitIndices[i] = blockInds[i];
	}
}



BitRaster::BitRaster(const mxArray *asdf) 
{
	//std::cout << "BEGIN!" << '\n';
	rows = mxGetM(asdf);
	if (rows < 3) {
		throw 1;
	}
	double *binSize = mxGetPr(mxGetCell(asdf, rows-2));
	double *data = mxGetPr(mxGetCell(asdf, rows-1));
	//std::cout << "Bin size: " << binSize[0] << '\n';
	cols = data[1];
	rows = rows - 2;
	//std::cout << "Allocating raster..." << '\n';
	raster = (SparseBitArray**) calloc(rows, sizeof(SparseBitArray*));
	uint32_t bytes = 0;
	for(uint32_t i = 0; i < rows; i++) {
		uint32_t path = i;
		raster[i] = new SparseBitArray(mxGetCell(asdf, i), binSize[0], cols, &path, 1);
		bytes += (raster[i]->sze*8)+(raster[i]->sze*4);
	}
}

void Bitraster::verifyRaster(	BitRaster 		*r,
								const mxArray 	*asdf)
{

	double *ts = mxGetPr(mxGetCell(asdf, mxGetM(asdf)-2));

	double timeStep = ts[0];

	std::vector<double> *times = new std::vector<double>;
	std::cout << "Verifying Raster..." << '\n';
	std::cout << "Time step: " << timeStep << '\n';
	std::cout << r->rows << '\n';
	for (int i = 0; i < r->rows; i++) 
	{
		SparseBitArray *sba = r->raster[i];
		std::cout << "SBA size: " << sba->sze << '\n';
		for (int j = 0; j < sba->sze; j++) 
		{
			uint64_t block = sba->bitIndices[j];
			uint64_t bits = sba->bitArr[j];
			int k = 63;
			while (bits != 0) 
			{
				if (k<0) {
					std::cout<< "Problem" << '\n';
					for (int w = 0; w < 64; w++) {
						uint64_t blergle = (bits<<w)>>(63);
						std::cout << blergle;
					}
					return;
				}
				uint64_t temp = bits>>k;
				if (temp) 
				{
					(*times).push_back(timeStep*((block * 64) + 63 - k));
					bits = (bits << (64-k)) >> (64-k);
					if (k==0) {
						break;
					}
				}
				k--;
			}
		}
		mxArray *asdfTimesMx = mxGetCell(asdf, i);
		uint32_t spks = mxGetN(asdfTimesMx); 
		double *asdfTimes = mxGetPr(asdfTimesMx);
		int bcheck = 0;
		//std::cout << "Made it..." << '\n';
		std::cout << spks << '\n';
		if (spks != (*times).size())
		{
			std::cout << "Bit array " << i << " contains incorrect number of spikes.  ASDF: "
			 << spks << " " << asdfTimes[spks-1] << '\t' << "Bits: " << (*times).size() << " " << (*times).back() << '\n';
			 bcheck = 1;
		}
		int clear = 1;
		for (int j = 0; j < spks; j++)
		{
			if (bcheck) {
				clear = 0;
				break;
			}
			if ((*times)[j] != asdfTimes[j])
			{
				clear = 0;
				std::cout << "Inconsistency: " <<'\t' << i << " ASDF: " << asdfTimes[j] << '\t' << (*times)[j] << '\n';
			}
		}
		if (clear) {
			std::cout << "Neuron " << i << " VERIFIED!" << '\n';
		}
		(*times).clear();
	}
	(*times).clear();
	(*times).shrink_to_fit();
	delete times;
}

SparseBitArray* SparseBitArray::compare(SparseBitArray *src,
										SparseBitArray *tar, 
										uint32_t 		delay,
										uint32_t 		nRanges,
										int				rangeStart,
										int 			rangeEnd)
{

	std::vector<uint64_t> shiftSrc ((int) (src->sze * 1.1));
	std::vector<uint32_t> shiftSrcInd ((int) (src->sze * 1.1));

	//TODO: This is dumb, replace it with a temporary...
	uint64_t *srcBaCpy = (uint64_t *) malloc(src->sze * sizeof(uint64_t));
	uint32_t *srcBICpy = (uint32_t *) malloc(src->sze * sizeof(uint32_t));
	for (int i = 0; i < src->sze; i++) {
		srcBaCpy[i] = src->bitArr[i];
	}
	for (int i = 0; i < src->sze; i++) {
		srcBICpy[i] = src->bitIndices[i];
	}


	uint64_t carryOver = 0x0ull;
	uint32_t i = 0;
	uint32_t j = 0;
	while (j < tar->sze && i < src->sze) {
		while (srcBICpys[i] != tar->bitIndices[j]) {
			if (srcBICpy[i] < tar->bitIndices[j]) {

				i++;
			} else {
				shiftSrc.push_back(0x0ull);
				shiftSrcInd.push_back(tar->bitIndices[j]);
				j++;
			}
		}
		if (j >= tar->sze || i >= src->sze) break;


		// "Smear" the current bit storage and shift according to
		// delay, then combine with carried over bits from the previous
		// bit block. 
		uint64_t current = srcBaCpy[i];
		current = current >> delay+rangeStart;
		for (uint32_t j = 1; j < nRanges; j++)
		{
			current = current | (current >> j);
		}
		current |= carryOver;
		uint32_t currInd = srcBICpy[i];
		shiftSrc.push_back(current);
		shiftSrcInd.push_back(currInd);	
		carryOver = 0x0ull;


		// If we're not at the end...
		// If the target's next index is the same as the 
		// source's current index + 1
		// Calculate the carry over
		// and replace the source copy's current
		// index with the next index and the bit block
		// with zeros, so that carry over will have somewhere to go
		if (j + 1 < tar->sze && i + 1 < src->sze) {
			if (tar->bitIndices[j+1] == currInd+1) {
				uint64_t mask = ~((~0x0ull) << delay+range[nRanges-1]); // 1's over part to be carried over
				for (uint32_t k = 0; k < nRanges; k++)
				{
					carryOver |= (srcBaCpy[i] & mask) << j;
					mask = mask >> 1;
				}
				carryOver = carryOver << 63-delay+rangeEnd;
				if (currInd + 1 != srcBICpy[i+1])
				{
					srcBICpy[i] = currInd + 1;
					srcBaCpy[i] = 0x0ull;
				}
			}
		}
	}
	free(srcBICpy);
	free(srcBaCpy);
	srcBICpy = NULL;
	srcBaCpy = NULL;

	if (shiftSrc.size() != tar -> sze) 
	{
		std::cout << "Problem: Shifted source not the same size as target." << '\n';
		return NULL; 
	}

	for (i= 0; i < shiftSrc.size(); i++) {
		shiftSrc[i] = shiftSrc[i] & tar->bitArr[i];
	}
	uint64_t nonzeros = 0;
	for (i= 0; i < shiftSrc.size(); i++) {
		nonzeros += shiftSrc[i] != 0 ? 1 : 0;
	}

	if (nonzeros == 0) {
		// No causal connections found from src->tar, either they weren'r
		// ever connected, or the path represented by src has terminated for
		// all tar.
		return NULL;
	}

	SparseBitArray *ret = new SparseBitArray;
	uint64_t *ba = (uint64_t *) malloc(nonzeros * sizeof(uint64_t));
	uint32_t *bi = (uint64_t *) malloc(nonzeros * sizeof(uint32_t));
	j = 0;
	for (i= 0; i < nonzeros; i++) {
		while (j < shiftSrc.size() && shiftSrc[j] == 0) {
			j++;
		}
		if (j < shiftSrc.size()) break;
		ba[i] = shiftSrc[j];
		bi[i] = shiftSrcInd[j];
		j++;
	}

	ret->bitArr = ba;
	ret->bitIndices = bi;
	if (src->pathLength == 1){
		ret->pathLength = 1 + tar->pathLength;
		ret->pathData = (uint32_t *) malloc(ret->pathLength * sizeof(uint32_t));
		ret->pathData[0] = src->pathData[0];
		for (i = 0; i < tar->pathLength; i++) {
			ret->pathData[i+1] = tar->pathData[i];
		}
	} else {
		ret->pathLength = src->pathLength + tar->pathLength - 1;
		ret->pathData = (uint32_t *) malloc(ret->pathLength * sizeof(uint32_t));
		for (i = 0; i < src->pathLength; i++) {
			ret->pathData[i] = src->pathData[i];
		}
		for (i = 1; i < tar->pathLength; i++) {
			ret->pathData[i+src->pathLength-1] = tar->pathData[i];
		}
	}
	return ret;
}

SparseBitArray** SparseBitArray::sweepIncoming (	BitRaster 		*home,
													SparseBitArray 	*tar, 
													uint32_t 		**delays,
 													uint32_t 		inDeg,
 													uint32_t 		nRanges,
 													int 			rangeStart,
 													int 			rangeEnd)
{
	SparseBitArray** incoming = (SparseBitArray*) malloc(inDeg
		* sizeof(SparseBitArray*));
	uint32_t k = 0;
	uint32_t j = tar->pathData[tar->pathLength-1];
	for (uint32_t i = 0; i < home->rows; i++) {
		if (delays[i][j] ~= 0)
		{
			incoming[k++] = SparseBitArray.compare(home->raster[i], tar,
				delays[i][j], nRanges, rangeStart, rangeEnd);
		}
	}
	return incoming;
}

void SparseBitArray::findHeads (	SparseBitArray 	**incoming,
									SparseBitArray 	*tar,
									uint32_t		inDeg)
{
	for (uint32_t i = 0; i < inDeg; i++)
	{
		tar.eliminateOverlap(incoming[i]);
	}
	tar.reduce();
}

void SparseBitArray::reduce()
{
	std::vector<uint64_t> bits;
	std::vector<uint32_t> indices;

	for(uint32_t i = 0; i < this->sze; i++)
	{
		if (this->bitArr[i] != 0)
		{
			bits.push_back(this->bitArr[i]);
			indices.push_back(this->bitIndices[i]);
		}
	}
	uint32_t bytes = bits.size() * sizeof(uint64_t);
	if (void *tempBa = realloc(this->bitArr, bytes)
		&& void tempBi = realloc(this->bitIndices, bytes))
	{
		this->bitArr = static_cast<uint64_t*>(tempBa);
		this->bitIndices = static_cast<uint32_t*>(tempBi);
		memcpy(this->bitArr, bits.data(), bytes);
		memcpy(this->bitIndices, indices.data(), bytes);
	} 
	else 
	{
		std::bad_alloc();
	}
	this->sze = bits.size();
}

void SparseBitArray::eliminateOverlap(SparseBitArray *inc)
{
	uint32_t i = 0;
	uint32_t j = 0;
	while (i < this->sze && j < inc->sze)
	{
		if(this->bitIndices[i] < inc->bitIndices[j])
		{
			i++;
			continue;
		}
		if(this->bitIndices[i] > inc->bitIndices[j])
		{
			j++;
			continue;
		}
		this->bitArr[i] = this->bitArr[i] ^ inc->bitArr[j];
		i++;
		j++;
	}
}

CausalTreeDictionary* CausalTreeDictionary::buildDictionary(
													BitRaster 	*bRast, 
													uint32_t 	**delays,
													uint32_t	*inDegrees,
													uint32_t	*outDegrees)
{

	uint32_t num_neu = bRast->rows;

	inDegrees = (uint32_t*) malloc(num_neu* sizeof(uint32_t));
	outDegrees = (uint32_t*) malloc(num_neu * sizeof(uint32_t));

	for(int i = 0; i < num_neu ; i++)
	{
		for (int j = 0; j < num_neu ; j++)
		{
			if (delays[i][j] != 0)
			{
				inDegrees[j]++;
				outDegrees[i]++;
			}
		}
	}

	SparseBitArray*** holder = (SparseBitArray***) malloc(
		num_neu * sizeof(SparseBitArray**));

	

}

/* The gateway function */
void mexFunction(int nlhs, mxArray *plhs[],
                 int nrhs, const mxArray *prhs[])
{
	try {
		BitRaster bob(prhs[0]);
		bob.verifyRaster(&bob, prhs[0]);


	} catch (int err) {
		if (err == 1) {
			std::cout << "Bit raster construction FAILED."<< '\n' 
				<<"Asdf file must have at least 1 spike train"
				<< " and/or dimensions must be nx1." << '\n';
		} else {
			std::cout << "An unknown error has been thrown and caught."
				<< '\n';
		}
		return;
	}
}