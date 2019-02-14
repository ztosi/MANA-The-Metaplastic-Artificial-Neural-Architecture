package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.sparse.InterleavedSparseAddOn;
import Java.org.network.mana.utils.BufferedFloatArray;
import Java.org.network.mana.utils.Utils;

public class MHPFunctions {

	public static double c_plus = 1;
	public static double c_minus = 1;

	public static void mhpStage1(final BufferedFloatArray efrsTar, final double[] pfrsTar,
								 final BufferedFloatArray efrsSrc, int tarNo, InterleavedSparseAddOn pfrLoc, boolean exc) {
		int start = pfrLoc.getStartIndex(tarNo);
		int end = pfrLoc.getEndIndex(tarNo);
		int[] orderInds = pfrLoc.getRawOrdIndices();
		//int swapper = exc ? 1:-1;
		try {
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				pfrLoc.values[ii] = (efrsTar.getData(tarNo) - efrsSrc.getData(orderInds[ii])) / pfrsTar[tarNo];
			}
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				pfrLoc.values[ii] = Utils.sign(pfrLoc.values[ii]) * Math.exp(-Math.abs(pfrLoc.values[ii]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void mhpStage2(int tarNo, double f_p, double f_m, InterleavedSparseAddOn pfrLoc) {
		int start = pfrLoc.getStartIndex(tarNo);
		int end = pfrLoc.getEndIndex(tarNo);
		for(int ii=start; ii<end; ii+=pfrLoc.getInc()) {
			pfrLoc.values[ii] *= c_plus * f_p * Utils.checkSign(-pfrLoc.values[ii])
					+ c_minus * f_m * Utils.checkSign(pfrLoc.values[ii]);
		}
	}


	public static void calcfTerm(final double[] pfrs, final long[] fVals,
								 double alpha, double beta, double lowFR) {
		double blowf = beta * lowFR;
		for(int ii=0, n=pfrs.length; ii<n; ++ii) {
			fVals[ii] = Float.floatToIntBits((float)Math.exp(-pfrs[ii]/blowf));
			fVals[ii] <<= 32;
		}
		for(int ii=0, n=pfrs.length; ii<n; ++ii) {
			fVals[ii] |= (long) Float.floatToIntBits((float) mhpLTDTerm(pfrs[ii], alpha, lowFR));
		}
	}

	public static double mhpLTDTerm(double val, double alpha, double lowF) {
		if(val > lowF)
			return 1 + (Math.log(1 + alpha*(val/lowF - 1)))/alpha;
		else
			return val/lowF;
	}

	public static double mhpLTPTerm(double val, double beta, double lowF) {
		return Math.exp(-val/(beta*lowF));
	}

	public static double getFp(long datum) {
		return Float.intBitsToFloat((int)(datum>>>32));
	}

	public static double getFm(long datum) {

	    return Float.intBitsToFloat((int)(datum & 0x00000000ffffffff));
	}
	
}
