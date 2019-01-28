package Java.org.network.mana.nodes;

import Java.org.network.mana.base_components.InputNeurons;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Neuron;
import Java.org.network.mana.base_components.SynapseData;
import Java.org.network.mana.base_components.enums.ConnectRule;
import Java.org.network.mana.base_components.enums.SynType;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt32;
import Java.org.network.mana.utils.ConnectSpecs;
import Java.org.network.mana.utils.Utils;
import Java.org.network.mana.utils.WeightData;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 
 * TODO: Currently just a holder for related sectors... eventually this will
 * contain Java.org.network.Java.org.network.mana.mana.functions for managing multiple "local" sectors as well as interfacing
 * with distant other units... can be thought of as roughly a cortical-column
 * equivalent sort of thing which also contains all incoming synaptic connections
 * (from anywhere) to that column. 
 * 
 * @author Zoë Tosi
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

	public double[] defXBounds = new double[]{x0, xf};

	public double[] defYBounds = new double[]{y0, yf};

	public double[] defZBounds = new double[]{z0, zf};

	public double defMaxDist = Math.sqrt(
			Math.pow(x0-xf, 2) +
			Math.pow(y0-yf, 2) +
			Math.pow(z0-zf, 2));

	private int fullSize, size, numExc, numAllExc, numInh, noSecs, nodesPerSec, noInp;

	private List<ArrayList<Double>> allSpikes = new ArrayList<>();

	public double [][] xyzCoors;

	public Map<String, MANA_Sector> sectors = new TreeMap<String, MANA_Sector>(
			(String a, String b) -> {
			int aint = Integer.parseInt(a.replaceAll("^[a-z]+", ""));
			int bint =  Integer.parseInt(b.replaceAll("^[a-z]+", ""));
			if(aint < bint) {
				return -1;
			} else if (aint > bint) {
				return  1;
			} else {
				return 0;
			}
	});

	public List<Neuron> inputs = new ArrayList<>();

	public List<MANANeurons> targets = new ArrayList<>();

	public List<MANA_Node> nodes = new ArrayList<>();

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
		InputNeurons inp = InputNeurons.buildInpNeuronsRandLocation(_inpFileName,
				unit.defXBounds, unit.defYBounds, unit.defZBounds);
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
		// Figure out how much of everything thre needs to be...
		unit.noInp = inp.getSize();
		unit.size=_N;
		unit.fullSize = _N + unit.noInp;
		unit.numExc = (int)(0.8*unit.size);
		unit.numAllExc = unit.numExc + unit.noInp;
		unit.numInh = (int)(0.2*unit.size);
		unit.noSecs = 5;
		int secSize = _N/unit.noSecs;
		unit.nodesPerSec = unit.noSecs+1; // input

        for(int ii=0; ii<unit.fullSize; ++ii) {
            unit.allSpikes.add(new ArrayList<>());
        }

		// Create the reservoir/MANA neurons
		for(int ii=0; ii< unit.noSecs; ++ii) {

			// Placement in a 3D space handled by builder methods...
			MANANeurons neu = MANANeurons.buildFromLimits(secSize, ii < (unit.noSecs * 0.8),
					unit.defXBounds,
					unit.defYBounds,
					unit.defZBounds);
			MANA_Sector newSector = MANA_Sector.buildEmptySector(neu, unit);
			unit.sectors.put(newSector.id, newSector); // A place to put Java.org.network.mana.nodes (what connects src neurons to these targets)
			unit.targets.add(neu);
		}

		// Populate the sectors with Java.org.network.mana.nodes
		for(MANA_Sector tar : unit.sectors.values()) {
			// First node in each sector is always the node containing connections from the input
			MANA_Node inpN = tar.add(inp, new ConnectSpecs(ConnectRule.Random,
					new double[]{0.25}, Utils.ProbDistType.NORMAL, new double[]{2,1}, unit.defMaxDist, SynapseData.MAX_DELAY));
			for(MANANeurons src : unit.targets) {
				// Use default connection specs to connect Java.org.network.mana.mana Java.org.network.mana.nodes to each other (these are recurrent/reservoir synapses)
				//ConnectSpecs cSpecs = new ConnectSpecs(ConnectRule.Random,
				//		new double[]{0.8},
				//		unit.defMaxDist, SynapseData.MAX_DELAY);
				ConnectSpecs cSpecs = new ConnectSpecs(ConnectRule.Distance2,
						//new double[] {2, unit.defMaxDist/2},
						new double[] {100},
						//new double[]{8*SynType.getConProbBase(src.isExcitatory(), tar.target.isExcitatory()), unit.defMaxDist/3},
						unit.defMaxDist, SynapseData.MAX_DELAY);
				tar.add(src, cSpecs);
			}
			unit.nodes.addAll(tar.childNodes.values());
		}

		System.out.println("Created MANA Unit with:    " + unit.fullSize
                + " neurons (including inputs) connected by:     " + unit.getTotalNNZ()
                + " synapses, contained in    " + unit.nodes.size() + "Java/org/network/mana/nodes");

		return unit;
	}



	private MANA_Unit() {

	}

	public int getTotalNNZ() {
        int nnzTotal = 0;
        for(MANA_Node node : nodes) {
            nnzTotal += node.getNNZ();
        }
        return nnzTotal;
    }

    public void initialize() {
		for(MANA_Sector sec : sectors.values()) {
			sec.init();
			for(MANA_Node node : sec.childNodes.values()) {
				if(node.srcData instanceof MANANeurons) {
					node.accumOutDegrees(((MANANeurons) node.srcData).outDegree);
				}
			}
		}
	}

	public void revalidateDegrees() {
		for(MANA_Sector sec : sectors.values()) {
			sec.recountInDegrees();
			for(MANA_Node node : sec.childNodes.values()) {
				if(node.srcData instanceof MANANeurons) {
					node.accumOutDegrees(((MANANeurons) node.srcData).outDegree);
				}
			}
		}
	}

	public int getNNZs() {
		int nnz = 0;
		for(MANA_Node node : nodes) {
			nnz += node.getNNZ();
		}
		return nnz;
	}

    /**
     * Returns the weight matrix represented by the Java.org.network.mana.nodes of the collective source and target neurons
     * @return
     */
	public WeightData getMatrix() {
        int nnzTotal = getTotalNNZ();

		WeightData wd = new WeightData(nnzTotal);

		int targOff = 0;
		int absShift = 0;
		for(String id : sectors.keySet()) {

			MANA_Sector sec = sectors.get(id);

			int srcOff = 0;
			for(Neuron n : sec.childNodes.keySet()) {

				MANA_Node node = sec.childNodes.get(n);
				int nnz = node.getNNZ();

				node.getWeightMatrix().getPtrsAsIndices(wd.tarInds, absShift, targOff);
				node.getWeightMatrix().getIndices(wd.srcInds, absShift, srcOff);
				node.getWeightValues(wd.values, absShift);

				if(!node.srcData.isExcitatory()) {
					for(int ii=0; ii < nnz; ++ii) {
						wd.values[ii+absShift] *= -1;
					}

				}

				srcOff += n.getSize();
				absShift += nnz;

			}
			targOff += sec.getWidth();
		}
		return  wd;
	}
//
//
//	public double [][] getPositions() {
//	    double[][] positions = new double[][]
//    }

    /**
     * Flushes recorded spike data from each sector into {@link #allSpikes} (which empties all the sector spike
     * data recorders). These are accumulated into allSpikes and previous data is left in tact there.
     * @param time
     * @param dt
     */
	public void collectSpikes(double time, double dt) {
	    int offset = 0;
	    for(String id : sectors.keySet()) {
	        MANA_Sector sec = sectors.get(id);
	        List<ArrayList<Double>> spks = sec.spkDat.flushToASDFFormat(time, dt);
	        for(int ii=0; ii<sec.getWidth(); ++ii) {
	            allSpikes.get(ii+offset).addAll(spks.get(ii));
            }
	        offset += sec.getWidth();
        }
    }

    /**
     * Prints relevant network data to a matlab .mat file which can be accessed with matlab.
     * @param outDir
     * @param outPrefix
     * @param time
     * @param dt
     */
	public void printData(final String outDir, final String outPrefix, final double time, final double dt) {
		Map<String, double []> data = new HashMap<>();
		data.put("PrefFRs", new double[size]);
		data.put("EstFRs", new double[size]);
		data.put("Threshs", new double[size]);
		data.put("NormBaseExc", new double[size]);
		data.put("NormBaseInh", new double[size]);
		data.put("x", new double[size]);
		data.put("y", new double[size]);
		data.put("z", new double[size]);
//        data.put("Positions", new double[size]);

		int i_offset = 0;
		for(String id : sectors.keySet()) {
			MANA_Sector s = sectors.get(id);
			System.arraycopy(s.target.prefFR, 0, data.get("PrefFRs"),
					i_offset, s.getWidth());
			s.target.estFR.copyTo(data.get("EstFRs"), i_offset);
			System.arraycopy(s.target.thresh, 0, data.get("Threshs"),
					i_offset, s.getWidth());
			System.arraycopy(s.target.normValsExc, 0, data.get("NormBaseExc"),
					i_offset, s.getWidth());
			System.arraycopy(s.target.normValsInh, 0, data.get("NormBaseInh"),
					i_offset, s.getWidth());
			System.arraycopy(s.target.getCoordinates(true)[0], 0, data.get("x"), i_offset, s.getWidth());
			System.arraycopy(s.target.getCoordinates(true)[1], 0, data.get("y"), i_offset, s.getWidth());
			System.arraycopy(s.target.getCoordinates(true)[2], 0, data.get("z"), i_offset, s.getWidth());
			i_offset+=s.getWidth();
		}
		List<MLArray> mlData = new ArrayList<MLArray>();
		for(String key : data.keySet()) {
			mlData.add(new MLDouble(key, data.get(key), 1));
		}
		WeightData wd = getMatrix();
        Utils.addScalar(wd.srcInds, 1);
        Utils.addScalar(wd.tarInds, 1);

        mlData.add(new MLInt32("srcInds", wd.srcInds, 1));
        mlData.add(new MLInt32("tarInds", wd.tarInds, 1));
		mlData.add(new MLDouble("wtValues", wd.values, 1));

        MLCell asdfCell = new MLCell("asdf", new int[]{fullSize+2, 1});
        collectSpikes(time, dt);
        for(int ii=0; ii<fullSize; ++ii) {
            asdfCell.set(new MLDouble("", Utils.getDoubleArr(allSpikes.get(ii)), 1), ii);
        }
        asdfCell.set(new MLDouble("", new double[]{dt}, 1), fullSize);
        asdfCell.set(new MLDouble("", new double[] {fullSize, time/dt}, 1), fullSize+1);

        mlData.add(asdfCell);
		try {
			new MatFileWriter(outDir + File.separator + (int)(time+dt)/1000 + "_" + outPrefix + ".mat", mlData);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

    public double getMaxDist() {
        return Math.sqrt(Math.pow(xf-x0, 2)+Math.pow(yf-y0, 2)+Math.pow(zf-z0, 2));
    }

    public void setMhpOn(boolean mhpOn) {
        this.mhpOn = mhpOn;
        for(MANA_Sector sec : sectors.values()) {
            sec.target.mhpOn = mhpOn;
        }

    }

    public void setSynPlasticOn(boolean synPlasticOn) {
        this.synPlasticOn = synPlasticOn;
        for(MANA_Node node : nodes) {
            node.synPlasticityOn = synPlasticOn;
        }
    }

    public void setNormalizationOn(boolean normalizationOn) {
        for(MANA_Node node : nodes) {
            node.normalizationOn = normalizationOn;
        }
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



}