package nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import base_components.InputNeurons;
import base_components.MANANeurons;
import base_components.Neuron;
import base_components.SynapseData;
import base_components.SynapseData.SynType;
import functions.MHPFunctions;
import functions.STDPFunctions;

public class MANA_Node {

	/** The sector this node belongs to/is managed by. Must have the same target neurons. */
	public MANA_Sector parent_sector;

	/** Index among other nodes in the sector. */
	public int sector_index;

	/** 
	 * Event (AP) source and the neurons (targets of APs/Synapses)
	 *  whose afferent synapses we opperate on
	 */
	public final Neuron srcData;

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

	private int widthAdj, heightAdj;
	
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
	public Map<Integer, int[][]> srcTarMap;

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

	public int[] localInDegrees;
	public int[] localOutDegrees;


	/**
	 * Constructor intended for use where the srcData is an input unit, but can be used in any
	 * situation where the weights and connectivty are prespecified.
	 * @param src
	 * @param targ
	 * @param _type
	 * @param _tarSrcMap
	 * @param _tarDlyMap
	 * @param _weights
	 */
	public MANA_Node(final Neuron src, final MANANeurons targ, SynType _type,
			final int[][] _tarSrcMap, final double[][] _tarDlyMap, double[][] _weights) {
		width = _tarSrcMap.length;
		type = _type;
		height = src.getSize();
		inputIsExternal = src instanceof InputNeurons;
		targData = targ;
		srcData = src;
		isTransUnit=false;
		this.weights = _weights;
		this.tarDlyMap = _tarDlyMap;
		this.tarSrcMap = _tarSrcMap;
		
		initBasic(); // Initialize all the values that aren't dependent upon the context of this constructor
		
		dws = new double[width][];
		lastArrs = new double[width][];
		synapses = new SynapseData[width][];
		for(int ii=0; ii<width; ++ii) {
			int inD = weights[ii].length;
			dws[ii] = new double[inD];
			Arrays.fill(weights, SynapseData.DEF_NEW_WEIGHT);
			lastArrs[ii] = new double[inD];
			synapses[ii] = new SynapseData[inD];
			for(int jj=0; jj<inD; ++jj) {
				synapses[ii][jj] = new SynapseData(type, weights[ii], dws[ii], lastArrs[ii], jj);
			}
		}
		
		
		// Calculate the src->target map
		resetSrcTarMap();
	}
	
	/**
	 * 
	 * @param src
	 * @param targ
	 * @param _type
	 * @param tarDlys
	 */
	public MANA_Node(final Neuron src, final MANANeurons targ,
			final SynType _type,
			final double [][] tarDlys,
			final boolean _transUnit) 
	{
		this.srcData = src;
		this.targData = targ;
		this.type = _type;
		inputIsExternal = src instanceof InputNeurons;
		this.isTransUnit = _transUnit;

		width = targ.getSize();
		height = src.getSize();
		initBasic();

		tarDlyMap = tarDlys;
		// Default is initialized to all to all
		synapses = new SynapseData[width][heightAdj];
		weights = new double[width][heightAdj];
		dws = new double[width][heightAdj];
		lastArrs = new double[width][heightAdj];
		tarSrcMap = new int[width][heightAdj];
		for(int ii=0; ii < width; ++ii) {
			eventQ.add(new PriorityQueue<Event>(Event.evtComp));
			Arrays.fill(weights[ii], SynapseData.DEF_NEW_WEIGHT);
			int off = 0;
			for(int jj=0; jj<heightAdj; ++jj) {
				if(src==targ && jj==ii) {
					off=1;
				}
				tarSrcMap[ii][jj] = jj+off;
				synapses[ii][jj] = new SynapseData(type, weights[ii], dws[ii], lastArrs[ii], jj);
			}
			
		}
		resetSrcTarMap();
	}

	/**
	 * Initializes all the basic values that don't really change depending on the constructor.
	 */
	private void initBasic() {
		localOutDegrees = new int[height];
		localInDegrees = new int[width];
		if(srcData == targData) {
			heightAdj = srcData.getSize()-1;
			widthAdj = targData.getSize()-1;
		} else {
			heightAdj = srcData.getSize();
			widthAdj = targData.getSize();
		}
		Arrays.fill(localOutDegrees, widthAdj);
		Arrays.fill(localInDegrees, heightAdj);
		
		// Initializing all the 1-D arrays (representing source or target data...)
		localSums = new double[width];
		localPFRPot = new double[width];
		localPFRDep = new double[width];
		evtCurrents = new double[width];
		evtInds = new int[width];
		eventQ = new ArrayList<PriorityQueue<Event>>();
	}

	/**
	 * Given a list of synapses to remove, new synapses to add (both based on target/source pairings) and delays
	 * adds/removes the synapses to/from this group... given the rigid performance-minded data-structures,
	 * requires a lot of new allocations and removals of old data. THIS IS VERY EXPENSIVE DO NOT CALL OFTEN. 
	 * @param toRemove
	 * @param toAdd
	 * @param toAddDlys
	 */
	public void reform(final Map<Integer, Set<Integer>> toRemove, final Map<Integer, List<Integer>> toAdd,
			final double[][] toAddDlys) {
		for(int ii=0; ii<width; ++ii) {
			int newSize;
			int [] newTarSrcMap;
			double [] newLastArrs, newDws, newWeights, newTarDlys;
			SynapseData[] newSynDat;
			if(toRemove.containsKey(ii)) {
				if(toAdd.containsKey(ii)) {
					toRemove.get(ii).removeAll(toAdd.get(ii));
					newSize = weights[ii].length + toAdd.get(ii).size() - toRemove.get(ii).size();
				} else {
					newSize = weights[ii].length - toRemove.get(ii).size();
				}
			} else {
				if(toAdd.containsKey(ii)) {
					newSize = weights[ii].length + toAdd.get(ii).size();

				} else {
					continue; // Nothing to add Or Remove
				}
			}
			// Initialize replacement arrays of the new correct size given additions/subtractions
			// of synapses...
			newTarSrcMap = new int[newSize];
			newLastArrs = new double[newSize];
			newDws = new double[newSize];
			newWeights = new double[newSize];
			newSynDat = new SynapseData[newSize];
			newTarDlys = new double[newSize];

			int kk=0;
			// If there are incoming synapses to remove...
			if(toRemove.containsKey(ii) && !toRemove.get(ii).isEmpty()) {
				for(int jj=0, m = weights[ii].length; jj<m; ++jj) {
					// Does the remove list for this (ii) target neuron contain
					// the index for the source neuron at jj index indicating
					// it should be removed?
					if(!toRemove.get(ii).contains(tarSrcMap[ii][jj])) { // Synapse is NOT in the remove list
						// So copy over data.
						newTarSrcMap[kk] = tarSrcMap[ii][jj];
						newLastArrs[kk] = lastArrs[ii][jj];
						newDws[kk] = dws[ii][jj];
						newWeights[kk] = weights[ii][jj];
						newSynDat[kk] = new SynapseData(synapses[ii][jj], kk,
								newLastArrs, newWeights, newDws);
						newTarDlys[kk] = tarDlyMap[ii][jj];
						for(Event evt : eventQ.get(ii)) {
							if(evt.synDat == synapses[ii][jj]) {
								evt.synDat = newSynDat[kk];
							}
						}
						kk++;
					} else {// The synapse IS in the remove list; DO NOT copy over the data
						// Decrement the appropriate degree counts.
						localOutDegrees[tarSrcMap[ii][jj]]--;
						localInDegrees[ii]--;
					}

				}
				// Remove events corresponding to deleted synapses
				Iterator<Event> evtIt = eventQ.get(ii).iterator();
				while(evtIt.hasNext()) {
					if(toRemove.get(ii).contains(evtIt.next().synDat.index)) {
						evtIt.remove();
					}
				}
			} else {
				kk=weights[ii].length;
				System.arraycopy(weights[ii], 0, newWeights, 0, kk);
				System.arraycopy(dws[ii], 0, newDws, 0, kk);
				System.arraycopy(lastArrs[ii], 0, newLastArrs[ii], 0, kk);
				System.arraycopy(tarSrcMap[ii], 0, newTarSrcMap[ii], 0, kk);
				System.arraycopy(tarDlyMap[ii], 0, newTarDlys[ii], 0, kk);
				for(int jj=0; jj<kk; ++jj) {
					synapses[ii][jj].lastArr = newLastArrs;
					synapses[ii][jj].dw = newDws;
					synapses[ii][jj].w = newWeights;
					// index remains the same...
				}
			}
			// Now check if we need to add any new synapses...
			int ll = kk;
			if(toAdd.containsKey(ii) && !toAdd.get(ii).isEmpty()) {
				// Yes, so kk is where we left off in the new array if
				for(Integer newSrc : toAdd.get(ii)) {
					newTarSrcMap[kk] = newSrc;
					newWeights[kk] = SynapseData.DEF_NEW_WEIGHT;
					newSynDat[kk] = new SynapseData(type, newWeights, newDws, newLastArrs, kk);
					newTarDlys[kk] = toAddDlys[ii][kk-ll]; // only contains new data so must subtract ll to index
					localOutDegrees[newSrc]++;
					localInDegrees[ii]++;
					kk++;
				}
			}
			// Replace old data for each target with the new data arrays which no longer
			// contain data for removed synapses and contain data for added synapses
			tarSrcMap[ii] = newTarSrcMap;
			weights[ii] = newWeights;
			synapses[ii] = newSynDat;
			lastArrs[ii] = newLastArrs;
			tarDlyMap[ii] = newTarDlys;

		}
		// Finally change the source->target mapping used for event scheduling to 
		// reflect added and removed synapses (nothing special just go over the
		// data in tar-src ordering and do the equivalent of histogramming)
		resetSrcTarMap();
	}
	
	/**
	 * 
	 */
	private void resetSrcTarMap() {
		Map<Integer, int[][]> newSrcTarMap = new HashMap<Integer, int[][]>();
		int[] counters = new int[height];
		for(int jj=0; jj<height; ++jj) {
			newSrcTarMap.put(jj, new int[2][localOutDegrees[jj]]);
		}
		for(int ii=0; ii<width; ++ii) {
			for(int kk=0; kk<weights[ii].length; ++kk) {
				int srcInd = tarSrcMap[ii][kk];
				int[][] mp = newSrcTarMap.get(srcInd);
				int ind = counters[srcInd];
				mp[0][ind] = ii;
				mp[1][ind] = kk;
				counters[srcInd]++;
			}
		}
		srcTarMap = newSrcTarMap;
	}
	
	/**
	 * Gives the source neurons that DO NOT connect to the target neuron with
	 * the given (tarNo) index. Not particularly optimized... don't call often.
	 * @param tarNo
	 * @return
	 */
	public Set<Integer> getUnconnectedSrc2Tar(int tarNo) {
		HashSet<Integer> unConSet = new HashSet<Integer>();
		for(int ii=0; ii<height; ++ii) {
			unConSet.add(ii);
		}
		for(int jj=0; jj<tarSrcMap[tarNo].length; ++jj) {
			unConSet.remove(tarSrcMap[tarNo][jj]);
		}
		return unConSet;
	}

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
		scheduleNewEvents(time);

		processEvents(time, dt); // Figure out what APs arrived and add their current

		handlePostSpikes(time, dt); // Handle STDP for target neurons that spike...

		updateWeightsAndSums();

		// If the source for this layer is not an exogenous input and mhp is
		// on for the target group, perform the first stage of MHP involving
		// determining contributions from pre-synaptic neurons
		if(targData.mhpOn && !inputIsExternal && useMHP) {
			for(int ii=0; ii<width; ++ii) {
				MHPFunctions.metaHPStage1(ii, targData.estFR[ii],
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
	 * Synaptic normalization where whether or not SN is turned on is
	 * checked for each node.
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

	/**
	 * Synaptic Normalization where whether or not SN is turned on it is
	 * executed for each node. Intended to be called after all neurons have
	 * had their SN turned on.
	 */
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
	public void scheduleNewEvents(final double time) {
		for(int ii=0; ii<height; ++ii) {
			if(srcData.getSpikes()[ii]) {
				int[][] map = srcTarMap.get(ii);
				for(int jj=0; jj<map[1].length; ++jj) {
					int tarNo = map[0][jj];
					int srcAddress = map[1][jj];
					double delay = tarDlyMap[tarNo][srcAddress];
					Event ev = new Event(time+delay, ii, synapses[tarNo][srcAddress]);
					eventQ.get(tarNo).add(ev);
				}
			}
		}
	}

	/**
	 * Processes spikes that happened in the source neuron which arrive in this time-step.
	 * Stores the indices of each target neuron where at least one spike arrived and the total current
	 * imbued at each targed by all APs that arrive on this time-step. Actual summing of these values into
	 * the incoming current arrays of the target neuron IS DONE AT THE SECTOR LEVEL.
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
				int index = eventQ.get(ii).peek().synDat.index;
				Event evt_loc = eventQ.get(ii).poll();
				dws[ii][index]=STDPFunctions.STDP(type, time, targData.lastSpkTime[ii], type.getLRate()); // new dw/dt
				evtCurrents[ptr] += STDPFunctions.getPSR_UDF(evt_loc.synDat, time);
				lastArrs[ii][index] = time;
			}
			if(!init) {
				++ptr;
			}
		}
		// NOTE: Acutally adding these currents to the target's currents is done AT THE SECTOR LEVEL!!!
	}

	/**
	 * Perform STDP (or anything else...) to handle post-synaptic spikes.
	 * @param time
	 * @param dt
	 */
	public void handlePostSpikes(final double time, final double dt) {
		for(int ii=0; ii<width; ++ii) {
			if(targData.spks[ii]) {
				STDPFunctions.STDP(type, lastArrs[ii], dws[ii], time, type.getLRate());
			}
		}
	}

	public void accumLocalOutDegs(int [] outDs) {
		for(int ii=0; ii<height; ++ii) {
			outDs[ii] += localOutDegrees[ii];
		}
	}

	public int getEvtPtr() {
		return ptr;
	}

	public void clearEvtCurrents() {
		ptr=0;
	}

	/**
	 * @return whether or not this node contains excitatory synapses (the source is excitatory)
	 */
	public boolean isExcitatory() {
		return type.isExcitatory();
	}
	
	/**
	 * TODO: Pull this out into its own separate class and put other utilities for handling events there too...
	 * @author z
	 *
	 */
	public static final class Event {
		public final double arrTime;
		/** Index of the source neuron in the source neuron group.*/
		public final int srcInd;
		//		/** Actual location  of synapse variables in the local array. */
		//		public int srcAddress; 
		public SynapseData synDat;

		public static final Comparator<Event> evtComp = (a, b) -> {
			// Sort first by arrival time
			if (a.arrTime < b.arrTime) {
				return -1;
			} else if (a.arrTime > b.arrTime) {
				return 1;
			} else {
				// Then by data location... if we're lucky it'll sometimes improve data locality during post-processing.
				if(a.synDat.index < b.synDat.index) {
					return -1;
				} else if (a.synDat.index > b.synDat.index) {
					return 1;
				} else {
					return 0;
				}
			}
		};

		public Event(final double _arrTime, final int _srcInd, final SynapseData _synDat ) {
			this.arrTime = _arrTime;
			this.srcInd = _srcInd;
			this.synDat = _synDat;
		}
	}






}
