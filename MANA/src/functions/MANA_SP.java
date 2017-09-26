package functions;

import java.util.ArrayList;
import java.util.List;

import nodes.MANA_Node;
import nodes.MANA_Unit;

public class MANA_SP {

	
	public static void prune(
			MANA_Unit unit, 
			int[][] excPruned, 
			int[][] inhPruned,
			double excThresh,
			double inhThresh,
			double absMin) {
		double maxExc = unit.findGlobalMaxExc();
		double maxInh = unit.findGlobalMaxInh();
		double excTestCut = maxExc * excThresh;
		double inhTestCut = maxInh * inhThresh;
		int noExc;
		int noInh;
		for(MANA_Node node : unit.nodes) {
			double thresh = node.type.isExcitatory() ? excTestCut : inhTestCut;
			List<int[]> toRemove = new ArrayList<int[]>();
			for(int ii=0; ii<node.width; ++ii) {
				for(int jj=0; jj<node.weights[ii].length; ++jj) {
					if(node.weights[ii][jj] < absMin) {
						toRemove.add(new int[]{ii, jj});
						continue;
					}
					if(node.weights[ii][jj] < thresh) {
						
					}
				}
			}
		}
		
		
	}
	
}
