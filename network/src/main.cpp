#include <arrayfire.h>
#include <vector>
#include <cstdint>
#include "AIFNeuron.h"
#include "FREsitmator.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "STDP.h"
#include "SynMatrices.h"
#include "UDF.h"

void buildNetwork() 
{
	Network net  = new Network();
	AIFNeuron* resEx = new AIFNeuron(net, 4000,
		(uint8_t) 1, 200, 400, 200, 400, 200, 600);
	AIFNeuron* resInh = new AIFNeuron(net, 1000,
		(uint8_t) 0, 250, 350, 250, 350, 250, 550);
	SynMatrices* rEE = SynMatrices::connectNeurons(
		*resEx, *resEx, )


}


int main () {



}