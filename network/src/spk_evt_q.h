#include <#arrayfire.h>


#ifndef SPK_EVT_Q_H_
#define SPK_EVT_Q_H_

	typedef struct {

		// Pointers to pointers, must be taken from
		// synapse objects and replace the values therein
		float ** w;
		float ** dw;
		float ** u;
		double ** lastSpkTime;

		// Constants used for computation
		int * postIndex;
		float * U;
		float * D;
		float * F;
		float * R;

	} SpikeEvents;

	typedef struct {

		unsigned int index;
		unsigned int maxDelay;
		SpikeEvents * eventElements;

	} SpikeEventQ;

extern SpikeEventQ * constructSpikeEventQ(unsigned int maxDelay);

extern void incrementIndex(SpikeEventQ * sevq);

extern SpikeEvents * getEventsForTime(unsigned int noTimeStepsInFuture);

extern void addEvent(SpkeEvents * evts);

#endif // SPK_EVT_Q_H_