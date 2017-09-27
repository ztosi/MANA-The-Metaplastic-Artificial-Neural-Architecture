package nodes;

import java.util.ArrayList;
import java.util.List;

import data_holders.InputData;
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
	
	public static final int DEFAULT_NODE_DIM = 200;
	public static final double DEFAULT_BOUND_START = 50; 
	public static final double DEFAULT_BOUND_END = 150;
	public static final double DEFAULT_INP_WITDTH = 200;
	private static final int DEFAULT_INP_WIDTH = 0;
	
	public double x0=DEFAULT_BOUND_START, xf=DEFAULT_BOUND_END,
			y0=DEFAULT_BOUND_START, yf=DEFAULT_BOUND_END,
			z0=DEFAULT_BOUND_START, zf=2*DEFAULT_BOUND_END;
	
	public final int fullSize, size, numExc, numInh, noSecs, nodesPerSec, noInp;
	
	public double [][] xyzCoors;
	
	public List<MANA_Sector> sectors = new ArrayList<MANA_Sector>();

	public List<Spiker> inputs = new ArrayList<Spiker>();
	
	public List<MANANeurons> targets = new ArrayList<MANANeurons>();
	
	public List<MANA_Node> nodes = new ArrayList<MANA_Node>();
	
	public MANA_Unit(final String _inpFileName, int _N) {
		InputData inp = new InputData(_inpFileName);
		if(_N%200 != 0 || _N < 1000) {
			System.out.println("The number you entered is... "
					+ "annoying... and I'm too lazy to deal with it. "
					+ "Rounding up to an easy number. ..."
					+ " I promise in a future release not to be lazy... maybe.");
			if(_N<1000) {
				_N=1000;
			}
			_N = (int)(_N/200 * Math.ceil(_N/200));
			System.out.println("New number is: "+_N);
		}
		noInp = inp.getSize();
		size=_N;
		fullSize = _N + noInp;
		numExc = (int)(0.8*size);
		numInh = (int)(0.2*size);
		
		noSecs = _N/200;
		nodesPerSec = noSecs+1; // input
		
		// Generate locations...
		xyzCoors = new double[3][fullSize];
		double delta = DEFAULT_INP_WIDTH/Math.ceil(Math.sqrt(noInp));
		int x_inp = (int) Math.ceil(Math.sqrt(noInp));
		for(int ii=0; ii<inp.getSize(); ++ii) {
			xyzCoors[0][ii] = (ii/x_inp) * delta;
			xyzCoors[1][ii] = (ii%x_inp) * delta;
		}
		for(int ii=0; ii<size; ++ii) {
			xyzCoors[0][ii+noInp] = x0 + Math.random()*(xf-x0);
			xyzCoors[1][ii+noInp] = y0 + Math.random()*(yf-y0);
			xyzCoors[2][ii+noInp] = z0 + Math.random()*(zf-z0);
		}
		
		for(int ii=0; ii<noSecs; ++ii) {
			MANA_Node[] secNodes = new MANA_Node[noSecs];
			
			
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
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
	
	/**
	 * 
	 * @return
	 */
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
