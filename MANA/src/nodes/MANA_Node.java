package nodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import data_holders.InputData;
import data_holders.MANANeurons;
import data_holders.Spiker;
import data_holders.SrcTarPair;
import data_holders.SynapseData;
import data_holders.SynapseData.SynType;
import functions.NeuronFunctions;
import functions.SynapseFunctions;

public class MANA_Node {
	
	/** The sector this node belongs to/is managed by. Must have the same target neurons. */
	public MANA_Sector parent_sector;
	
	/** Index among other nodes in the sector. */
	public int sector_index;
	
	/** 
	 * Event (AP) source and the neurons (targets of APs/Synapses)
	 *  whose afferent synapses we opperate on
	 */
	public final Spiker srcData;
	
	/** The neurons whose inputs from the specific srcData this node handles,
	 * most opperations are done on a by-target-node basis making them the primary
	 * data this node works on and its primary basis of organization
	 *  (perhaps alongside srcData, but srcData despite co-defining the node does
	 *  not exert as much influence on its organization).
	 */
	public final MANANeurons targData;
	
	/** 
	 * True if and only if the neurons that are the source of
	 *  the synapses covered by this node are not experimenter-driven
	 * external input nodes (and therefore provide no contribution to
	 *  meta-homeostatic plastcity)
	 */
	public final boolean inputIsExternal;
	
	/** 
	 * Can be used to selectively disable the contributions of the
	 * source neurons for this node to the Meta-homeostatic plasticity
	 * calculations for the target neurons.
	 */
	public boolean useMHP=true;
	
	/** True if and only if the synapses in this node are "trans-unit" meaning
	 * they connect groups of neurons belonging to distinct MANA units. Biologically
	 * these would be roughly equivalent to non-local white-matter synapses which
	 * connect distant cortical columns.
	 */
	public final boolean isTransUnit;
	
	/** Width-> number of target neurons; always same as sector width */
	public final int width;
	/** 
	 * Height -> number of source neurons, can be different from width
	 *  and even other nodes in the same sector
	 */
	public final int height;
	
	/**
	 *  The "type" of node this is in terms of its synapses,
	 *   does it connect excitatory neurons to excitatory 
	 *   neurons or inhibitory neurons to excitatory neurons,
	 *   and so on.
	 */
	public final SynType type;
	
	/** The sum of all synaptic weights impinging on each target in this node (locally).*/
	public double [] localSums;
	
	/**
	 * For each source neuron index (key) provides a 2D array where the
	 * first row is the indices of the targets in this node that the
	 * source key connects to and the second row is the index of the 
	 * connection represented by the key row pair in tarSrcMap
	 */
	HashMap<Integer, int[][]> srcTarMap;
	
	/**
	 * Rows" are targets, "cols" are indices of source neurons
	 * that impinge on those targets in the same order as all other
	 * data structures arranged the same way see: weights, dws,
	 * synapses, tarDlyMap, lastArrs. For a given value at a given
	 * pair of indices in those structures this gives the index of the
	 * source neuron corresponding to those values.
	 */
	public int [][] tarSrcMap;
	
	
	public double [][] tarDlyMap;
	public SynapseData [][] synapses;
	public double [][] weights;
	public double [][] dws;
	public double [][] lastArrs;
	public double[] localPFRPot;
	public double[] localPFRDep;
	
	public ArrayList<PriorityQueue<Event>> eventQ; 
	public double[] evtCurrents;
	public int[] evtInds;
	private int ptr=0;

	
	/**
	 * 
	 * @param src
	 * @param targ
	 * @param _type
	 * @param tarDlys
	 */
	public MANA_Node(final Spiker src, final MANANeurons targ,
			final SynType _type,
			final double [][] tarDlys,
			final boolean _transUnit) 
	{
		this.srcData = src;
		this.targData = targ;
		this.type = _type;
		inputIsExternal = src instanceof InputData;
		this.isTransUnit = _transUnit;
		
		int noTargs = targ.getSize();
		int noSrcs = src.getSize();
		
		width = noTargs;
		height = noSrcs;
		
		tarDlyMap = new double[noTargs][];
		// Default is initialized to all to all
		synapses = new SynapseData[noTargs][noSrcs];
		eventQ = new ArrayList<PriorityQueue<Event>>();
		for(int ii=0; ii < noTargs; ++ii) {
			eventQ.add(new PriorityQueue<Event>(Event.evtComp));
			tarDlyMap[ii] = new double[tarDlys[ii].length];
			System.arraycopy(tarDlys[ii], 0,
					tarDlyMap[ii], 0, tarDlys[ii].length);
			weights[ii] = new double[noSrcs];
		//	lastUps[ii] = new double[noSrcs];
			lastArrs[ii] = new double[noSrcs];
			dws[ii] = new double[noSrcs];
			synapses[ii] = new SynapseData[noSrcs];
			// TODO finish constructor
		}
		
		
		srcTarMap = new HashMap<Integer, int[][]>();
		for(int ii=0; ii < noSrcs; ++ii) {
			srcTarMap.put(ii, new int[2][noTargs]);
			
		}
		
	}
	
//	public void reform(final Map<Integer, Set<Integer>> toRemove, final Map<Integer, Set<Integer>> toAdd) {
//		for(int ii=0; ii<width; ++ii) {
//			int newSize;
//			int [] newSrc
//			if(toRemove.containsKey(ii)) {
//				if(toAdd.containsKey(ii)) {
//					toRemove.get(ii).removeAll(toAdd.get(ii));
//					newSize = weights[ii].length + toAdd.get(ii).size() - toRemove.get(ii).size();
//				} else {
//					newSize = weights[ii].length - toRemove.get(ii).size();
//				}
//			} else {
//				if(toAdd.containsKey(ii)) {
//					newSize = weights[ii].length + toAdd.get(ii).size();
//					
//				} else {
//					continue; // Nothing to add Or Remove
//				}
//			}
//			
//		}
//		
//}
//	
//	public void reform(List<SrcTarPair> toRemove, List<SrcTarPair> toAdd) {
//			toRemove.removeAll(toAdd); // Lucky day for some synapses
//			toRemove.sort(SrcTarPair.getComparator());
//			toAdd.sort(SrcTarPair.getComparator());
//			int q_st=0, q_ed=0, p_st=0, p_ed = 0;
//			for(int ii=0; ii<width; ++ii) {
//				
//				while(toRemove.get(q_ed).tar==ii) {
//					q_ed++;
//				}
//			}
//			
//	}
	
	/**
	 * Perform all node level updates, including processing arriving action
	 * potentials (including UDF short term plasticity and pre- triggered STDP),
	 * processing all action potentials in member neurons (perform post-STDP
	 * on the afferents within this node), adding all changes in weights to their
	 * respective weights, reporting the new tota
	 * @param time
	 * @param dt
	 */
	public void update(final double time, final double dt) {
		
		if((parent_sector.allExcSNon && type.isExcitatory())
				|| (parent_sector.allInhSNon && !type.isExcitatory()))
			normalizeNoCheck();
		else
			normalizeCheck();

		// Check for spikes from the source and place them for processing based on the synaptic delay
		placeNewEvents(time);
		
		processEvents(time, dt); // Figure out what APs arrived and add their current
		
		handlePostSpikes(time, dt); // Handle STDP for target neurons that spike...
		
		updateWeightsAndSums();
		
		// If the source for this layer is not an exogenous input and mhp is
		// on for the target group, perform the first stage of MHP involving
		// determining contributions from pre-synaptic neurons
		if(targData.mhpOn && !inputIsExternal && useMHP) {
			for(int ii=0; ii<width; ++ii) {
				NeuronFunctions.metaHPStage1(ii, targData.estFR[ii],
						targData.prefFR[ii],
						((MANANeurons) srcData).estFR,
						localPFRDep, localPFRPot, tarSrcMap[ii]);
			}
		}
		
		
		// Thread executing last node in the sector responsible for
		// updating sector-variables
		if(parent_sector.countDown.decrementAndGet() == 0) {
			parent_sector.updateNoSync(time, dt);
		}
	}

	/**
	 * 
	 */
	public void updateWeightsAndSums() {
		// Add up the new sum of incoming weights for the weights in this node
		// to the neurons in this node and update weight values
		for(int ii=0; ii < width; ++ii) {
			localSums[ii] = 0;
			for(int jj=0, m = weights[ii].length; jj<m; ++jj) {
				weights[ii][jj] += dws[ii][jj];
			}
			for(int jj=0, m = weights[ii].length; jj<m; ++jj) {
				if(weights[ii][jj]<0) 
					weights[ii][jj]=0;
			}
			for(int jj=0, m = weights[ii].length; jj<m; ++jj) {
				localSums[ii] += weights[ii][jj];
			}
		}
	}
	
	/**
	 * 
	 */
	public void normalizeCheck() {
		double[] sums, scaleFs;
		boolean[] snOn;
		if(type.isExcitatory()) {
			sums = parent_sector.secExcSums;
			scaleFs = targData.exc_sf;
			snOn = targData.excSNon;
		} else {
			sums = parent_sector.secInhSums;
			scaleFs = targData.inh_sf;
			snOn = targData.inhSNon;
		}
		for(int ii=0; ii<width; ++ii) {
			if(snOn[ii]) {
				double scNVal=targData.normVals[ii]*scaleFs[ii]/sums[ii];
				for(int jj=0, m=weights[ii].length; jj<m; ++jj) {
					weights[ii][jj] *= scNVal;
				}
			}
		}
	}
	
	public void normalizeNoCheck() {
		double[] sums, scaleFs;
		if(type.isExcitatory()) {
			sums = parent_sector.secExcSums;
			scaleFs = targData.exc_sf;
		} else {
			sums = parent_sector.secInhSums;
			scaleFs = targData.inh_sf;
		}
		for(int ii=0; ii<width; ++ii) {
			double scNVal=targData.normVals[ii]*scaleFs[ii]/sums[ii];
			for(int jj=0, m=weights[ii].length; jj<m; ++jj) {
				weights[ii][jj] *= scNVal;
			}
		}
	}

	/**
	 * 
	 * @param time
	 */
	public void placeNewEvents(final double time) {
		for(int ii=0; ii<height; ++ii) {
			if(srcData.getSpikes()[ii]) {
				int[][] map = srcTarMap.get(ii);
				for(int jj=0; jj<map[1].length; ++jj) {
					int tarNo = map[0][jj];
					int srcAddress = map[1][jj];
					double delay = tarDlyMap[tarNo][srcAddress];
					Event ev = new Event(time+delay, ii, srcAddress, synapses[tarNo][srcAddress]);
					eventQ.get(tarNo).add(ev);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param time
	 */
	public void processEvents(final double time, final double dt) {
		for(int ii=0; ii<width; ++ii) {
			boolean init=true;
			while(eventQ.get(ii).peek().arrTime <= time) {
				if(init) {
					evtInds[ptr] = ii;
					evtCurrents[ptr] = 0;
				}
				init=false;
				int index = eventQ.get(ii).peek().srcAddress;
				Event evt_loc = eventQ.get(ii).poll();
				dws[ii][index]=SynapseFunctions.STDP(type, time, targData.lastSpkTime[ii], type.getLRate()); // new dw/dt
				evtCurrents[ptr] += SynapseFunctions.getPSR_UDF(evt_loc.synDat, time);
				lastArrs[ii][index] = time;
			}
			if(!init) {
				++ptr;
			}
		}
	}
	
	/**
	 * Perform STDP (or anything else...) to handle post-synaptic spikes.
	 * @param time
	 * @param dt
	 */
	public void handlePostSpikes(final double time, final double dt) {
		for(int ii=0; ii<width; ++ii) {
			if(targData.spks[ii]) {
				SynapseFunctions.STDP(type, lastArrs[ii], dws[ii], time, type.getLRate());
			}
		}
	}

	public void accumLocalOutDegs(int [] outDs) {
		for(int ii=0; ii<height; ++ii) {
			outDs[ii] += srcTarMap.get(ii)[0].length;
		}
	}
	
	public int getEvtPtr() {
		return ptr;
	}
	
	public void clearEvtCurrents() {
		ptr=0;
	}
	
	public static final class Event {
		public final double arrTime;
		/** Index of the source neuron in the source neuron group.*/
		public final int srcInd;
		/** Actual location  of synapse variables in the local array. */
		public int srcAddress; 
		public final SynapseData synDat;
		
		public static final Comparator<Event> evtComp = (a, b) -> {
			// Sort first by arrival time
			if (a.arrTime < b.arrTime) {
				return -1;
			} else if (a.arrTime > b.arrTime) {
				return 1;
			} else {
				// Then by data location... if we're lucky it'll sometimes improve data locality during post-processing.
				if(a.srcAddress < b.srcAddress) {
					return -1;
				} else if (a.srcAddress > b.srcAddress) {
					return 1;
				} else {
					return 0;
				}
			}
		};
		
		public Event(final double _arrTime, final int _srcInd,
				final int _srcAddress, final SynapseData _synDat ) {
			this.arrTime = _arrTime;
			this.srcInd = _srcInd;
			this.srcAddress = _srcAddress;
			this.synDat = _synDat;
		}
	}
	

	
	
	
	
}
