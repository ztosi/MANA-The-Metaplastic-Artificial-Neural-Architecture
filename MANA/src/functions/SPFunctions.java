package functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import base_components.SynapseData;
import nodes.MANA_Node;
import nodes.MANA_Sector;
import nodes.MANA_Unit;
import utils.Utils;

public class SPFunctions {

	public static final double DEF_EXC_THRESH = 0.05;
	public static final double DEF_INH_THRESH = 0.05;
	public static final double DEF_CON_CONST = 0.4;
    public static final double P_ADD_MIN = 0.01;
    public static final double RT_LOG_PA_MIN = Math.sqrt(-Math.log(P_ADD_MIN));	
    public static final double DEF_PG_INTERVAL = 5000; // ms
    
	/**
	 * Performs the growth and pruning operations on the given MANA unit
	 * @param unit
	 * @param excThresh
	 * @param inhThresh
	 * @param absMin
	 * @param lambda
	 * @param con_const
	 * @param maxAdd
	 */
	public static void pruneGrow(MANA_Unit unit, double excThresh,
			double inhThresh, double absMin, double lambda, double con_const,
			int maxAdd) {
		Map<MANA_Node, Map<Integer, Set<Integer>>> pruneMap = prune(unit, excThresh, inhThresh, absMin);
		int[] eiPop = new int[2];
		mapPop(pruneMap, eiPop);
		System.out.println("Exc: " + eiPop[0] + " Inh: " + eiPop[1]);
		Map<MANA_Node, Map<Integer, Set<Integer>>> growthMap = 
				grow(unit, eiPop[0], eiPop[1], maxAdd, lambda, con_const);
		Map<MANA_Node, double[][]> nodeDlys = getDelays(growthMap, SynapseData.MAX_DELAY, unit.getMaxDist());
		for(MANA_Node node : unit.nodes) {
			node.reform(pruneMap.get(node), growthMap.get(node), nodeDlys.get(node));
		}
	}
	
	/**
	 * Based on the sandard pruning function from Tosi 2017, selects synapses for removal. This funcion
	 * returns a map which gives the information necessary to find the synapses which should be pruned.
	 * IT DOES NOT PRUNE THEM ITSELF.
	 * @param unit The MANA Unit (recurrent reservoir and external inputs) whose synapses will be pruned
	 * @param excThresh all Exc. synapses with weights less than excThresh * MAX(W_e*) [the maximum exc. weight, are *candidates* for removal.
	 * @param inhThresh same as excThresh, but for inhibitory synapses
	 * @param absMin the absolute weakest synapse allowed (All weaker synapses are pruned unconditionally)
	 * @return A map where each MANA_Node in the unit is the key to another map, which maps target neuron ids (within node) to source
	 * neuron ids (within node) which have been selected for removal.
	 */
	public static Map<MANA_Node, Map<Integer, Set<Integer>>> prune(
			MANA_Unit unit, 
			double excThresh,
			double inhThresh,
			double absMin) {
		double maxExc = unit.findGlobalMaxExc();
		double maxInh = unit.findGlobalMaxInh();
		double excTestCut = maxExc * excThresh;
		double inhTestCut = maxInh * inhThresh;
		int noExc = unit.getNumAllExc();
		int noInh = unit.getNumInh();
		for(MANA_Sector sec : unit.sectors) {
			sec.updateInDegrees();
		}
		Map<MANA_Node, Map<Integer, Set<Integer>>> remove =
				new HashMap<MANA_Node, Map<Integer, Set<Integer>>>();
		for(MANA_Node node : unit.nodes) {
			double thresh = node.isExcitatory() ? excTestCut : inhTestCut;
			Map<Integer, Set<Integer>> toRemove = new HashMap<Integer, Set<Integer>>();
			int[] inDs = node.isExcitatory() ? node.targData.excInDegree 
					: node.targData.inhInDegree;
			int inDMax = node.isExcitatory() ? noExc : noInh;
			int[] outDs = node.srcData.getOutDegree();
			for(int ii=0; ii<node.width; ++ii) {
				toRemove.put(ii, new HashSet<Integer>());
				for(int jj=0; jj<node.weights[ii].length; ++jj) {
					if(node.weights[ii][jj] < absMin) {
						toRemove.get(ii).add(jj);
						continue;
					}
					if(node.weights[ii][jj] < thresh) {
						double oDcont = (double)outDs[ii]/(noExc+noInh);
						double p_rm = (double)inDs[ii]/inDMax * oDcont * oDcont;
						if(ThreadLocalRandom.current().nextDouble() < p_rm) {
							toRemove.get(ii).add(jj);
						}
					}
				}
			}
			remove.put(node, toRemove);
		}
		return remove;
	}
	
	/**
	 * 
	 * @param unit The MANA unit where we are going to attempt to grow synapses
	 * @param totalRemovedExc the number of excitatory synapses staged to be removed
	 * @param totalRemovedInh same as totalRemovedExc but for inhibitory synapses
	 * @param maxAdd the maximum total number of synapses that can be grown.
	 * @param lambda 
	 * @param con_const
	 * @return
	 */
	public static Map<MANA_Node, Map<Integer, Set<Integer>>> grow(
			MANA_Unit unit, int totalRemovedExc, int totalRemovedInh, int maxAdd, double lambda, double con_const){
		Map<MANA_Node, Map<Integer, Set<Integer>>> growM = new HashMap<MANA_Node, Map<Integer, Set<Integer>>>();
		final double lambdaSq = lambda*lambda;
		// Randomly grow connections based on distance... untill at least the max we are allowed to add has been reached
		boolean cycle1 = true;
		int[] ei = new int[2];
		while(ei[0]+ei[1] < maxAdd) {
			for(MANA_Node node : unit.nodes) {
				if(cycle1)
					growM.put(node, new HashMap<Integer, Set<Integer>>());
				double[][] srcXYZ = node.srcData.getCoordinates();
				double[][] tarXYZ = node.targData.getCoordinates();
				int[][] disConMap = node.getDisconnected();
				for(int ii=0; ii<node.width; ++ii) {
					if(cycle1)
						growM.get(node).put(ii, new LinkedHashSet<Integer>());
					for(int jj=0, m=disConMap[ii].length; jj<m; ++jj) {
						int srcInd = disConMap[ii][jj];
						double dist = Utils.euclidean(srcXYZ, tarXYZ, srcInd, ii);
						double prob = con_const*Math.exp(-(dist*dist)/lambdaSq);
						if(ThreadLocalRandom.current().nextDouble() < prob) {
							growM.get(node).get(ii).add(srcInd);
						}
					}
				}
			}
			cycle1=false;
			mapPop(growM, ei);
		}
		int maxAddExc = (int)(maxAdd * 0.8); // TODO: Magic Number...
		int maxAddInh = (int)(maxAdd * 0.2); // TODO: Magic Number...
		// Now remove from the growth list randomly until we are in line with our quota...
		while(ei[0]+ei[1] > maxAdd || ei[0]+ei[1] > totalRemovedExc+totalRemovedInh) {
			int nodeInd = ThreadLocalRandom.current().nextInt(unit.nodes.size());
			MANA_Node localNode = unit.nodes.get(nodeInd);
			if(localNode.isExcitatory()) {
				// Stop removing if the number we want to add is less than or equal
				// to the max allowed but also less than or equal to the total that
				// has been removed. That is, keep removing add candidates
				// until we are adding less than the max allowed or less than
				// the total which were removed if it is smaller
				if(ei[0] <= totalRemovedExc && ei[0] <= maxAddExc) continue;
				int neuronInd = ThreadLocalRandom.current().nextInt(localNode.width);
				if(!growM.get(localNode).get(neuronInd).isEmpty()) {
					int synInd = ThreadLocalRandom.current().nextInt(growM.get(localNode).get(neuronInd).size());
					growM.get(localNode).get(neuronInd).remove(synInd);
					ei[0]--;
				}
			} else {
				if(ei[1] <= totalRemovedExc && ei[1] <= maxAddInh) continue;
				int neuronInd = ThreadLocalRandom.current().nextInt(localNode.width);
				if(!growM.get(localNode).get(neuronInd).isEmpty()) {
					int synInd = ThreadLocalRandom.current().nextInt(growM.get(localNode).get(neuronInd).size());
					growM.get(localNode).get(neuronInd).remove(synInd);
					ei[1]--;
				}
			}
		}
		return growM;
	}
	
	/**
	 * 
	 * @param map
	 * @param maxDly
	 * @param maxDist
	 * @return
	 */
	public static Map<MANA_Node, double [][]> getDelays(
			Map<MANA_Node, Map<Integer, Set<Integer>>> map,
			double maxDly, double maxDist) {
		Map<MANA_Node, double[][]> nodeDlys = new HashMap<MANA_Node, double[][]>();
		double ratio = maxDly/maxDist;
		for(MANA_Node node : map.keySet()) {
			double [][] dlysLoc = new double[node.width][];
			for(int ii=0; ii<node.width; ++ii) {
				dlysLoc[ii] = new double[map.get(node).get(ii).size()];
				int jj=0;
				for(Integer ind : map.get(node).get(ii)) {
					dlysLoc[ii][jj++] = ratio * Utils.euclidean(
							node.srcData.getCoordinates(),
							node.targData.getCoordinates(), ind, ii);
				}
			}
		}
		return nodeDlys;
	}
	
	private static <V> void mapPop(Map<MANA_Node, Map<V, Set<V>>> growM, int[] ei) {
		if (growM.isEmpty()) {
			ei[0]=0;
			ei[1]=0;
			return;
		}
		for(MANA_Node t : growM.keySet()) {
			for(V v : growM.get(t).keySet()) {
				if(t.isExcitatory()) {
					ei[0] += growM.get(t).get(v).size();
				} else {
					ei[1] += growM.get(t).get(v).size();
				}
			}
		}
	}
	
}
