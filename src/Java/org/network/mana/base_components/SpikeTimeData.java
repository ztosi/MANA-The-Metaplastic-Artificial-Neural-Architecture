package Java.org.network.mana.base_components;

import Java.org.network.mana.utils.BoolArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Records spike times and can return them in formats which allow them to be reasonably written to files. Can be used
 * one of two ways
 */
public class SpikeTimeData {

	private static final int DEF_INIT_CAP = 1000;
	private static final double DEF_LOAD_FAC = 0.8;
	
	private final double[][] spkTimes;
	private final int[] ptrs;
	public final double loadFac;
	public final int initCap;
	public final int size;
	private double lastFlush = 0;
	public final List<BoolArray> buffer = new ArrayList<>();
	
	public SpikeTimeData(final int n) { // all default values...
		size = n;
		spkTimes = new double[n][];
		ptrs = new int[n];
		initCap = DEF_INIT_CAP;
		loadFac = DEF_LOAD_FAC;
		initSpkTimes();
	}

	/**
	 * Record spikes directly to a 2D array instead of the buffer
	 * @param spks
	 * @param time
	 */
	public void update(BoolArray spks, final double time) {
		for(int ii=0; ii<size;++ii) {
			if(spks.get(ii)) { // if the neuron spiked record the time at which it did...
				if(ptrs[ii] > spkTimes[ii].length * loadFac) { // If we're running out of space to hold calcSpikeResponses times, make more...
					double[] newTimes = new double[spkTimes[ii].length*2];
					System.arraycopy(spkTimes[ii], 0, newTimes, 0, spkTimes[ii].length);
					spkTimes[ii] = newTimes;
				} 
				// record the calcSpikeResponses time.
				spkTimes[ii][ptrs[ii]] = time;
				ptrs[ii]++;
			}
			
		}
	}

	/**
	 * Record spikes by pushing them to the buffer.
	 * @param spks
	 */
	public void pushSpks(BoolArray spks) {
		buffer.add(new BoolArray(spks));
	}



	/**
	 * Clears all recorded times and buffers, returns values as a list of array lists in ASDF format, presumably
	 * to be written to a .mat or some other file.
	 * @param time current simulation time
	 * @param dt simulation time step
	 * @return a list of array lists in ASDF
	 */
	public List<ArrayList<Double>> flushToASDFFormat(double time, double dt) {
		List<ArrayList<Double>> temp = new ArrayList<>();
		for(int jj=0; jj<size; ++jj) {
			temp.add(new ArrayList<>());
		}
		for(int ii=0, n = buffer.size(); ii < n; ++ii) {
			for(int jj=0; jj<size; ++jj) {
				if(buffer.get(ii).get(jj)) {
					temp.get(jj).add(lastFlush + ii*dt);
				}
			}
		}
		lastFlush = time;
		buffer.clear();
		return temp;
	}

	private double[] listDouble2DoubleArr(List<Double> list) {
		double [] ret = new double[list.size()];
		for(int ii=0, n = list.size(); ii < n; ++ii) {
			ret[ii] = list.get(ii).doubleValue();
		}
		return ret;
	}

	private final void initSpkTimes() {
		for(int ii=0; ii<size; ++ii) {
			ptrs[ii] = 0;
			spkTimes[ii] = new double[initCap]; 
		}
	}
	
	private final double[] getTrimmed(int ind) {
		double[] trimmed = new double[ptrs[ind]];
		System.arraycopy(spkTimes[ind], 0, trimmed, 0, ptrs[ind]);
		return trimmed;
	}
	
}
