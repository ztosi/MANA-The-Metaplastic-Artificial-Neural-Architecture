package base_components;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Contains all data associated with synapses which undergo STDP and Short term plasticity.
 * The UDF parameters are held in an internal invidiual array, but the weight, dw, last arrival,
 * and last update time are aliased references to arrays containing all that data for a given 
 * synapse with respect to a particular post synaptic neuron in a given node.
 * @author zach
 *
 */
public final class SynapseData {
	
	private static final double PI_4TH_RT = Math.pow(Math.PI, 0.25);
	
	private static final double[] EEUDF = new double[]{0.5, 1100, 50};
	private static final double[] EIUDF = new double[]{0.05, 125, 1200};
	private static final double[] IEUDF = new double[]{0.25, 700, 20};
	private static final double[] IIUDF = new double[]{0.32, 144, 60};
	
	public static final double ExcTau = 3.0;
	public static final double InhTau = 6.0;
	
	public static final double eeTauPlus = 25;
	public static final double eeTauMinus = 100;
	public static final double eiTauPlus = 25;
	public static final double eiTauMinus = 100;
	public static final double ieSigma = 22;
	public static final double iiSigma = 12;
	
	public static final double eeWPlus = 5;
	public static final double eeWMinus = 1;
	public static final double eiWPlus = 5;
	public static final double eiWMinus = 1;
	public static final double ieWPlus = 1.8;
	public static final double ieWMinus = 1.8;
	public static final double iiWPlus = 1.2;
	public static final double iiWMinus = 2.2;
	
	public static final double ieNrmSq = 2.0/(Math.sqrt(3*ieSigma)*PI_4TH_RT);
	public static final double ieSigSq = ieSigma * ieSigma;
	public static final double iiNrmSq = 2.0/(Math.sqrt(3*iiSigma)*PI_4TH_RT);
	public static final double iiSigSq = iiSigma * iiSigma;
	
	public static final double E_LR = 1E-6;
	public static final double I_LR = 1E-6;
	
	
	public static final double MAX_WEIGHT= 20;
	public static final double DEF_NEW_WEIGHT = 0.01;
	public static final double MAX_DELAY= 15; //ms
	
	
	public enum SynType {
		EE {
			@Override
			public double[] getDefaultUDFMeans() {
				return EEUDF;
			}
			@Override
			public double getDefaultQTau() {
				return ExcTau;
			}
			@Override
			public boolean isExcitatory() {
				return true;
			}
			@Override
			public double getLRate() {
				return SynapseData.E_LR;
			}
			@Override
			public double getDefaultShapeP1() {
				return eeTauPlus;
			}
			@Override
			public double getDefaultShapeP2() {
				return eeTauMinus;
			}
			@Override
			public double getDefaultWPlus() {
				return eeWPlus;
			}
			@Override
			public double getDefaultWMinus() {
				return eeWMinus;
			}
			
		}, EI {

			@Override
			public double[] getDefaultUDFMeans() {
				return EIUDF;
			}
			@Override
			public double getDefaultQTau() {
				return ExcTau;
			}
			@Override
			public boolean isExcitatory() {
				return true;
			}
			@Override
			public double getLRate() {
				return SynapseData.E_LR;
			}
			@Override
			public double getDefaultShapeP1() {
				return eiTauPlus;
			}
			@Override
			public double getDefaultShapeP2() {
				return eiTauMinus;
			}
			@Override
			public double getDefaultWPlus() {
				return eiWPlus;
			}
			@Override
			public double getDefaultWMinus() {
				return eiWMinus;
			}
		}, IE {
			@Override
			public double[] getDefaultUDFMeans() {
				return IEUDF;
			}
			@Override
			public double getDefaultQTau() {
				return InhTau;
			}
			@Override
			public boolean isExcitatory() {
				return false;
			}
			@Override
			public double getLRate() {
				return SynapseData.I_LR;
			}
			@Override
			public double getDefaultShapeP1() {
				return ieSigSq;
			}
			@Override
			public double getDefaultShapeP2() {
				return ieNrmSq;
			}
			@Override
			public double getDefaultWPlus() {
				return ieWPlus;
			}
			@Override
			public double getDefaultWMinus() {
				return ieWMinus;
			}
		}, II {
			@Override
			public double[] getDefaultUDFMeans() {
				return IIUDF;
			}
			@Override
			public double getDefaultQTau() {
				return InhTau;
			}
			@Override
			public boolean isExcitatory() {
				return false;
			}
			@Override
			public double getLRate() {
				return SynapseData.I_LR;
			}
			@Override
			public double getDefaultShapeP1() {
				return iiSigSq;
			}
			@Override
			public double getDefaultShapeP2() {
				return iiNrmSq;
			}
			@Override
			public double getDefaultWPlus() {
				return iiWPlus;
			}
			@Override
			public double getDefaultWMinus() {
				return iiWMinus;
			}
		};
		
		public abstract double[] getDefaultUDFMeans();
		public abstract double getDefaultQTau();
		public abstract boolean isExcitatory();
		public abstract double getLRate();
		public abstract double getDefaultShapeP1();
		public abstract double getDefaultShapeP2();
		public abstract double getDefaultWPlus();
		public abstract double getDefaultWMinus();
		public static SynType getSynType(boolean srcExc, boolean tarExc) {
			if(srcExc) {
				if(tarExc){
					return SynType.EE;
				} else {
					return SynType.EI;
				}
			} else {
				if(tarExc) {
					return SynType.IE;
				} else {
					return SynType.II;
				}
			}
		}
	}
	
	public double[] w;
	public double[] dw;
	public double[] lastArr;
	/** Where weight, dw, and lastArr, data are actually stored in the parent arrays in the node*/
	public int index;
	private final double[] data = new double[5];
	public final SynType type;

	public SynapseData(final SynType _type, final double[] _w,
			final double[] _dw, final double[] _lastArr,
			final int _index) {
		this.type = _type;
		this.index = _index;
		this.w =_w; // Aliasing...
		this.dw = _dw; // Aliasing... and they said Java had no pointers, HA!
		//this.lastUp = _lastUp;
		this.lastArr = _lastArr; // Aliasing
		//this.normPool = _normPool;
		System.arraycopy(_type.getDefaultUDFMeans(), 0, data, 2, 3);
		data[1] = 1;
		data[2] = Math.abs(data[2] + (ThreadLocalRandom.current().nextGaussian()*data[2]/2.0));
		data[3] = Math.abs(data[3] + (ThreadLocalRandom.current().nextGaussian()*data[3]/2.0));
		data[4] = Math.abs(data[4] + (ThreadLocalRandom.current().nextGaussian()*data[4]/2.0));
	}

	public SynapseData(final SynapseData _orig, final int newIndex,
			final double[] _lastArr, final double[] _w, final double[] _dw) {
		this.type = _orig.type;
		this.index = newIndex;
		this.w = _w;
		this.dw = _dw;
		this.lastArr = _lastArr;
		System.arraycopy(_orig.data, 0, this.data, 0, this.data.length);
	}
	
	public double getdW() {
		return dw[index];
	}

	public double getW() {
		return w[index];
	}

//	public double getLastUp() {
//		return lastUp[index];
//	}

	public double getLastArr() {
		return lastArr[index];
	}

	public double getLittleU() {
		return data[0];
	}

	public double getR() {
		return data[1];
	}

	public double getBigU() {
		return data[2];
	}

	public double getD() {
		return data[3];
	}

	public double getF() {
		return data[4];
	}

	public void setdW(double _dw) {
		dw[index] = _dw; // Aliasing...
	}

	public void setW(double _w) {
		w[index] = _w; // Aliasing...
	}

//	public void setLastUp(double lu) {
//		lastUp[index] = lu;
//	}

	public void setLastArr(double la) {
		lastArr[index] = la;
	}

	public void setdLittleU(double u) {
		data[0] = u;
	}

	public void setR(double R) {
		data[1] = R;
	}

	public void setBigU(double U) {
		data[2] = U;
	}

	public void setD(double D) {
		data[3] = D;
	}

	public void setF(double F) {
		data[4] = F;
	}
}