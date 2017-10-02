package functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import nodes.MANA_Node;
import nodes.MANA_Sector;
import nodes.MANA_Unit;
import utils.Utils;

public class SPFunctions {

	
	public static void pruneGrow(MANA_Unit unit, double excThresh,
			double inhThresh, double absMin, double lambda, double con_const,
			double maxAdd) {
		Map<MANA_Node, Map<Integer, Set<Integer>>> pruneMap = prune(unit, excThresh, inhThresh, absMin);
	}
	
	public static Map<MANA_Node, Map<Integer, Set<Integer>>> prune(
			MANA_Unit unit, 
			double excThresh,
			double inhThresh,
			double absMin) {
		double maxExc = unit.findGlobalMaxExc();
		double maxInh = unit.findGlobalMaxInh();
		double excTestCut = maxExc * excThresh;
		double inhTestCut = maxInh * inhThresh;
		int noExc = unit.numAllExc;
		int noInh = unit.numInh;
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
	
	public static Map<MANA_Node, Map<Integer, List<Integer>>> grow(
			MANA_Unit unit, int totalRemovedExc, int totalRemovedInh, int maxAdd, double lambda, double con_const){
		Map<MANA_Node, Map<Integer, List<Integer>>> growM = new HashMap<MANA_Node, Map<Integer, List<Integer>>>();
		final double lambdaSq = lambda*lambda;
		// Randomly grow connections based on distance...
		for(MANA_Node node : unit.nodes) {
			growM.put(node, new HashMap<Integer, List<Integer>>());
			double[][] srcXYZ = node.srcData.getCoordinates();
			double[][] tarXYZ = node.targData.getCoordinates();
			int[][] disConMap = node.getDisconnected();
			for(int ii=0; ii<node.width; ++ii) {
				growM.get(node).put(ii, new LinkedList<Integer>());
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
		int[] ei = new int[2];
		mapPop(growM, ei);
		// Now remove from the growth list randomly until we are in line with our quota...
		while(ei[0] + ei[1] > maxAdd) {
			int nodeInd = ThreadLocalRandom.current().nextInt(unit.nodes.size());
			MANA_Node localNode = unit.nodes.get(nodeInd);
			if(localNode.isExcitatory()) {
				if(ei[0] <= totalRemovedExc) continue;
				int neuronInd = ThreadLocalRandom.current().nextInt(localNode.width);
				if(!growM.get(localNode).get(neuronInd).isEmpty()) {
					int synInd = ThreadLocalRandom.current().nextInt(growM.get(localNode).get(neuronInd).size());
					growM.get(localNode).get(neuronInd).remove(synInd);
					ei[0]--;
				}
			} else {
				if(ei[1] <= totalRemovedExc) continue;
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
	
	private static <V> void mapPop(Map<MANA_Node, Map<V, List<V>>> _map, int[] ei) {
		for(MANA_Node t : _map.keySet()) {
			for(V v : _map.get(t).keySet()) {
				if(t.isExcitatory()) {
					ei[0] += _map.get(t).get(v).size();
				} else {
					ei[1] += _map.get(t).get(v).size();
				}
			}
		}
	}
	
}
