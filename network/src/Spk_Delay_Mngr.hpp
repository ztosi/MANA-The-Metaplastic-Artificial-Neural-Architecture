#include <arrayfire.h>
#include "AIFNeuron.h"
#include <vector>
#include <cstdint>

#ifndef SPK_DELAY_MNGR_H_
#define SPK_DELAY_MNGR_H_

class Spk_Delay_MNGR {

	public:

		AIFNeuron* neu_host;
		SynMatrices* syn_host;

		std::vector<af::array> cycSpkArr;
	
	Spk_Delay_MNGR(AIFNeuron &host, uint32_t MAX_DLY);
	~Spk_Delay_MNGR();
	af::array getDelayedSpikes(uint32_t del) {
		int nd = presentIndex-del;
		if (nd < 0) {
			return cycSpkArr[cycSpkArr.size() + nd];
		} else {
			return cycSpkArr[nd];
		}
	}
	void increment();

	private:
		uint32_t presentIndex;

	void insertCurrentSpks(af::array &spks)
	{
		cycSpkArr[presentIndex] = spks;
		presentIndex++;
	} 

};

#endif // SPK_DELAY_MNGR_H_