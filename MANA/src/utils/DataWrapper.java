package utils;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A lightweight wrapper class for arrays that represent data, particularly 
 * parameters which can take on uniform values across a whole collection of
 * neurons or different values for each neuron and the variables that that they
 * interact with. DataWrapper allows the caller to mark the wrapper as 
 * compressible or not. If it is compressible and at any time all the elements
 * are set to the same value, then the data array is reallocated and made size
 * 1, containing the value. Basic arithmetic operations are associated with
 * the wrapper as each has contingencies for if the operand is scalar or vector
 * as well as if the calling wrapper operand is compressed or not. These 
 * contingencies make it worth while to make variables that interact
 * with other wrapped data also of this type, since the methods of this class
 * check if operands are of size one automatically.
 * 
 * Once a DataWrapper is designated not compressible it is no longer
 * compressible and the data array backing it will always remain full size.
 * Compressability is set at object creation and can only be turned off later.
 * 
 * 
 * It 
 * 
 * @author ZTosi
 *
 */
public final class DataWrapper {

	private double [] data;
	private boolean compressible;
	private boolean compressed;
	private final int length;
	
	/**
	 * 
	 * @param size
	 * @param compressible
	 */
	public DataWrapper(final int size, final boolean compressible) {
		this.compressible = compressible;
		length = size;
		if (compressible) {
			data = new double[]{0};
			compressed = true;
		} else {
			data = new double[size];
			compressed = false;
		}
	}
	
	/**
	 * 
	 * @param size
	 * @param compressible
	 * @param val
	 */
	public DataWrapper(final int size, final boolean compressible, final double val) {
		this.compressible = compressible;
		length = size;
		if (compressible) {
			data = new double[]{val};
			compressed = true;
		} else {
			data = new double[size];
			setAll(val);
		}
	}
	
	/**
	 * 
	 * @param vals
	 */
	public DataWrapper(final double[] vals) {
		this(vals.length, false);
		set(vals, 0, vals.length);
	}
	
	/**
	 * 
	 * @param size
	 * @param val
	 */
	public DataWrapper(final int size, final double val) {
		this(size, true);
		setAll(val);
	}
	
	public int size() {
		return length;
	}
	
	public int realSize() {
		return data.length;
	}
	
	/**
	 * 
	 * @param vals
	 * @param start
	 * @param stop
	 */
	public final void set(final double[] vals, final int start, final int stop)
	{
		if (compressed && stop != 1) {
			decompress();
		}
		
		if (vals.length == stop-start) {
			System.arraycopy(vals, 0, data, start, vals.length);
		} else if (vals.length == data.length) {
			System.arraycopy(vals, start, data, start, stop-start);
		} else {
			throw new IllegalArgumentException("Setter array dimension mismatch.");
		}
	}
	
	/**
	 * 
	 * @param vals
	 * @param start
	 * @param stop
	 */
	public final void set(final double vals, final int start, final int stop)
	{
		if (start==0 && stop == length) {
			setAll(vals);
		} else {
			if (!compressed && stop != 1) {
				Arrays.fill(data, start, stop, vals);
			} else {
				decompress();
				Arrays.fill(data, start, stop, vals);
			}
		}
	}
	
	/**
	 * 
	 * @param val
	 */
	public final void setAll(final double val) {
		if (compressible && !compressed) {
			data = new double[]{val};
			compressed = true;
		}
		Arrays.fill(data, 0, data.length, val);
	}
	
	/**
	 * 
	 * @param val
	 * @param index
	 */
	public final void setAt(final double val, int index)
	{
		if (compressed && index != 0) {
			decompress();
		}
		data[index] = val;
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public final double get(final int index) {
		if(compressed) {
			return data[0];
		} else {
			return data[index];
		}
	}
	
	/**
	 * 
	 * @param mean
	 * @param std
	 * @param start
	 * @param stop
	 */
	public final void randomize(
			final double mean,
			final double std,
			final int start,
			final int stop)
	{
		for(int ii=start; ii<stop; ++ii)
		{
			data[ii] = ThreadLocalRandom.current().nextGaussian()*std + mean;
		}
	}
	
	/**
	 * 
	 * @param mean
	 * @param std
	 * @param start
	 * @param stop
	 */
	public final void addNoise(
			final double mean,
			final double std,
			final int start,
			final int stop)
	{
		for(int ii=start; ii<stop; ++ii)
		{
			data[ii] += ThreadLocalRandom.current().nextGaussian()*std + mean;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public final double[] data() {
		return data;
	}
	
	public final double dataAt(final int index) {
		if (!compressed) {
			return data[index];
		} else {
			return data[0];
		}
	}
	
	
	/**
	 * 
	 * @return
	 */
	public final boolean isCompressed() {
		return compressed;
	}
	
	/**
	 * 
	 * @return
	 */
	public final boolean isCompressible() {
		return compressible;
	}
	
	/**
	 * 
	 */
	public final void makeUncompressible() {
		if (compressible) {
			decompress();
			compressible = false;
			
		}
	}
	
	public final void decompress() {
		if(!compressed) {
			return;
		} 
		double oVal = data[0];
		data = new double[length];
		Arrays.fill(data, oVal);
		compressed = false;
	}
	
	public final void add(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			add(result, val[0], start, stop);
			return;
		}
		if (!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] + val[ii];
			}
		} else {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[0] + val[ii];
			}
		}

	}
	
	public final void add(final double[] result, final double val, final int start, final int stop) {
		if (val == 0) return;
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] + val;
			}
		} else {
			double nval = val + data[0];
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}

	}
	
	public final void subtractFrom(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			subtractFrom(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = val[ii] - data[ii];
			}
		} else {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = val[ii] - data[0];
			}
		}

	}
	
	public final void subtract(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			subtract(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] - val[ii];
			}
		} else {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[0] - val[ii];
			}
		}

	}
	
	public final void subtractFrom(final double[] result, final double val, final int start, final int stop) {
		if (!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = val - data[ii];
			}
		} else {
			double nval = val - data[0];
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}

	}
	
	public final void subtract(final double[] result, final double val, final int start, final int stop) {
		if (val == 0) return;
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] - val;
			}
		} else {
			double nval = data[0] - val;
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}

	}
	
	public final void multiply(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			multiply(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[ii] * val[ii];
			}
		} else {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[0] * val[ii];
			}
		}

	}
	
	public final void multiply(final double[] result, final double val, final int start, final int stop) {
		if (val == 1) return;
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] * val;
			}
		} else {
			double nval = data[0] * val;
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}

	}
	
	public final void divideLeft(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			divideLeft(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] / val[ii];
			}
		} else {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[0] / val[ii];
			}
		}

	}
	
	public final void divideLeft(final double[] result, final double val, final int start, final int stop) {
		if (val==1) return;
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] / val;
			}
		} else {
			double nval = data[0] / val;
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}
	}
	
	public final void divideRight(final double[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			divideRight(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = val[ii] / data[ii];
			}
		} else {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = val[ii] / data[0];
			}
		}

	}
	
	public final void divideRight(final double[] result, final double val, final int start, final int stop) {
		if (!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = val / data[ii];
			}
		} else {
			double nval = val / data[0];
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}
	}
	
	public final void greaterThan(final boolean[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			greaterThan(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[ii] > val[ii];
			}
		} else {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[ii] > val[0];
			}
		}

	}
	
	public final void greaterThan(final boolean[] result, final double val, final int start, final int stop) {
		if (!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] > val;
			}
		} else {
			boolean nval = data[0] > val;
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}
	}
	
	public final void lessThan(final boolean[] result, final double[] val, final int start, final int stop) {
		if (val.length == 1) {
			greaterThan(result, val[0], start, stop);
			return;
		}
		if(!compressed) {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[ii] < val[ii];
			}
		} else {
			for(int ii=0; ii<stop; ++ii) {
				result[ii] = data[ii] < val[0];
			}
		}

	}
	
	public final void lessThan(final boolean[] result, final double val, final int start, final int stop) {
		if (!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = data[ii] < val;
			}
		} else {
			boolean nval = data[0] < val;
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = nval;
			}
		}
	}
	
	public final void sqrt(final double[] result, final int start, final int stop)
	{
		if(!compressed) {
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = Math.sqrt(data[ii]);
			}
		} else {
			double sqrt = Math.sqrt(data[0]);
			for(int ii=start; ii<stop; ++ii) {
				result[ii] = sqrt ;
			}
		}
	}
	
	public final void sqrt(final DataWrapper result, final int start, final int stop)
	{
		if (result.isCompressed()) {
			if(!compressed) {
				for(int ii=start; ii<stop; ++ii) {
					result.data[0] = Math.sqrt(data[ii]);
				}
			} else {
				result.data[0] = Math.sqrt(data[0]);
			}
		} else {
			sqrt(result.data, start, stop);
		}
		
	}
}
