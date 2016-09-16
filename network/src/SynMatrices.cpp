#include <arrayfire.h>
#include <math.h>
#include <cstdint>
#include <vector>
#include "SynMatrices.h"

using namespace af;

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

SynMatrices::SynMatrices(	const AIFNeuron &src,
							const AIFNeuron &tar,
							uint32_t maxDly,
							uint32_t minDly ) 
	: srcHost(src), tarHost(tar), manager(_manager)
{
	uint32_t sSz = src.size;
	uint32_t tSz = tar.size;
	uint32_t dlyRange = maxDly - minDly;
	uint32_t** dlys = calcDelayMat(&src, &tar, maxDly);

	std::vector<std::vector<uint32_t>> srcs(maxDly-minDly, std::vector<uint32_t>);
	std::vector<std::vector<uint32_t>> tars(maxDly-minDly, std::vector<uint32_t>);
	for (uint32_t i = 0; i < sSz; i++) {
		for (uint32_t j = 0; j < tSz; j++) {
			if (dlys[i][j] !=0) {
				srcs[dlys[i][j] - minDly].push_back(i);
				tars[dlys[i][j] - minDly].push_back(j);
			}
		}
	}

	// TODO make sure this doesn't cause a memory leak...
	for (uint32_t d = 0; d < (maxDly-minDly); d++) {
		uint32_t szLoc = srcs[d].size();
		spWtMat.push_back(sparse(sSz, tSz, szLoc,
			(float*)calloc(szLoc, sizeof(float)),
			srcs[d].data(),
			tars[d].data(),
				f32, AF_STORAGE_CSC)); 
	}

}