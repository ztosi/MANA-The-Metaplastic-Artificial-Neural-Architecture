package Java.org.network.mana.mana_components;

import Java.org.network.mana.base_components.neurons.InputNeurons;
import Java.org.network.mana.base_components.neurons.Neuron;
import Java.org.network.mana.enums.ConnectRule;
import Java.org.network.mana.base_components.synapses.SynapseProperties;
import Java.org.network.mana.base_components.synapses.ConnectSpecs;
import Java.org.network.mana.utils.Utils;
import Java.org.network.mana.base_components.sparse.WeightData;

import java.util.*;

/**
 *
 * TODO: Currently just a holder for related sectors... eventually this will
 * contain Java.org.network.Java.org.network.exec.exec.functions for managing multiple "local" sectors as well as interfacing
 * with distant other units... can be thought of as roughly a cortical-column
 * equivalent sort of thing which also contains all incoming synaptic connections
 * (from anywhere) to that column. 
 *
 * @author Zoë Tosi
 *
 */
public class MANA_Unit {

	public static final int DEFAULT_NODE_DIM = 200;
	public static final double DEFAULT_BOUND_START = 50;
	public static final double DEFAULT_BOUND_END = 150;
	public static final double DEFAULT_EXC_RATIO = 0.8;

	public boolean synPlasticOn = true;
	public boolean mhpOn = true;

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

	private ConnectSpecs recConSpecs = new ConnectSpecs(ConnectRule.Distance2,
			new double[] {300, 300, 200, 200, 1},
			defMaxDist, SynapseProperties.MAX_DELAY);

	private ConnectSpecs inpConSpecs = new ConnectSpecs(ConnectRule.Random,
			new double[]{0.25}, Utils.ProbDistType.NORMAL, new double[]{3, 1},defMaxDist, SynapseProperties.MAX_DELAY);


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
	 * Otherwise this constructor uses ALL DEFAULT VALUES to create a MANA reservoir.
	 *
	 * Takes some desired number of neurons and an input file, creates a reasonable
	 * number of MANA_Nodes that can be updated concurrently and assigns them to sectors.
	 *
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
		// Figure out how much of everything thre needs to be...
		unit.noInp = inp.getSize();
		unit.size=_N;
		unit.fullSize = _N + unit.noInp;
		unit.numExc = (int)Math.ceil(DEFAULT_EXC_RATIO*unit.size);
		unit.numAllExc = unit.numExc + unit.noInp;
		unit.numInh = (int)Math.ceil((1-DEFAULT_EXC_RATIO)*unit.size);

		int numInhSecs = (int) Math.ceil(unit.numInh/DEFAULT_NODE_DIM);
		int [] inhSecSizes = new int[] {unit.numInh/numInhSecs,  unit.numInh%numInhSecs};

		int numExcSecs = (int) Math.ceil(unit.numExc/DEFAULT_NODE_DIM);
		int [] excSecSizes = new int[] {unit.numExc/numExcSecs,  unit.numInh%numExcSecs};

		for(int ii=0; ii<unit.fullSize; ++ii) {
			unit.allSpikes.add(new ArrayList<>());
		}

		unit.noSecs = numExcSecs + numInhSecs;
		unit.nodesPerSec = unit.noSecs+1; // input

		// Create the reservoir/MANA neurons
		for(int ii=0; ii< unit.noSecs; ++ii) {
			boolean exc = ii >= numInhSecs;
			// Placement in a 3D space handled by builder methods...
			MANANeurons neu = MANANeurons.buildFromLimits(exc ? excSecSizes[0] + excSecSizes[1]
							: inhSecSizes[0] + inhSecSizes[1], exc,
					unit.defXBounds,
					unit.defYBounds,
					unit.defZBounds);
			if(exc) {
				excSecSizes[1] = 0;
			} else {
				inhSecSizes[1] = 0;
			}
			MANA_Sector newSector = MANA_Sector.buildEmptySector(neu, unit);
			unit.sectors.put(newSector.id, newSector); // A place to put Java.org.network.exec.mana_components (what connects src neurons to these targets)
			unit.targets.add(neu);
		}

		// Populate the sectors with Java.org.network.exec.mana_components
		for(MANA_Sector tar : unit.sectors.values()) {
			// First node in each sector is always the node containing connections from the input
			MANA_Node inpN = tar.add(inp, unit.inpConSpecs);
			for(MANANeurons src : unit.targets) {
				// Use default connection specs to connect Java.org.network.exec.exec Java.org.network.exec.mana_components to each other (these are recurrent/reservoir synapses)
				//ConnectSpecs cSpecs = new ConnectSpecs(ConnectRule.Random,
				//		new double[]{0.8},
				//		unit.defMaxDist, SynapseData.MAX_DELAY);
				tar.add(src, unit.recConSpecs);
			}
			unit.nodes.addAll(tar.childNodes.values());
		}

		System.out.println("Created MANA Unit with:    " + unit.fullSize
				+ " neurons (including inputs) connected by:     " + unit.getTotalNNZ()
				+ " synapses, contained in    " + unit.nodes.size() + "Java/org/network/mana/mana_components");

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
					node.accumOutDegrees((node.srcData).getOutDegree());
				}
			}
		}
	}

	public void revalidateDegrees() {
		for(MANA_Sector sec : sectors.values()) {
			sec.recountInDegrees();
			for(MANA_Node node : sec.childNodes.values()) {
				if(node.srcData instanceof MANANeurons) {
					node.accumOutDegrees((node.srcData).getOutDegree());
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
	 * Returns the weight matrix represented by the Java.org.network.exec.mana_components of the collective source and target neurons
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

	private double maxExc = 0;
	private double maxInh = 0;
	private double lastExcTime = 0;
	private double lastInhTime = 0;

	public synchronized double getMaxExcLazy(double time) {
		if(Math.abs(lastExcTime - time) > 10) {
			maxExc = nodes.stream()
					.filter(node -> node.srcData.isExcitatory())
					.mapToDouble(node->node.getWeightMatrix().getMax(0))
					.max().getAsDouble();
			lastExcTime = time;
		}
		return maxExc;
	}

	public synchronized double getMaxInhLazy(double time) {
		if(Math.abs(lastInhTime - time) > 10) {
			maxInh = nodes.stream()
					.filter(node -> !node.srcData.isExcitatory())
					.mapToDouble(node->node.getSynMatrix().getMaxWeight())
					.max().getAsDouble();
			lastInhTime = time;
		}
		return maxInh;
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

	public List<ArrayList<Double>> getAllSpikes() {
		return  allSpikes;
	}

}