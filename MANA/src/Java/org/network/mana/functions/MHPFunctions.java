package Java.org.network.mana.functions;

import java.util.concurrent.ThreadLocalRandom;

import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.utils.BufferedFloatArray;
import Java.org.network.mana.utils.Utils;

public class MHPFunctions {

	public static double c_plus = 1;
	public static double c_minus = 1;

	public static void mhpStage1(final BufferedFloatArray efrsTar, final double[] pfrsTar,
								 final BufferedFloatArray efrsSrc, int tarNo, InterleavedSparseAddOn pfrLoc) {
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

	/**
	 * Executed in Nodes by worker threads
	 * 
	 * Notes: Re-zeros the local buffer vals before accumulating into them
	 * @param tarInd
	 * @param efrTar
	 * @param pfrTar
	 * @param efrSrc
	 * @param pfrLTDBuffLocal
	 * @param pfrLTPBuffLocal
	 * @param indices
	 */
	public static void metaHPStage1(int tarInd, double efrTar, double pfrTar,
			double[] efrSrc, double[] pfrLTDBuffLocal, double[] pfrLTPBuffLocal, int[] indices) {
		
			pfrLTDBuffLocal[tarInd] = 0;
			pfrLTPBuffLocal[tarInd] = 0;
			for(int ii=0, n = indices.length; ii<n; ++ii) {
				int srcInd = indices[ii];
				if(efrSrc[srcInd] <= efrTar) {
					pfrLTPBuffLocal[tarInd] += Utils.expLT0Approx((efrSrc[srcInd]-efrTar)/pfrTar);
				} else {
					pfrLTDBuffLocal[tarInd] += Utils.expLT0Approx((efrTar-efrSrc[srcInd])/pfrTar);
				}
			}
	}


	/**
	 * Executed in Sector by synchronizing worker thread
	 * @param pfrLTDBuffer
	 * @param pfrLTPBuffer
	 * @param neurons
	 * @param eta
	 * @param dt
	 */
	public static void metaHPStage2(double[] pfrLTDBuffer, double[] pfrLTPBuffer,
			MANANeurons neurons, double eta, double dt)
	{
		mhpLTPTerm(pfrLTPBuffer, neurons);
		mhpLTDTerm(pfrLTDBuffer, neurons);
		double ra = eta*dt;
		for(int ii=0, n=neurons.getSize(); ii<n; ++ii) {
			neurons.prefFR[ii] += ra *(pfrLTPBuffer[ii]+pfrLTDBuffer[ii]) 
					* (1+(ThreadLocalRandom.current().nextGaussian() * neurons.noiseVar));
			if(neurons.prefFR[ii] < 0.01) {
				neurons.prefFR[ii] = 0.01;
			}
		}
	}
	
	/**
	 * 
	 * @param pfrLTPBuffer
	 * @param neurons
	 */
	public static void mhpLTPTerm(double[] pfrLTPBuffer, MANANeurons neurons) {
		
		if(neurons.getSize() != pfrLTPBuffer.length) {
			throw new IllegalArgumentException("PrefFR/Buffer Dimension Mismatch");
		}
		//System.out.println("I happen");
		
		for(int ii=0, n=pfrLTPBuffer.length; ii<n; ++ii) {
			double ltpTerm = Math.exp(neurons.prefFR[ii]/(neurons.beta.get(ii)
					* neurons.lowFRBound.get(ii)));
			pfrLTPBuffer[ii] *= ltpTerm;
		}
		
	}

	/**
	 * 
	 * @param pfrLTDBuffer
	 * @param neurons
	 */
	public static void mhpLTDTerm(double[] pfrLTDBuffer, MANANeurons neurons) {
		if(neurons.getSize() != pfrLTDBuffer.length) {
			throw new IllegalArgumentException("PrefFR/Buffer Dimension Mismatch");
		}
		
		for(int ii=0, n=pfrLTDBuffer.length; ii<n; ++ii) {
			double ltdTerm;
			if(neurons.prefFR[ii] <= neurons.lowFRBound.get(ii)) {
				ltdTerm = -neurons.prefFR[ii]/neurons.lowFRBound.get(ii);
			} else {
				ltdTerm = 1 + (Math.log(1+(neurons.alpha.get(ii)*
						((neurons.prefFR[ii]/neurons.lowFRBound.get(ii)) - 1 )))
						/neurons.alpha.get(ii));
				ltdTerm *= -1;
			}
			pfrLTDBuffer[ii] *= ltdTerm;
		}
		
	}
	
	public static void newNormValsFromPfr(double [] normVals, double[] pfrs, MANANeurons neurons) {
		for(int ii=0, n=normVals.length; ii<n; ++ii) {
			normVals[ii] = neurons.sat_a/(1+Utils.expLT0Approx(-neurons.sat_b*pfrs[ii]))
					+ neurons.sat_c[ii];
		}
	}
	
}
