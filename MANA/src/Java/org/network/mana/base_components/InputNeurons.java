package Java.org.network.mana.base_components;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.BufferedDoubleArray;
import Java.org.network.mana.utils.Syncable;
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
	private int[] outDegree;
	public double[][] xyzCoors;

	public static InputNeurons buildInpNeuronsRandLocation (final String _filename,
															   double[] xlims,
															   double[] ylims,
															   double [] zlims) {
		InputNeurons inNeu = new InputNeurons(_filename);
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

	public static  InputNeurons buildInpNeuronsFromLocations (final String _filename,
															  double[] xCoors,
															  double[] yCoors,
															  double[] zCoors) {
		InputNeurons inNeu = new InputNeurons(_filename);
		inNeu.xyzCoors = new double[inNeu.getSize()][3];
		for (int ii = 0; ii < inNeu.getSize(); ++ii) {
			inNeu.xyzCoors[ii][0] = xCoors[ii];
			inNeu.xyzCoors[ii][1] = yCoors[ii];
			inNeu.xyzCoors[ii][2] = zCoors[ii];
		}
		return inNeu;
	}


	private InputNeurons(String _filename) {
		id = MANA_Globals.getID();
		Scanner sc = null;
		try {
			int noNeu;
			if(_filename.contains(".mat")) {
				MatFileReader mfr = new MatFileReader(_filename);
				MLCell asdf = (MLCell) mfr.getContent().get("asdf");
				if(asdf == null) {
					throw new IOException("Cell array containing calcSpikeResponses times in mat-file must be named \"asdf\"");
				}
				ArrayList<MLArray> mlSpkT = asdf.cells();
				noNeu = mlSpkT.size()-2;
				spk_times = new double[noNeu][];
				for(int ii=0; ii<noNeu; ++ii) {
					spk_times[ii] = new double[((MLDouble) mlSpkT.get(ii)).getSize()];
					double[][] temp = ((MLDouble) mlSpkT.get(ii)).getArray();
					for (int jj = 0; jj < spk_times[ii].length; ++jj){
						spk_times[ii][jj] = temp[0][jj];
					}
				}
			} else {
				sc = new Scanner(new FileReader(_filename));
				noNeu = sc.nextInt();
				spk_times = new double[noNeu][];
				for(int ii=0; ii<noNeu; ++ii) {
					Scanner lineSc = new Scanner(sc.nextLine());
					ArrayList<Double> times = new ArrayList<Double>();
					while(lineSc.hasNext()) {
						times.add(lineSc.nextDouble());
					}
					spk_times[ii] = Utils.getDoubleArr(times);
					lineSc.close();
				}
			}
			ptrs = new int[noNeu];
			offsets = new double[noNeu];
			lastSpkTime = new BufferedDoubleArray(noNeu);
			spks = new BoolArray(noNeu);
			outDegree = new int[noNeu];
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Could not fine input file... exiting...");
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Exiting...");
			System.exit(0);
		} catch (ClassCastException e) {
			e.printStackTrace();
			System.err.println("asdf was not a cell... asdf format must use cell array. Alternatively, calcSpikeResponses times must be doubles.");
			System.exit(0);
		} finally {
			if(sc!=null) {
				sc.close();
			}
		}
	}


	public void update(double dt, double time, BoolArray spkBuffer) {
		double edge2 = time+dt;
		for(int ii=0, n=spk_times.length; ii<n; ++ii) {
			if(ptrs[ii] >= spk_times[ii].length) {
				ptrs[ii] = 0;
				offsets[ii] += time; // start the cycle over again
			}
			double nextSpkTime = spk_times[ii][ptrs[ii]];
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

	public BoolArray getSpikes() {
		return spks;
	}

	public int getSize() {
		return spk_times.length;
	}

	@Override
	public int[] getOutDegree() {
		return outDegree;
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

}
