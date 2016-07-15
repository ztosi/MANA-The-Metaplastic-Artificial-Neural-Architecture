#ifndef ASDF2AVLETS_H_
#define ASDF2AVLETS_H_

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
		~SparseBitArray();
		void SparseBitArray::findHeads (	SparseBitArray 	**incoming,
											SparseBitArray 	*tar,
											uint32_t		inDeg);
		void eliminateOverlap(SparseBitArray *inc);
		void reduce();
		static SparseBitArray compare(	SparseBitArray *src,
										SparseBitArray *tar, 
										uint32_t 		delay,
										uint32_t 		nRanges,
										int				rangeStart,
										int 			rangeEnd);
		static SparseBitArray** SparseBitArray::sweepIncoming (	
													BitRaster 		*home,
													SparseBitArray 	*tar, 
													uint32_t 		**delays,
 													uint32_t 		inDeg,
 													uint32_t 		nRanges,
 													int 			rangeStart,
 													int 			rangeEnd);
};

class BitRaster {
	
	public:
		
		SparseBitArray **raster;
		uint32_t rows;
		uint32_t cols;
		uint32_t num8Bytes;
		uint32_t avalanche;
		
		BitRaster(const mxArray *asdf);
		~BitRaster();
		void verifyRaster(	BitRaster 		*r,
							const mxArray 	*asdf); 


};

class CausalTreeDictionary {

	public:
	
		uint32_t headID;
		uint32_t sze;
		uint32_t occupancy;
		SparseBitArray **descendents;
		CausalTreeDictionary *descDictLocs;

		CausalTreeDictionary();
		~CausalTreeDictionary();
		CausalTreeDictionary(uint32_t id, uint32_t sze);
		static CausalTreeDictionary* buildDictionary();

};

class CausalTreeNode {

	public:

		SparseBitArray *node;
		CausalTreeDictionary *rootHome;
		std::deque<CausalTreeNode> leaves;

		CausalTreeNode(SparseBitArray *rt);
		~CausalTreeNode();
		void search(CausalTreeDictionary** dictionary);

};



#endif // ASDF2AVLETS_H_