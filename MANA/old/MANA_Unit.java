package Java.org.network.mana.nodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import Java.org.network.mana.base_components.enums.SynType;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

import Java.org.network.mana.base_components.InputNeurons;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Neuron;
import Java.org.network.mana.base_components.SynapseData;
import Java.org.network.mana.utils.SpikeTimeData;
import Java.org.network.mana.utils.Utils;

/**
 * 
 * TODO: Currently just a holder for related sectors... eventually this will
 * contain Java.org.network.Java.org.network.mana.mana.functions for managing multiple "local" sectors as well as interfacing
 * with distant other units... can be thought of as roughly a cortical-column
 * equivalent sort of thing which also contains all incoming synaptic connections
 * (from anywhere) to that column. 
 * 
 * @author z
 *
 */
public class MANA_Unit {

	public static final double START_TIME = 20000;
	public static final int DEFAULT_NODE_DIM = 200;
	public static final double DEFAULT_BOUND_START = 50; 
	public static final double DEFAULT_BOUND_END = 150;
	public static final double DEFAULT_INP_WITDTH = 200;
	private static final int DEFAULT_INP_WIDTH = 0;

	public boolean synPlasticOn = true;
	public boolean mhpOn = true;
	public boolean hpOn = true;
	public boolean snOnAll = true;

	public double x0=DEFAULT_BOUND_START, xf=DEFAULT_BOUND_END,
			y0=DEFAULT_BOUND_START, yf=DEFAULT_BOUND_END,
			z0=DEFAULT_BOUND_START, zf=2*DEFAULT_BOUND_END;

	private int fullSize, size, numExc, numAllExc, numInh, noSecs, nodesPerSec, noInp;

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
	public static MANA_Unit MANABuilder(final String _inpFileName, int _N) {
		MANA_Unit unit = new MANA_Unit();
		InputNeurons inp = new InputNeurons(_inpFileName);
		unit.inputs.add(inp);
		unit.externalInp = inp;
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
		unit.noInp = inp.getSize();
		unit.size=_N;
		unit.fullSize = _N + unit.noInp;
		unit.numExc = (int)(0.8*unit.size);
		unit.numAllExc = unit.numExc + unit.noInp;
		unit.numInh = (int)(0.2*unit.size);

		int nodD = unit.numInh/200;
		unit.noSecs = nodD*5;
		int secSize = _N/unit.noSecs;
		unit.nodesPerSec = unit.noSecs+1; // input


		// Generate locations...
		unit.xyzCoors = new double[3][unit.fullSize];
		double delta = DEFAULT_INP_WIDTH/Math.ceil(Math.sqrt(unit.noInp));
		int x_inp = (int) Math.ceil(Math.sqrt(unit.noInp));
		for(int ii=0; ii<inp.getSize(); ++ii) {
			unit.xyzCoors[0][ii] = (ii/x_inp) * delta;
			unit.xyzCoors[1][ii] = (ii%x_inp) * delta;
		}


		for(int ii=0; ii<unit.size; ++ii) {
			unit.xyzCoors[0][ii+unit.noInp] = 
					unit.x0 + Math.random()*(unit.xf-unit.x0);
			unit.xyzCoors[1][ii+unit.noInp] = 
					unit.y0 + Math.random()*(unit.yf-unit.y0);
			unit.xyzCoors[2][ii+unit.noInp] =
					unit.z0 + Math.random()*(unit.zf-unit.z0);
		}

		int[] swappy = new int[unit.noInp];
		for(int ii=0; ii<unit.noInp; ++ii) {
			swappy[ii] = ii;
		}

		for(int ii=0; ii<unit.noSecs; ++ii) {
			// Holds the xyz coordinate copies from the "big array" of all xyz across all neurons in all Java.org.network.mana.nodes in the unit
			double[] x = new double[secSize];
			double[] y = new double[secSize];
			double[] z = new double[secSize];
			System.arraycopy(unit.xyzCoors[0], (ii*secSize+unit.noInp), x, 0, secSize);
			System.arraycopy(unit.xyzCoors[1], (ii*secSize+unit.noInp), y, 0, secSize);
			System.arraycopy(unit.xyzCoors[2], (ii*secSize+unit.noInp), z, 0, secSize);
			// Our target neurons
			MANANeurons neus = new MANANeurons(secSize, ii>=unit.noSecs/5, x, y, z);
			unit.targets.add(neus);
			unit.inputs.add(neus);

		}
		for(int ii=0; ii<unit.noSecs; ++ii) {
			MANANeurons neus = unit.targets.get(ii);
			SynType itype = SynType.getSynType(true, ii>=unit.noSecs/5);
			// Neus is the current target
			int[][] conMap = new int[neus.getSize()][];
			double[][] dlys = null;
			double[][] weights = new double[neus.getSize()][];
			for(int jj=0, n=neus.getSize(); jj<n; ++jj) {
				int inD = 0;
				for(int kk=0, p=unit.inputs.get(0).getSize(); kk<p; ++kk) {
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
				neus.sat_c[jj] = Utils.sum(weights[jj])-100; // TODO: magic number...
				conMap[jj] = new int[inD];
				for(int kk=0; kk<inD; ++kk) {
					conMap[jj][kk] = swappy[kk];
				}
			}

			dlys = Utils.getDelays(inp.xyzCoors,
					neus.xyzCoors, unit.getMaxDist(), SynapseData.MAX_DELAY, conMap);
			MANA_Node inpNode = new MANA_Node(inp, neus, itype, conMap, dlys, weights);
			unit.nodes.add(inpNode);
			MANA_Node[] nPSec = new MANA_Node[unit.nodesPerSec];
			nPSec[0] = inpNode;

			for(int jj=1; jj<unit.inputs.size(); ++jj) {
				SynType rtype = SynType.getSynType(
						unit.inputs.get(jj).isExcitatory(),
						unit.targets.get(ii).isExcitatory());
				MANA_Node recN = new MANA_Node(unit.inputs.get(jj), unit.targets.get(ii),
						rtype, 
						Utils.getDelays(
								unit.inputs.get(jj).getCoordinates(),
								unit.targets.get(ii).getCoordinates(),
								unit.targets.get(ii)==unit.inputs.get(jj),
								unit.getMaxDist(), SynapseData.MAX_DELAY),
						false);
				nPSec[jj] = recN;
				unit.nodes.add(recN);
			}
			MANA_Sector sec = MANA_Sector.sector_builder(nPSec, unit.targets.get(ii), unit);
			unit.sectors.add(sec);
		}
		return unit;
	}



	/**
	 * Creates an independent "MANA Unit" comprised of an experimenter-driven,
	 * dynamic-less input layer attached to a recurrent reservoir. The properties
	 * of the input are defined in a file and the size of the reservoir is specified.
	 * Otherwise this constructor uses ALL DEFAULT VALUES to create a MANA reservoir
	 * as described in Tosi, 2017.
	 * TODO: Create a constructor where NOT everything is automatically default
	 * @param _inpFileName
	 */
	public static MANA_Unit MANABuilder(final String _inpFileName, int numGroups, int nPerGroup) {

		return null;

	}

	private MANA_Unit() {

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
							max = node.weights[ii][jj];
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

	public double getMaxDist() {
		return Math.sqrt(Math.pow(xf-x0, 2)+Math.pow(yf-y0, 2)+Math.pow(zf-z0, 2));
	}
	
	private double maxDist = -1;
	public double getMaxDistFast() {
		if(maxDist < 0) {
			maxDist = getMaxDist();
		}
		return maxDist;
	}

	public int getNumAllExc() {
		return numAllExc;
	}

	public int getNumInh() {
		return numInh;
	}
	
	public int getSize() {
		return size;
	}
	
	public int getFullSize() {
		return fullSize;
	}
	
	/**
	 * 
	 * @return a list of the calcSpikeResponses time data (record of who spiked and when) across all member sectors.
	 */
	public List<SpikeTimeData> getSpkTimeData() {
		List<SpikeTimeData> spktd = new ArrayList<SpikeTimeData>();
		for(MANA_Sector s : sectors) {
			spktd.add(s.spkDat);
		}
		return spktd;
	}
	
	/**
	 * 
	 * @return
	 */
	public double[][] getMatrix() {
		int i_offset = 0;
		int j_offset = 0;
		double[][] mat = new double[size][fullSize];
		for(MANA_Sector s : sectors) {
			j_offset = 0;
			for(MANA_Node node : s.childNodes) {
				for(int ii=0; ii<node.width; ++ii) {
					for(int jj=0; jj<node.tarSrcMap[ii].length; ++jj) {
						int sign = node.isExcitatory() ? 1:-1;
						mat[ii+i_offset][node.tarSrcMap[ii][jj]+j_offset] =
								node.weights[ii][jj] * sign;
						
					}
				}
				j_offset += node.height;
			}
			i_offset += s.width;
		}
		return mat;
	}

	public void printData(final String outDir, final String outPrefix, final double time) {
		Map<String, double []> data = new HashMap<String, double[]>();
		data.put("PrefFRs", new double[size]);
		data.put("EstFRs", new double[size]);
		data.put("Threshs", new double[size]);
		data.put("NormBaseExc", new double[size]);
		data.put("NormBaseInh", new double[size]);
		
		int i_offset = 0;
		for(MANA_Sector s : sectors) {
			System.arraycopy(s.target.prefFR, 0, data.get("PrefFRs"),
					i_offset, s.width);
			System.arraycopy(s.target.estFR, 0, data.get("EstFRs"),
					i_offset, s.width);
			System.arraycopy(s.target.thresh, 0, data.get("Threshs"),
					i_offset, s.width);
			System.arraycopy(s.target.normValsExc, 0, data.get("NormBaseExc"),
					i_offset, s.width);
			System.arraycopy(s.target.normValsInh, 0, data.get("NormBaseInh"),
					i_offset, s.width);
			i_offset+=s.width;
		}
		List<MLArray> mlData = new ArrayList<MLArray>();
		for(String key : data.keySet()) {
			mlData.add(new MLDouble(key, data.get(key), 1));
		}
		mlData.add(new MLDouble("wtMat", getMatrix()));
		try {
			new MatFileWriter(outDir + File.separator + (int)time/1000 + "_" + outPrefix + ".mat", mlData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}