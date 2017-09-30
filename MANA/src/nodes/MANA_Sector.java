package nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import base_components.MANANeurons;
import functions.MHPFunctions;
import functions.STDPFunctions;
import utils.Syncable;

/**
 * 
 * @author z
 *
 */
public class MANA_Sector implements Syncable {
	
	public final int numNodes;
	public final int width;
	public double [] secExcSums;
	public double [] secInhSums;
	public boolean [] snExcOn;
	public boolean [] snInhOn;
	public double [] pfrLTDAccum;
	public double [] pfrLTPAccum;
	public double [] lastSpkTimeBuffer;
	public double [] estFRBuffer;
	public boolean [] spkBuffer;
	
	public final AtomicInteger countDown;
	
	public MANA_Node[] childNodes;
	public final MANANeurons target;
	
	public boolean allExcSNon = false;
	public boolean allInhSNon = false;
	
	public ArrayList<ArrayList<Double>> spikeTimes;
	
	/**
	 * 
	 * @param children
	 * @param target
	 * @return
	 */
	public static MANA_Sector sector_builder(final MANA_Node[] children, MANANeurons target) {
		MANA_Sector sector = new MANA_Sector(children, target);
		for(int ii=0; ii<children.length; ++ii) {
			children[ii].parent_sector = sector;
			children[ii].sector_index = ii;
		}
		return sector;
	}
	
	/**
	 * 
	 * @param children
	 * @param _target
	 */
	private MANA_Sector(MANA_Node [] children, MANANeurons _target) {
		target = _target;
		numNodes = children.length;
		countDown = new AtomicInteger(numNodes);
		childNodes = children;
		width = children[0].width;
		spikeTimes = new ArrayList<ArrayList<Double>>();
		for(int ii=0; ii<_target.getSize(); ++ii) {
			spikeTimes.add(new ArrayList<Double>());
		}
		
	}
	
	/**
	 * 
	 */
	public void updateInDegrees() {
		Arrays.fill(target.excInDegree, 0);
		Arrays.fill(target.inhInDegree, 0);
		for(MANA_Node node : childNodes) {
			int[] inDegs = node.type.isExcitatory() ? target.excInDegree : target.inhInDegree;
			for(int ii=0; ii<width; ++ii) {
				inDegs[ii] += node.localInDegrees[ii];
			}
		}
		for(int ii=0; ii<width; ++ii) {
			target.inDegree[ii] = target.excInDegree[ii] + target.inhInDegree[ii];
		}
	}
	
	/**
	 * Before the next iteration the target neurons this sector is responsible
	 * for must update these values, however during update they must remain
	 * in buffers since other MANA nodes require the data for this group
	 * of neurons from the previous time-step. Must be called before
	 * the next iteration, but after all sectors have finished updating.
	 */
	public void synchronize(final double time) {
		boolean[] holder = target.spks;
		// shallow copy over 
		target.spks = spkBuffer;
		// switch the addresses of the arrays, so a new one
		// does not have to be instantiated.
		spkBuffer = holder;
		
		for(int ii=0; ii<width; ++ii) {
			if(target.spks[ii]) {
				spikeTimes.get(ii).add(time);
			}
		}
		
		// ditto
		double[] doubleHolder = target.estFR;
		target.estFR = estFRBuffer;
		estFRBuffer = doubleHolder;
		
		// deep copy required, since the values here are not updated *every* iteration.
		System.arraycopy(lastSpkTimeBuffer, 0, target.lastSpkTime, 0, width);
		
		STDPFunctions.newNormScaleFactors(target.exc_sf, target.thresh, target.threshRA, 5, true);
		STDPFunctions.newNormScaleFactors(target.inh_sf, target.thresh, target.threshRA, 5, true);

	}
	
	/**
	 * 
	 * @param time
	 * @param dt
	 */
	public void updateNoSync(final double time, final double dt) {
		gatherChildData(time, dt);
		updateTargetNeurons(dt, time);
		countDown.set(numNodes);
	}
	
	/**
	 * After all child nodes have been updated, gather information from
	 * them that is,g gather from them the synaptic currents that arrived from
	 * their pre-synaptic neurons, the sum of their new weights, and so on.
	 * @param time
	 * @param dt
	 */
	public void gatherChildData(final double time, final double dt) {
		
		Arrays.fill(secExcSums, 0);
		Arrays.fill(secInhSums, 0);
		
		// Update normalization pools
		for(int ii=0; ii < childNodes.length; ++ii) {
			if (childNodes[ii].type.isExcitatory()) {
				for (int jj = 0; jj < width; ++jj) {
					secExcSums[jj] += childNodes[ii].localSums[jj];
				}
			} else {
				for (int jj = 0; jj < width; ++jj) {
					secInhSums[jj] += childNodes[ii].localSums[jj];
				}
				
			}
		}
		
		// Synchronize incoming exc/inh currents
		for(int ii=0; ii<numNodes; ++ii) {
			int ptr = childNodes[ii].getEvtPtr();
			if(ptr > 0) {
				int[] evtInds = childNodes[ii].evtInds;
				double[] evtCurrs = childNodes[ii].evtCurrents;
				if (childNodes[ii].type.isExcitatory()) {
					for (int jj = 0; jj<ptr ; ++jj) {
						target.i_e[evtInds[jj]] += evtCurrs[jj];
					}
				} else {
					for (int jj = 0; jj < ptr; ++jj) {
						target.i_i[evtInds[jj]] += evtCurrs[jj];			
					}
				}
				childNodes[ii].clearEvtCurrents();
			}
		}
		
		// Add up pfr change contributions from member nodes
		Arrays.fill(pfrLTDAccum, 0);
		Arrays.fill(pfrLTPAccum, 0);
		for(int ii=0; ii<numNodes; ++ii) {
			for(int jj=0; jj < width; ++jj) {
				pfrLTDAccum[jj] += childNodes[ii].localPFRDep[jj];
				pfrLTPAccum[jj] += childNodes[ii].localPFRPot[jj];
			}
		}
		
	}
	
	/**
	 * Updates information about the target neurons based on what was calculated
	 * from the synapses by the child nodes. Should be called after all child nodes
	 * updates have been executed but before synchronization. Does not update any
	 * values which are/must be visible to downstream neurons and thus can be
	 * called asynchronously.
	 * 
	 * MUST GATHER CHILD NODE DATA FIRST!!!
	 * @param dt
	 * @param time
	 */
	public void updateTargetNeurons(double dt, double time) {
		
		// Figure out voltages and who spikes for the next time-step
		// and put the new spike times in buffers (no one needs voltage
		// information non-locally during updates).
		target.update(dt, time, spkBuffer, lastSpkTimeBuffer);
		
		// Figure out the estimated firing rates for next time-step
		// and put them in a buffer
		target.updateEstFR(dt, estFRBuffer);
		
		// Figure out new target firing rates using the difference function
		// between pre- and post- synaptic estimated firing rates as calculated
		// in each node and accumulated by the sector
		// MUST CALL gatherChildData(...) first!!!
		MHPFunctions.metaHPStage2(pfrLTDAccum, pfrLTPAccum,
				target, target.eta, dt);
		
		// Homeostatic plasticity...
		target.updateThreshold(dt);
		
		// Decay MHP and HP constants...
		target.eta += dt * MANANeurons.mhp_decay * (MANANeurons.final_tau_MHP - target.eta);
		target.lambda += dt * MANANeurons.hp_decay * (MANANeurons.final_tau_HP - target.lambda);
		
		// Check if excitatory synaptic totals for each neuron have exceeded their
		// norm values for this first time, and turn on synaptic normalization for
		// each neuron for which that is true
		if(!allExcSNon) {
			allExcSNon = true;
			for(int ii=0; ii<width; ++ii) {
				allExcSNon &= target.excSNon[ii];
				if(!target.excSNon[ii]) {
					if(secExcSums[ii] >= target.normVals[ii]*target.exc_sf[ii]) {
						target.excSNon[ii] = true;
					}
					
				}
			}
		}
		if(!allInhSNon) {
			allInhSNon = true;
			for(int ii=0; ii<width; ++ii) {
				allInhSNon &= target.inhSNon[ii];
				if(!target.inhSNon[ii]) {
					if(secInhSums[ii] >= target.normVals[ii]*target.inh_sf[ii]) {
						target.inhSNon[ii] = true;
					}
					
				}
			}
		}

		
		
	}
	

}
