package nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import base_components.InputNeurons;
import base_components.MANANeurons;
import base_components.Neuron;
import base_components.SynapseData;
import base_components.SynapseData.SynType;
import utils.Utils;

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

	public List<Neuron> inputs = new ArrayList<Neuron>();
	
	public List<MANANeurons> targets = new ArrayList<MANANeurons>();
	
	public List<MANA_Node> nodes = new ArrayList<MANA_Node>();
	
	public InputNeurons externalInp;
	
	/**
	 * Creates an independent "MANA Unit" comprised of an experimenter-driven,
	 * dynamic-less input layer attached to a recurrent reservoir. The properties
	 * of the input are defined in a file and the size of the reservoir is specified.
	 * Otherwise this constructor uses ALL DEFAULT VALUES to create a MANA reservoir
	 * as described in Tosi, 2017. 
	 * TODO: Create a constructor where NOT everything is automatically default
	 * @param _inpFileName
	 * @param _N
	 */
	public MANA_Unit(final String _inpFileName, int _N) {
		InputNeurons inp = new InputNeurons(_inpFileName);
		inputs.add(inp);
		externalInp = inp;
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
		
		int nodD = numInh/200;
		noSecs = nodD*5;
		int secSize = _N/noSecs;
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
		
		int[] swappy = new int[noInp];
		for(int ii=0; ii<noInp; ++ii) {
			swappy[ii] = ii;
		}
		
		for(int ii=0; ii<noSecs; ++ii) {
			// Holds the xyz coordinate copies from the "big array" of all xyz across all neurons in all nodes in the unit
			double[] x = new double[secSize];
			double[] y = new double[secSize];
			double[] z = new double[secSize];
			System.arraycopy(xyzCoors[0], (ii*secSize+noInp), x, 0, secSize);
			System.arraycopy(xyzCoors[1], (ii*secSize+noInp), y, 0, secSize);
			System.arraycopy(xyzCoors[2], (ii*secSize+noInp), z, 0, secSize);
			// Our target neurons
			MANANeurons neus = new MANANeurons(secSize, ii>=noSecs/5, x, y, z);
			targets.add(neus);
			inputs.add(neus);
			
		}
		for(int ii=0; ii<noSecs; ++ii) {
			MANANeurons neus = targets.get(ii);
			SynType itype = SynType.getSynType(true, ii>=noSecs/5);
			// Neus is the current target
			int[][] conMap = new int[neus.getSize()][];
			double[][] dlys = null;
			double[][] weights = new double[neus.getSize()][];
			for(int jj=0, n=neus.getSize(); jj<n; ++jj) {
				int inD = 0;
				for(int kk=0, p=inputs.get(0).getSize(); kk<p; ++kk) {
					if(ThreadLocalRandom.current().nextDouble() < InputNeurons.def_con_prob) {
						int holder = swappy[inD];
						int swap = ThreadLocalRandom.current().nextInt(swappy.length);
						swappy[inD] = swappy[swap];
						swappy[swap] = holder;
						inD++;
					}
				}
				weights[jj] = Utils.getRandomArray(InputNeurons.def_pd,
						InputNeurons.def_mean, InputNeurons.def_std, inD);
				conMap[jj] = new int[inD];
				for(int kk=0; kk<inD; ++kk) {
					conMap[jj][kk] = swappy[kk];
				}
			}
			dlys = Utils.getDelays(inp.xyzCoors,
					neus.xyzCoors, getMaxDist(), SynapseData.MAX_DELAY, conMap);
			MANA_Node inpNode = new MANA_Node(inp, neus, itype, conMap, dlys, weights);
			nodes.add(inpNode);
			MANA_Node[] nPSec = new MANA_Node[nodesPerSec];
			nPSec[0] = inpNode;
			
			for(int jj=1; jj<inputs.size(); ++jj) {
				SynType rtype = SynType.getSynType(
						inputs.get(jj).isExcitatory(),
						targets.get(ii).isExcitatory());
				MANA_Node recN = new MANA_Node(inputs.get(jj), targets.get(ii),
						rtype, 
						Utils.getDelays(
								inputs.get(jj).getCoordinates(),
								targets.get(ii).getCoordinates(),
								targets.get(ii)==inputs.get(jj),
								getMaxDist(), SynapseData.MAX_DELAY),
						false);
				nPSec[jj] = recN;
				nodes.add(recN);
			}
			MANA_Sector sec = MANA_Sector.sector_builder(nPSec, targets.get(ii));
			sectors.add(sec);
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
