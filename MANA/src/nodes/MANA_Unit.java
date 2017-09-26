package nodes;

import java.util.ArrayList;
import java.util.List;

import data_holders.MANANeurons;
import data_holders.Spiker;

/**
 * 
 * TODO: Currently just a holder for related sectors... eventually this will
 * contain functions for managing multiple "local" sectors as well as interfacing
 * with distant other units... can be thought of as roughly a cortical-column
 * equivalent sort of thing which also contains all incoming synaptic connections
 * (from anywhere) to that column. 
 * 
 * @author z
 *
 */
public class MANA_Unit {
	
	public static final double DEFAULT_BOUND_START = 50; 
	public static final double DEFAULT_BOUND_END = 150;
	
	public double x0=DEFAULT_BOUND_START, xf=DEFAULT_BOUND_END,
			y0=DEFAULT_BOUND_START, yf=DEFAULT_BOUND_END,
			z0=DEFAULT_BOUND_START, zf=2*DEFAULT_BOUND_END;
	
	public List<MANA_Sector> sectors = new ArrayList<MANA_Sector>();

	public List<Spiker> inputs = new ArrayList<Spiker>();
	
	public List<MANANeurons> targets = new ArrayList<MANANeurons>();
	
	public List<MANA_Node> nodes = new ArrayList<MANA_Node>();
	
	public double findGlobalMaxExc() {
		double max = 0;
		for(MANA_Node node: nodes) {
			if(node.type.isExcitatory()) {
				for(int ii=0; ii<node.width; ++ii) {
					for(int jj=0; jj<node.weights[ii].length; ++jj) {
						if(node.weights[ii][jj] > max) {
							max= node.weights[ii][jj];
						}
					}
				}
			}
		}
		return max;
	}
	
	public double findGlobalMaxInh() {
		double max = 0;
		for(MANA_Node node: nodes) {
			if(!node.type.isExcitatory()) {
				for(int ii=0; ii<node.width; ++ii) {
					for(int jj=0; jj<node.weights[ii].length; ++jj) {
						if(node.weights[ii][jj] > max) {
							max= node.weights[ii][jj];
						}
					}
				}
			}
		}
		return max;
	}
	
	public int getNumExcSrc() {
		return 0;
	}
	
	public double getMaxDist() {
		return Math.sqrt(Math.pow(xf-x0, 2)+Math.pow(yf-y0, 2)+Math.pow(zf-z0, 2));
	}
	
}