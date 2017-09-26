package functions;

import data_holders.SynapseData;

public class UtilFunctions {

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
}
