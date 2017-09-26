package functions;

import data_holders.SynapseData;
import data_holders.SynapseData.SynType;

public class SynapseFunctions {
	
	public static enum STDPWindow {
		StandardHebb  {
			@Override
			public double windowVal(double s1, double s2, double wplus, double wminus, double delta_t, double lrate) {
				return standardWindow(s1, s2, wplus, wminus, delta_t, lrate);
			}
		}, MexHat {

			@Override
			public double windowVal(double s1, double s2, double wplus, double wminus, double delta_t, double lrate) {
				return mexicanHatWindow(s1, s2, wplus, wminus, delta_t, lrate);
			}
			
		};
		
		public abstract double windowVal(double s1, double s2, double wplus,
				double wminus, double delta_t, double lrate);
	}

//	public static void STDP(SynapseData dat, double post_time) {
//		
////		// Update weight
////		dat.setW(dat.getdW() * (time-dat.getLastUp()));
////		dat.setLastUp(time);
//		
//		double delta_t = post_time - dat.getLastArr();
//	
//		switch(dat.type) {
//		
//		case EE: standardWindow(dat, SynapseData.eeTauPlus,
//				SynapseData.eeTauMinus,
//				SynapseData.eeWPlus, SynapseData.eeWMinus, delta_t);
//		break;
//		case EI: standardWindow(dat, SynapseData.eiTauPlus,
//				SynapseData.eiTauMinus,
//				SynapseData.eiWPlus, SynapseData.eiWMinus, delta_t);
//		break;
//		case IE: mexicanHatWindow(dat, SynapseData.ieSigSq, SynapseData.ieNrmSq,
//				SynapseData.ieWPlus, SynapseData.ieWMinus, delta_t);
//		break;
//		case II: mexicanHatWindow(dat, SynapseData.iiSigSq, SynapseData.iiNrmSq,
//				SynapseData.iiWPlus, SynapseData.iiWMinus, delta_t);
//		break;
//				
//		default: throw new IllegalArgumentException("Not a valid synapse type identifier.");
//			
//		}
//		
//	}
	
	/**
	 * 
	 * @param type
	 * @param lastArrs
	 * @param dws
	 * @param post_time
	 * @param lrate
	 */
	public static void STDP(SynType type, double[] lastArrs, double[] dws, double post_time, double lrate) {
		STDPWindow win = type == SynType.EE||type==SynType.EI ?
				STDPWindow.StandardHebb : STDPWindow.MexHat;
		for(int ii=0, n=lastArrs.length; ii<n; ++ii) {
			double delta_t = post_time - lastArrs[ii];
			dws[ii] = win.windowVal(type.getDefaultShapeP1(),
					type.getDefaultShapeP2(), type.getDefaultWPlus(),
					type.getDefaultWMinus(), delta_t, lrate);
		}
	}
	
	/**
	 * 
	 * @param type
	 * @param lastArrs
	 * @param dws
	 * @param post_time
	 * @param lrate
	 */
	public static double STDP(SynType type, double lastArr, double post_time, double lrate) {
		STDPWindow win = type == SynType.EE||type==SynType.EI ?
				STDPWindow.StandardHebb : STDPWindow.MexHat;
			double delta_t = post_time - lastArr;
			return win.windowVal(type.getDefaultShapeP1(),
					type.getDefaultShapeP2(), type.getDefaultWPlus(),
					type.getDefaultWMinus(), delta_t, lrate);
	}
	
//	public static void postUpdate(double[] w, double[] dwLoc, double[] dw, 
//			double [] normPool, double[] normVal, boolean snon, double lrate,
//			double dt, double time, int tarIndex) 
//	{
////		for(int ii=0, n=w.length; ii<n; ++ii) {
////			dwLoc[ii] = dw[ii] * lrate * (time-lastUp[ii]); // update and store putative change
////		}
//		for(int ii=0, n=w.length; ii<n; ++ii) {
//			if (-dwLoc[ii] >= w[ii]) {
//				dwLoc[ii] = -w[ii];
//			}
//		}
//		for(int ii=0, n=w.length; ii<n; ++ii) {
//			normPool[tarIndex] += dwLoc[ii];
//		}
//	}
	
//	public static void postSTDP(SynapseData[] dat, double time) {
//		for(int ii=0,  n=dat.length; ii<n; ++ii) {
//			STDP(dat[ii], time);
//		}
//	}
	
	
//	public static void updateValue_Cnt(SynapseData dat, double time, double dt) {
//		dat.setW(dat.getdW()*dat.type.getLRate() * (time - dat.getLastUp()));
//		dat.setLastUp(time);
//	}
	
	public static void normalizeSingle(SynapseData dat, double scalF_new,
			double scalF_old, double normValBase, double accumChanges) {
		double newWt = (scalF_new * normValBase) * (dat.getW() 
				/ (accumChanges + (normValBase * scalF_old)));
		dat.setW(newWt);
	}
	
	public static double getPSR_UDF(SynapseData dat, double time) {
		double isi = dat.getLastArr() - time;
		
		dat.setdLittleU(dat.getBigU() + (dat.getLittleU() 
				* (1 - dat.getBigU()) * UtilFunctions.expLT0Approx(isi / dat.getF())));
		dat.setR( 1 + ((dat.getR() - (dat.getLittleU() * dat.getR()) - 1)
				* UtilFunctions.expLT0Approx(isi / dat.getD())));
		dat.setLastArr(time);
		
		return dat.getR() * dat.getW() * dat.getLittleU() * 10; // do something about magic number...
	}
	
	public static double standardWindow(double tauplus, double tauminus, double wplus,
			double wminus, double delta_t, double lrate) {
		double dw;
		if(delta_t >= 0) { // Pre fired before post
			dw=wplus * UtilFunctions.expLT0Approx(-delta_t/tauplus);
		} else {
			dw=-wminus * UtilFunctions.expLT0Approx(delta_t/tauminus);
		}
		return dw*lrate;
	}
	
	public static double mexicanHatWindow( double sigma, double normTerm, double wplus,
			double wminus, double delta_t, double lrate) {
		double dw = mexicanHatFunction(delta_t, sigma, normTerm);
		if(dw<0) {
			dw*=-wminus;
		} else {
			dw*=wplus;
		}
		return dw*lrate;
	}
	
	public static double mexicanHatFunction(double x, double sigmaSq, double normTerm) {
		double x_nrm_sq = (x*x)/sigmaSq;
		return normTerm * (1 - x_nrm_sq)*UtilFunctions.expLT0Approx(-0.5*x_nrm_sq);
	}
	
	public static void newNormScaleFactors(double[] scaleFacs,
			double[] thresholds, double[] threshBase, double rho, boolean exc) {
		if(exc) {
			for(int ii=0, n=scaleFacs.length; ii<n; ++ii) {
				scaleFacs[ii] = Math.exp((threshBase[ii]-thresholds[ii])/rho);
			}
		} else {
			for(int ii=0, n=scaleFacs.length; ii<n; ++ii) {
				scaleFacs[ii] = Math.exp((thresholds[ii]-threshBase[ii])/rho);
			}
		}
	}
	
}
