#include <arrayfire.h>
#include <cstdint>
#include "AIFNeuron.h"
#include "SynMatrices.h"
#include "HPComponent.h"
#include "IPComponent.h"
#include "SynActors.h"
#include "UDFPlasticity.h"
#include "Network.h"

SOModule::SOModule(	const Network &_host,
					const uint32_t size,
					const Position minPos,
					const Position maxPos	)
	: host(_host),
	excNeuGrp(_host, (uint32_t)(size * (1-DEF_IE_RATIO)), (bool)1,
		minPos, maxPos),
	inhNeuGrp(_host, (uint32_t)(size * DEF_IE_RATIO), (bool)0,
		minPos, maxPos)
{
	synGrps = NULL;
	hpComp = NULL;
	ipComp = NULL;
	UDFComp = NULL;
	stdp = NULL;
	synScale = NULL;
	norman = NULL;
}

SOModule::SOModule(	const Network &_host,
					const uint32_t size,
					const Position minPos,
					const Position maxPos,
					const float ieRatio	)
	: host(_host),
	excNeuGrp(_host, (uint32_t)(size * (1-ieRatio)), (bool)1,
		minPos, maxPos),
	inhNeuGrp(_host, (uint32_t)(size * ieRatio), (bool)0,
		minPos, maxPos)
{
	synGrps = NULL;
	hpComp = NULL;
	ipComp = NULL;
	UDFComp = NULL;
	stdp = NULL;
	synScale = NULL;
	norman = NULL;
}

SOModule* SOModule::buildSOModule(	const Network &_host,
									const uint32_t size,
									const Position minPos,
									const Position maxPos	)
{
	return SOModule::buildSOModule(_host, size, minPos, maxPos, DEF_IE_RATIO);
}

SOModule* SOModule::buildSOModule(	const Network &_host,
									const uint32_t size,
									const Position minPos,
									const Position maxPos,
									const float ieRatio )
{
	SOModule* module = new SOModule(_host, size, minPos, maxPos, ieRatio);
	

	return module;
}

