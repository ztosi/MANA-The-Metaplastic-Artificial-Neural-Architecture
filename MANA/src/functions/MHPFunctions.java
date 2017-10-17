package functions;

import java.util.concurrent.ThreadLocalRandom;

import base_components.MANANeurons;
import utils.Utils;

public class MHPFunctions {
	
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
		System.out.println("I happen");
		
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
