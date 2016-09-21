#include <arrayfire.h>
#include <math.h>
#include <cstdint>
#include <vector>
#include "SynMatrices.h"

using namespace af;

SynMatrices::SynMatrices(	const AIFNeuron &src,
							const AIFNeuron &tar,
							const uint32_t _maxDly,
							const uint32_t _minDly ) 
	: srcHost(src), tarHost(tar), manager(_manager)
{
	maxDly = _maxDly;
	minDly = _minDly;
	uint32_t sSz = src.size;
	uint32_t tSz = tar.size;
	dlyRange = maxDly - minDly;
	dlyMat = calcDelayMat(&src, &tar, maxDly);

	std::vector<std::vector<uint32_t>> srcs(maxDly-minDly, std::vector<uint32_t>);
	std::vector<std::vector<uint32_t>> tars(maxDly-minDly, std::vector<uint32_t>);
	for (uint32_t i = 0; i < sSz; i++) {
		for (uint32_t j = 0; j < tSz; j++) {
			if (dlyMat[i][j] !=0) {
				srcs[dlyMat[i][j] - minDly].push_back(i);
				tars[dlyMat[i][j] - minDly].push_back(j);
			}
		}
	}

	for (uint32_t d = 0; d < (maxDly-minDly); d++) {
		uint32_t szLoc = srcs[d].size();
		spWtMat.push_back(
			sparse( sSz, tSz, szLoc,
			constant(START_WT, szLoc, f32),
			array(szLoc, srcs[d].data()),
			array(szLoc, tars[d].data()),
				f32, AF_STORAGE_CSR )	); 
		spdWMat.push_back(
			sparse( sSz, tSz, szLoc,
			constant(0, szLoc, f32),
			array(szLoc, srcs[d].data()),
			array(szLoc, tars[d].data()),
				f32, AF_STORAGE_CSR )	); 
		lastUp.push_back(
			sparse( sSz, tSz, szLoc,
			constant(0, szLoc, f32),
			array(szLoc, srcs[d].data()),
			array(szLoc, tars[d].data()),
				f32, AF_STORAGE_CSR )	); 
		lastArrT.push_back(
			sparse( sSz, tSz, szLoc,
			constant(0, szLoc, f32),
			array(szLoc, srcs[d].data()),
			array(szLoc, tars[d].data()),
				f32, AF_STORAGE_CSR )	); 
	}

	srcPol = src.polarity;
	tarPol = tar.polarity;

}

SynMatrices::uint32_t** calcDelayMat(AIFNeuron* src, AIFNeuron* tar, uint32_t maxDly) 
{
	float** dists = new float *[src->size];
	float MAX_DIST = 0;
	for (int i = 0; i < src->size; i++) {
		dists[i] = new float[tar->size];
		for (int j = 0; j < tar->size; j++) {
			float xdist = src->x[i] - tar->x[j];
			float ydist = src->y[i] - tar->y[j];
			float zdist = src->z[i] - tar->z[j];
			float dist = sqrt(xdist*xdist + ydist*ydist + zdist*zdist);
			dists[i][j] = dist;
			if (dist > MAX_DIST) {
				MAX_DIST = dist;
			}
		}
	}

	uint32_t** dlys = new uint32_t *[src->size];
	for (int i = 0; i < src->size; i++) {
		dlys[i] = new uint32_t [tar->size];
		for (int j = 0; j < tar->size; j++) {
			dlys[i][j] = (uint32_t) (maxDly*dists[i][j]/MAX_DIST);
		}
		delete [] dists[i];
	}
	delete [] dists;

	return dlys;
}

SynMatrices::void propagate(const uint32_t time, const float dt)
{

	gfor (seq,  dlyRange) {

	}

}
