package Java.org.network.mana.base_components;

import Java.org.network.mana.base_components.enums.SynType;

import java.util.concurrent.ThreadLocalRandom;


/**
 * Contains all data associated with synapses which undergo STDP and Short term plasticity.
 * The UDF parameters are held in an internal invidiual array, but the weight, dw, last arrival,
 * and last update time are aliased references to arrays containing all that data for a given 
 * synapse with respect to a particular post synaptic neuron in a given node.
 * @author Zoe Tosi
 *
 */
public final class SynapseData {

	public static final double E_LR = 1E-5;
	public static final double I_LR = 1E-5;
	
	
	public static final double MAX_WEIGHT= 20;
	public static final double MIN_WEIGHT = 0.01;
	public static final double DEF_NEW_WEIGHT = 0.1;
	public static final double MAX_DELAY= 20; //ms
	
	public static final double DEF_INIT_WDERIV = 5E-6;

	
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