package Java.org.network.mana.base_components.neurons;

import Java.org.network.mana.exec.Syncable;
import Java.org.network.mana.globals.Default_Parameters;
import Java.org.network.mana.io.InputReader;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.BufferedDoubleArray;
import Java.org.network.mana.utils.Utils;
import Java.org.network.mana.utils.Utils.ProbDistType;

public class InputNeurons implements Neuron, Syncable {

	public static final double def_con_prob = 0.25;
	public static final double def_mean = 3;
	public static final double def_std = 1;
	public static final ProbDistType def_pd = ProbDistType.NORMAL;

	public final int id;
	private String filename;

	private double[][] spk_times;
	private int[] ptrs;
	private double[] offsets;
	public BufferedDoubleArray lastSpkTime; // vanilla MANA does not use this for inputs, but someone might....
	public BoolArray spks;
	public double[][] xyzCoors;
	public int [] outDegree;

	public static InputNeurons buildInpNeuronsRandLocation (final String _filename,
															   double[] xlims,
															   double[] ylims,
															   double [] zlims) {
		InputNeurons inNeu = new InputNeurons();
		InputReader.readInputs(inNeu, _filename);
		double[] xCoors = Utils.getRandomArray(ProbDistType.UNIFORM, xlims[0], xlims[1], inNeu.getSize());
		double[] yCoors = Utils.getRandomArray(ProbDistType.UNIFORM, ylims[0], ylims[1], inNeu.getSize());
		double[] zCoors = Utils.getRandomArray(ProbDistType.UNIFORM, zlims[0], zlims[1], inNeu.getSize());
		inNeu.xyzCoors = new double[inNeu.getSize()][3];
		for (int ii = 0; ii < inNeu.getSize(); ++ii) {
			inNeu.xyzCoors[ii][0] = xCoors[ii];
			inNeu.xyzCoors[ii][1] = yCoors[ii];
			inNeu.xyzCoors[ii][2] = zCoors[ii];
		}
		return inNeu;

	}

	public static InputNeurons buildInpNeuronsFromLocations (final String _filename,
															  double[] xCoors,
															  double[] yCoors,
															  double[] zCoors) {
		InputNeurons inNeu = new InputNeurons();
		InputReader.readInputs(inNeu, _filename);
		inNeu.xyzCoors = new double[inNeu.getSize()][3];
		for (int ii = 0; ii < inNeu.getSize(); ++ii) {
			inNeu.xyzCoors[ii][0] = xCoors[ii];
			inNeu.xyzCoors[ii][1] = yCoors[ii];
			inNeu.xyzCoors[ii][2] = zCoors[ii];
		}
		return inNeu;
	}

	public static InputNeurons buildInpNeurons (final double[][] spk_times,
															 double[] xCoors,
															 double[] yCoors,
															 double[] zCoors) {
		InputNeurons inNeu = new InputNeurons();
		inNeu.init(spk_times.length);
		for(int ii=0, n = spk_times.length; ii<n; ++ii) {
			inNeu.spk_times[ii] = new double[spk_times[ii].length];
			System.arraycopy(spk_times[ii], 0, inNeu.spk_times[ii], 0, spk_times[ii].length);
		}
		inNeu.xyzCoors = new double[inNeu.getSize()][3];
		for (int ii = 0; ii < inNeu.getSize(); ++ii) {
			inNeu.xyzCoors[ii][0] = xCoors[ii];
			inNeu.xyzCoors[ii][1] = yCoors[ii];
			inNeu.xyzCoors[ii][2] = zCoors[ii];
		}
		return inNeu;
	}

	private InputNeurons() {
		id = Default_Parameters.getID();
	}

	public void init(int noNeu) {
		spk_times = new double[noNeu][];
		ptrs = new int[noNeu];
		offsets = new double[noNeu];
		lastSpkTime = new BufferedDoubleArray(noNeu);
		spks = new BoolArray(noNeu);
		outDegree = new int[noNeu];
	}

	public void update(double dt, double time, BoolArray spkBuffer) {
		double edge2 = time+1.5*dt;
		for(int ii=0, n=spk_times.length; ii<n; ++ii) {
			if(ptrs[ii] >= spk_times[ii].length) {
				ptrs[ii] = 0;
				offsets[ii] += time; // start the cycle over again
			}
			double nextSpkTime = spk_times[ii][ptrs[ii]] + offsets[ii];
			spkBuffer.set(ii, nextSpkTime >= time && nextSpkTime < edge2);
			if(spkBuffer.get(ii)) {
				++ptrs[ii];
				lastSpkTime.setBuffer(ii, nextSpkTime);
			}
		}
	}

	public void readInSpikeTimes(String _filename) {
		this.filename = _filename;

	}

	public double[][] getSpk_times() {
		return spk_times;
	}

	public BoolArray getSpikes() {
		return spks;
	}

	public int getSize() {
		return spk_times.length;
	}

	@Override
	public boolean isExcitatory(){
		return true;
	}

	@Override
	public double[][] getCoordinates(boolean trans) {
		if (trans) {
			double[][] xyzCpy = new double[3][getSize()];
			for (int ii = 0; ii < getSize(); ++ii) {
				xyzCpy[0][ii] = xyzCoors[ii][0];
				xyzCpy[1][ii] = xyzCoors[ii][1];
				xyzCpy[2][ii] = xyzCoors[ii][2];
			}
			return xyzCpy;
		} else {
			return  xyzCoors;
		}
	}

	@Override
	public int getID() {
		return id;
	}

	public int[] getOutDegree() {
		return  outDegree;
	}

}
