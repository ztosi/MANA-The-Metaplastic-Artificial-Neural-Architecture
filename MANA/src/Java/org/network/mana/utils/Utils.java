package Java.org.network.mana.utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import Java.org.network.mana.base_components.SynapseData;

public class Utils {

	public enum ProbDistType {
		NORMAL {
			@Override
			public double getRandom(double mean, double std) {
				return mean+(ThreadLocalRandom.current().nextGaussian()*std);
			}
		}, UNIFORM {
			@Override
			public double getRandom(double ceil, double floor) {
				return ThreadLocalRandom.current().nextDouble(ceil, floor);
			}
		}, LOGNORMAL {
			@Override
			public double getRandom(double location, double scale) {
				return Math.exp(location+(ThreadLocalRandom.current().nextGaussian()*scale));
			}
		};
		
		public abstract double getRandom(double a, double b);
	}
	
	public static double[] getRandomArray(ProbDistType pdt, double a, double b, int N) {
		double[] randArr = new double[N];
		for(int ii=0; ii<N; ++ii) {
			randArr[ii] = pdt.getRandom(a, b);
		}
		return randArr;
	}
	
	public static double expLT0Approx(final double x) {
//		if (x<-5) {
//			return 0;
//		}
//		if(x==0) {
//			return 1;
//		}
//		return (0.3877*x+1.959)/(x*x - 1.332 * x + 1.981);
		return Math.exp(x);
	}
	
	
	public static void sortByKey (final int[] key, final int[] arr) {
		if(key.length != arr.length) {
			throw new IllegalArgumentException("Key length does not match array length.");
		}
		int[] temp = new int[arr.length];
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			temp[ii] = arr[key[ii]];
		}
		System.arraycopy(temp, 0, arr, 0, arr.length);
	}
	
	// If only Java had nice generics for primitives and allowed creation of generic arrays...
	public static void sortByKey (final int[] key, final double[] arr) {
		if(key.length != arr.length) {
			throw new IllegalArgumentException("Key length does not match array length.");
		}
		double[] temp = new double[arr.length];
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			temp[ii] = arr[key[ii]];
		}
		System.arraycopy(temp, 0, arr, 0, arr.length);
	}
	
	public static void sortByKey (final int[] key, final SynapseData[] arr) {
		if(key.length != arr.length) {
			throw new IllegalArgumentException("Key length does not match array length.");
		}
		SynapseData[] temp = new SynapseData[arr.length];
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			temp[ii] = arr[key[ii]];
			temp[ii].index = ii;
		}
		System.arraycopy(temp, 0, arr, 0, arr.length);
	}
	
	public static void fillUsingDist(final ProbDistType dist,
			double a, double b, final double[][] vals) {
		for(int ii=0, n=vals.length; ii<n; ++ii) {
			for(int jj=0, m=vals[ii].length; jj<m; ++jj) {
				vals[ii][jj] = dist.getRandom(a, b);
			}
		}
	}
	
	public static double euclidean(double x1, double x2, double y1, double y2,
			double z1, double z2) {
		double xDiff = x2-x1;
		double yDiff = y2-y1;
		double zDiff = z2-z1;
		return Math.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff);
	}

	public static double euclidean(double [] p1, double [] p2) {
		return  euclidean(p1[0], p2[0], p1[1], p2[1], p1[2], p2[2]);
	}

	public static double euclidean(double[][] srcXYZ, double[][] tarXYZ,
			final int srcInd, final int tarInd) {
		return euclidean(srcXYZ[0][srcInd], tarXYZ[0][tarInd],
				srcXYZ[1][srcInd], tarXYZ[1][tarInd],
				srcXYZ[2][srcInd], tarXYZ[2][tarInd]);
	}
	
	public static double[][] getDelays(final double[][] xyz1, final double[][] xyz2,
			boolean rec, double maxDist, double maxDly) {
		double[][] dlys;
		if(rec) {
			dlys = new double[xyz2[0].length][xyz1[0].length-1];
		} else {
			dlys = new double[xyz2[0].length][xyz1[0].length];
		}
		int kk;
		for(int ii=0, n=xyz2[0].length; ii<n; ++ii) {
			kk=0;
			for(int jj=0, m=xyz1[0].length; jj<m; ++jj) {
				if(rec&&ii==jj) {
					continue;
				}
				dlys[ii][kk] = maxDly * euclidean(xyz1[0][kk], xyz2[0][ii],
						xyz1[1][kk], xyz2[1][ii], xyz1[2][kk], xyz2[2][ii])
						/ maxDist;
				++kk;
			}
		}
		return dlys;
	}

	public static double[] getUniformRandomArray(int n, double floor, double ceil) {
		double [] arr = new double[n];
		getUniformRandomArray(arr, floor, ceil);
		return arr;
	}

	public static double[] getGaussRandomArray(int n, double mean, double std) {
		double [] arr = new double[n];
		getGaussRandomArray(arr, mean, std);
		return arr;
	}


	public static void getUniformRandomArray(double[] arr, double floor, double ceil) {
		if (ceil <= floor || checkDoubleValidity(floor) != 0 || checkDoubleValidity(ceil) != 0) {
			// TODO forward specificity of the particlar reason it's invalid
			throw new IllegalArgumentException("Invalid floor or ceiling for uniform random.");
		}
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			// TODO forward specificity of the particlar reason it's invalid
			arr[ii] = (ceil-floor)*ThreadLocalRandom.current().nextDouble() + floor;
		}
	}

	public static void getGaussRandomArray(double[] arr, double mean, double std) {
		if (checkDoubleValidity(mean) != 0 || checkDoubleValidity(std) != 0) {
			throw new IllegalArgumentException("Invalid mean or standard dev. for gauss random.");
		}
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			arr[ii] = ThreadLocalRandom.current().nextGaussian()*std+mean;
		}
	}

	/**
	 *
	 * @param d
	 * @return 0 for valid, 1 for infinite, -1 for NaN
	 */
	public static int checkDoubleValidity(double d) {
		if (!Double.isFinite(d)) {
			return 1;
		}
		if (Double.isNaN(d)) {
			return -1;
		}
		return 0;
	}

	/**
	 * -1 if negative 1 otherwise (including if v is 0), returns -1 for -0
	 * @param v
	 * @return
	 */
	public static long sign(double v) {
		long d = Double.doubleToLongBits(v);
		d >>>= 63;
		return -(d*2-1);
	}

	public static <T> int [] getSortKey(List<T> thing, Comparator<T> sorter) {
		int [] sortKey = new int[thing.size()];
		List<Object[]> tmp = new ArrayList<>();
		Integer counter = 0;
		for(T telm : thing) {
			Object [] el = {telm, counter++};
			tmp.add(el);
		}
		Comparator<Object[]> compWrapper = (Object[] a, Object[] b) -> {
			return sorter.compare((T)a[0], (T)b[0]);
		};

		Collections.sort(tmp, compWrapper);

		counter = 0;
		for(Object[] oelm : tmp) {
			sortKey[counter++] = ((Integer) oelm[1]).intValue();
		}
		return sortKey;
	}

	public static double maxInt(int[] arr) {
		double mVal = Double.MIN_VALUE;
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			if(arr[ii] > mVal) {
				mVal = arr[ii];
			}
		}
		return mVal;
	}

	public static double[][] getDelays(final double[][] xyz1, final double[][] xyz2,
			double maxDist, double maxDly, int[][] conMap) {
		double[][] dlys = new double[xyz2.length][];
		for(int ii=0, n=xyz2.length; ii<n; ++ii) {
			dlys[ii] = new double[conMap[ii].length];
			for(int jj=0, m=conMap[ii].length; jj<m; ++jj) {
			int kk=conMap[ii][jj];
			dlys[ii][jj] = maxDly * euclidean(xyz1[0][kk], xyz2[0][ii],
					xyz1[1][kk], xyz2[1][ii], xyz1[2][kk], xyz2[2][ii])
					/ maxDist;
			}
		}
		return dlys;
	}

	public static double [] scalarMulti(double[] arr, double val) {
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			arr[ii] *= val;
		}
		return arr;
	}

	public static double [] scalarMult(double [] arr, double val) {
		double [] cpy = new double[arr.length];
		System.arraycopy(arr, 0, cpy, 0, arr.length);
		return scalarMulti(cpy, val);
	}

	public static double[] getDoubleArr(Collection<Double> doubleCol) {
		double[] out = new double[doubleCol.size()];
		Iterator<Double> dcIter = doubleCol.iterator();
		int ii=0;
		while(dcIter.hasNext()) {
			out[ii++] = dcIter.next();
		}
		return out;
	}
	
	public static double sum(double [] arr) {
		double su = 0;
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			su += arr[ii];
		}
		return su;
	}


	public static void addScalar(int [] arr, int scalar) {
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			arr[ii] += scalar;
		}
	}

	public static double[] intArr2Double(int[] src) {
		double[] dest = new double[src.length];
		for(int ii=0, n=src.length; ii<n; ++ii) {
			dest[ii] = (int) src[ii];
		}
		return dest;
	}

	public static void retainBounds(double[] arr, double ceil, double floor) {
		for(int ii=0, n=arr.length; ii<n; ++ii) {
			if(arr[ii] > ceil)
				arr[ii] = ceil;
			if(arr[ii] < floor)
				arr[ii] = floor;
		}
	}

	/**
	 * 0 if positive, 1 otherwise
	 * @param value
	 * @return
	 */
	public static int checkSign(double value) {
		return (int)(Double.doubleToLongBits(value) >>> 63);
	}


	public static void main(String [] args ) {
		double b = -5;
		double d = 5;

		System.out.println(Long.toBinaryString(Double.doubleToLongBits(b)));

		System.out.println(checkSign(b));
		System.out.println(checkSign(d));
		System.out.println(sign(-12.0));
		System.out.println(sign(12.0));
		System.out.println(sign(1E20));
		System.out.println(sign(0));
		System.out.println(sign(-0));
		System.out.println(sign(-10));
	}

}
