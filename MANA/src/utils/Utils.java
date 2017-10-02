package utils;

import java.util.concurrent.ThreadLocalRandom;

import base_components.SynapseData;

public class Utils {

	public static enum ProbDistType {
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
		if (x<-5) {
			return 0;
		}
		if(x==0) {
			return 1;
		}
		return (0.3877*x+1.959)/(x*x - 1.332 * x + 1.981);
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
			dlys = new double[xyz2.length][xyz1.length-1];
		} else {
			dlys = new double[xyz2.length][xyz1.length];
		}
		int kk;
		for(int ii=0, n=xyz2.length; ii<n; ++ii) {
			kk=0;
			for(int jj=0, m=xyz1.length; jj<m; ++jj) {
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
	
}
