package Java.org.network.mana.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

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

	public void pushSpks(BoolArray spks) {
		buffer.add(new BoolArray(spks));
	}

	public void flush(String filename, String name, double time, double dt)
			throws IOException {

		MLCell asdf = new MLCell(name, new int[]{size+2, 1});
		asdf.set(new MLDouble("", new double[]{(double)size},1), size+1); // meta-data 1: # of neurons
		asdf.set(new MLDouble("", new double[]{time, dt}, 1), size+2); // meta-data 2: time and time-bin
		List<ArrayList<Double>> temp = flushToASDFFormat(time, dt);
		for(int ii=0; ii<size; ++ii) {
			asdf.set(new MLDouble("", listDouble2DoubleArr(temp.get(ii)), 1), ii);
		}
		new MatFileWriter(filename, Collections.singleton(asdf)); // write to file...

	}

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
	
	public static void flush(final List<SpikeTimeData> spktd,
			final String filename, final String name,
			final double time, final double dt) throws IOException {
		int totalSize = 0;
		for(int ii=0; ii<spktd.size(); ++ii) {
			totalSize += spktd.get(ii).size;
		}
		MLCell asdf = new MLCell(name, new int[]{totalSize+2, 1});
		asdf.set(new MLDouble("", new double[]{(double)totalSize},1), totalSize+1); // meta-data 1: # of neurons
		asdf.set(new MLDouble("", new double[]{time, dt}, 1), totalSize+2); // meta-data 2: time and time-bin
		int ind = 0;
		for(int ii=0; ii<spktd.size(); ++ii) {
			for(int jj=0; jj<spktd.get(ii).size; ++jj) {
				asdf.set(new MLDouble("", spktd.get(ii).getTrimmed(jj), 1), ind);
				ind++;
			}
			spktd.get(ii).initSpkTimes(); // reset data holders...
		}
			new MatFileWriter(filename, Collections.singleton(asdf)); // write to file...
	}
	
}
