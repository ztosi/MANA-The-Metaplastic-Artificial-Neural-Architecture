package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.utils.BufferedFloatArray;
import Java.org.network.mana.utils.Utils;

import java.util.concurrent.ThreadLocalRandom;

public class MHPFunctions {

	public static double c_plus = 1;
	public static double c_minus = 1;

	public static final float alpha = 1.0f;
	public static final float beta = 0.1f;
	public static final float omega = 0.25f;
	public static final float zeta = 4f;

	public static final float sqrt_2 = (float) Math.sqrt(2);
	public static final float sqrt_2_beta = sqrt_2 * beta;
	public static final float dm = alpha/(sqrt_2 * beta);


	public static void mhpStochastic_1(final MANANeurons src, final MANANeurons tar, int tarNo,
									   final InterleavedSparseAddOn pfrLoc) {
		int start = pfrLoc.getStartIndex(tarNo);
		int end = pfrLoc.getEndIndex(tarNo);
		int[] orderInds = pfrLoc.getRawOrdIndices();
		BufferedFloatArray lgETar = tar.logEstFR;
		BufferedFloatArray lgESrc = src.logEstFR;
		float [] buffer = new float[(end-start)/pfrLoc.getInc()];
		try {
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				buffer[(ii-start)/pfrLoc.getInc()] = -(lgESrc.getData(orderInds[ii]) - lgETar.getData(tarNo));
			}
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				pfrLoc.values[ii] = Utils.erf_approx(buffer[(ii-start)/pfrLoc.getInc()] /sqrt_2_beta);
			}
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				pfrLoc.values[ii] = 0.5 * (pfrLoc.values[ii] + 1);
			}

			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				pfrLoc.values[ii] = ThreadLocalRandom.current().nextFloat() < pfrLoc.values[ii] ?
						ThreadLocalRandom.current().nextFloat() :
						-ThreadLocalRandom.current().nextFloat();
			}
			for (int ii = start; ii < end; ii += pfrLoc.getInc()) {
				float buff_val = buffer[(ii-start)/pfrLoc.getInc()];
				pfrLoc.values[ii] *= omega * (float)Math.exp(-zeta*buff_val*buff_val);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void mhpStochastic_2(final MANANeurons tar, final double[] dpfr) {
		float [] buffer = new float[tar.N];
		for(int ii = 0; ii<tar.N; ++ii) {
			buffer[ii] =  (alpha-tar.logEstFR.getData(ii))/sqrt_2_beta;
		}
		for(int ii = 0; ii<tar.N; ++ii) {
			buffer[ii] = (float)(0.5*(Utils.erf_approx(buffer[ii])+1));
		}
		for(int ii = 0; ii<tar.N; ++ii) {
			dpfr[ii] += (ThreadLocalRandom.current().nextFloat() < buffer[ii] ?
					ThreadLocalRandom.current().nextFloat()/2 :
					-ThreadLocalRandom.current().nextFloat())/2;
		}
	}

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
