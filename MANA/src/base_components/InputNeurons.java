package base_components;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

import utils.Syncable;
import utils.Utils;
import utils.Utils.ProbDistType;

public class InputNeurons implements Neuron, Syncable {

	public static final double def_con_prob = 0.25;
	public static final double def_mean = 3;
	public static final double def_std = 1;
	public static final ProbDistType def_pd = ProbDistType.NORMAL;

	private String filename;

	private double[][] spk_times;
	private int[] ptrs;
	private double[] offsets;
	public double[] lastSpkTime; // vanilla MANA does not use this for inputs, but someone might....
	public boolean[] spks;
	private int[] outDegree;
	public double[][] xyzCoors;

	public InputNeurons(final String _filename) {
		Scanner sc = null;
		try {
			int noNeu;
			if(_filename.contains(".mat")) {
				MatFileReader mfr = new MatFileReader(_filename);
				MLCell asdf = (MLCell) mfr.getContent().get("asdf");
				if(asdf == null) {
					throw new IOException("Cell array containing spike times in mat-file must be named \"asdf\"");
				}
				ArrayList<MLArray> mlSpkT = asdf.cells();
				noNeu = mlSpkT.size()-2;
				spk_times = new double[noNeu][];
				for(int ii=0; ii<noNeu; ++ii) {
					spk_times[ii] = new double[((MLDouble) mlSpkT.get(ii)).getSize()];
					double[][] temp = ((MLDouble) mlSpkT.get(ii)).getArray();
					for (int jj = 0; jj < spk_times[ii].length; ++jj){
						spk_times[ii][jj] = temp[jj][0];
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
			lastSpkTime = new double[noNeu];
			spks = new boolean[noNeu];
			outDegree = new int[noNeu];
			xyzCoors = new double[3][noNeu];
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
			System.err.println("asdf was not a cell... asdf format must use cell array. Alternatively, spike times must be doubles.");
			System.exit(0);
		} finally {
			if(sc!=null) {
				sc.close();
			}
		}
	}

	public void update(double dt, double time, boolean[] spkBuffer, double[] lastSpkTimeBuffer) {
		double edge2 = time+dt;
		for(int ii=0, n=spk_times.length; ii<n; ++ii) {
			if(ptrs[ii] >= spk_times[ii].length) {
				ptrs[ii] = 0;
				offsets[ii] += time; // start the cycle over again
			}
			double nextSpkTime = spk_times[ii][ptrs[ii]];
			spkBuffer[ii] = nextSpkTime >= time && nextSpkTime < edge2;
			if(spkBuffer[ii]) {
				++ptrs[ii];
				lastSpkTimeBuffer[ii] = nextSpkTime;
			}
		}
	}

	public void readInSpikeTimes(String _filename) {
		this.filename = _filename;

	}

	public boolean [] getSpikes() {
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
		return false;
	}

	@Override
	public double[][] getCoordinates() {
		return xyzCoors;
	}

}
