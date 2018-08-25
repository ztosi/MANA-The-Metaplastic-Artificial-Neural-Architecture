package nodes;

import base_components.InputNeurons;
import base_components.MANANeurons;
import base_components.Neuron;
import base_components.SynapseData;
import base_components.enums.ConnectRule;
import base_components.enums.SynType;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import utils.ConnectSpecs;
import utils.SpikeTimeData;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
public class MANA_Unit2 {

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

	public double defMaxDist = Math.sqrt(
			Math.pow(x0-xf, 2) +
			Math.pow(y0-yf, 2) +
			Math.pow(z0-zf, 2));

	private int fullSize, size, numExc, numAllExc, numInh, noSecs, nodesPerSec, noInp;

	public double [][] xyzCoors;

	public List<MANA_Sector2> sectors = new ArrayList<MANA_Sector2>();

	public List<Neuron> inputs = new ArrayList<>();

	public List<MANANeurons> targets = new ArrayList<>();

	public List<MANA_Node2> nodes = new ArrayList<>();

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
	public static MANA_Unit2 MANABuilder(final String _inpFileName, int _N) {
		MANA_Unit2 unit = new MANA_Unit2();
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

		for(int ii=0; ii< unit.noSecs; ++ii) {
			MANANeurons neu = MANANeurons.buildFromLimits(secSize, ii < (unit.noSecs *0.8),
					new double[] {unit.x0, unit.xf},
					new double[] {unit.y0, unit.yf},
					new double[] {unit.z0, unit.zf});
			unit.sectors.add(MANA_Sector2.buildEmptySector(neu, unit));
			unit.targets.add(neu);
		}

		for(MANA_Sector2 tar : unit.sectors) {
			tar.add(inp, new ConnectSpecs(ConnectRule.AllToAll,
					new double[]{0}, unit.defMaxDist, SynapseData.MAX_DELAY));
			for(MANANeurons src : unit.targets) {
				ConnectSpecs cSpecs = new ConnectSpecs(ConnectRule.Distance,
						new double[]{SynType.getConProbBase(src.isExcitatory(), tar.target.isExcitatory())*4, unit.defMaxDist/4},
						unit.defMaxDist, SynapseData.MAX_DELAY);
				tar.add(src, cSpecs);
			}
		}

		return unit;
	}

	private MANA_Unit2() {

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


	public void printData(final String outDir, final String outPrefix, final double time) {
//		Map<String, double []> data = new HashMap<String, double[]>();
//		data.put("PrefFRs", new double[size]);
//		data.put("EstFRs", new double[size]);
//		data.put("Threshs", new double[size]);
//		data.put("NormBaseExc", new double[size]);
//		data.put("NormBaseInh", new double[size]);
//
//		int i_offset = 0;
//		for(MANA_Sector s : sectors) {
//			System.arraycopy(s.target.prefFR, 0, data.get("PrefFRs"),
//					i_offset, s.width);
//			System.arraycopy(s.target.estFR, 0, data.get("EstFRs"),
//					i_offset, s.width);
//			System.arraycopy(s.target.thresh, 0, data.get("Threshs"),
//					i_offset, s.width);
//			System.arraycopy(s.target.normValsExc, 0, data.get("NormBaseExc"),
//					i_offset, s.width);
//			System.arraycopy(s.target.normValsInh, 0, data.get("NormBaseInh"),
//					i_offset, s.width);
//			i_offset+=s.width;
//		}
//		List<MLArray> mlData = new ArrayList<MLArray>();
//		for(String key : data.keySet()) {
//			mlData.add(new MLDouble(key, data.get(key), 1));
//		}
//		mlData.add(new MLDouble("wtMat", getMatrix()));
//		try {
//			new MatFileWriter(outDir + File.separator + (int)time/1000 + "_" + outPrefix + ".mat", mlData);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}
	
}