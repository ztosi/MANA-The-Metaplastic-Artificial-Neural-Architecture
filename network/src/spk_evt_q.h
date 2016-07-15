#include <vector>


#ifndef SPK_EVT_Q_H_
#define SPK_EVT_Q_H_

class SpikeEvents {

	private:
		// Pointers to pointers, must be taken from
		// synapse objects and replace the values therein
		std::vector<float*> **w;
		std::vector<float*> **dw;
		std::vector<float*> **u;
		std::vector<float*> **R;
		std::vector<uint32_t*> **lastArr;
		std::vector<uint32_t*> **lu;
		
		std::vector<uint32_t> **lastSpkTime;
		std::vector<float> **U;
		std::vector<float> **D;
		std::vector<float> **F;

		uint32_t index;
		uint32_t maxSize;

	public:
		SpikeEvents(uint32_t maxDelay);
		~SpikeEvents();
		// For a given queue of spike events takes all the events
		// arriving at the current time and processes them
		// this includes:
		// -Updating the synapse weight based on time of last update
		// 		and dwdt calculated at that time
		// -Updating UDF plasticity
		// -Updates the synapses last update time
		// -Updates the spike arrival time
		// -Calculates the post synaptic response and adds it to
		//		the appropriate current term
		// NOTE: Automatically increments the index
		void SpikeEvents::propagate(SpikeEvents 	*spkEvt,
									Network 		*root,
									bool 			excSrc,
									bool 			excTar); 
		uint32_t incrementIndex();
		uint32_t getIndex4Future(uint32_t delay);
		void addEvent(	float		*w_,
						float		*dw_,
						float		*lu_,
						float		*u_,
						float		*R,
						uint32_t	*lastArr,
						uint32_t 	lastSpkTime,
						float 		U,
						float 		D,
						float 		F);
		void clear(uint32_t index);
		float** getW();
		float** getDwDt();
		float** get_u();
		float** getR();
		uint32_t** getLastArr();
		uint32_t** getLu();
		uint32_t* getLastSpkTime();
		float* getU();
		float* getD();
		float* getF();

}

#endif // SPK_EVT_Q_H_