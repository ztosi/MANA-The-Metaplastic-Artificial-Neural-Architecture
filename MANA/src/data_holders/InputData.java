package data_holders;

import nodes.Syncable;

public class InputData implements Spiker, Syncable {
	
	public static double def_con_prob = 0.25;
	public static double def_mean = 3;
	public static double def_std = 1;
	
	private String filename;
	
	private double[][] spk_times;
	private int[] ptrs;
	private double[] offsets;
	public double[] lastSpkTime; // vanilla MANA does not use this for inputs, but someone might....
	public boolean[] spks;
	public int[] outDegree;
	double[][] xyzCoors;
	
	public InputData(final String _filename) {
		// TODO:
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
