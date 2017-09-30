package functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import nodes.MANA_Node;
import nodes.MANA_Sector;
import nodes.MANA_Unit;

public class SPFunctions {

	
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
			MANA_Unit unit, int totalRemoved, int maxAdd, double lambda, double con_const){
		return null;
	}
	
}
