package Java.org.network.mana.base_components;

import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.BufferedDoubleArray;
import Java.org.network.mana.utils.DataWrapper;

import java.util.Arrays;

public abstract class IFNeuron implements SpikingNeuron {

    public final int id;
    public final int N;
    public final boolean exc;

    public static final double default_i_bg = 18.5;
    public static final double default_leak_rev_mV = -70.6;

    /* Neuron Properties */
    public double [] v_m;
    public double [] dv_m;
    public double [] i_e;
    public double [] i_i;
    public DataWrapper i_bg;
    public volatile BufferedDoubleArray lastSpkTime;
    public volatile BoolArray spks;


    public int[] inDegree;
    public int[] excInDegree;
    public int[] inhInDegree;
    public int[] outDegree;
    public double [] exc_sf;
    public double [] inh_sf;

    public double [][] xyzCoors;


    /**
     * Creates MANA neurons of the specified polarity with default parameters.
     * @param _N size of group
     * @param _exc polarity (excitatory: true, inhibitory: false)
     */
    public IFNeuron(int _N, boolean _exc, double[] xCoor, double[] yCoor, double[] zCoor) {
        this(_N, _exc);
        if(xCoor != null &&
                yCoor != null &&
                zCoor != null) {
            for (int ii = 0; ii < _N; ++ii) {
                xyzCoors[ii][0] = xCoor[ii];
                xyzCoors[ii][1] = yCoor[ii];
                xyzCoors[ii][2] = zCoor[ii];
            }
        }
    }

    /**
     * Creates MANA neurons of the specified polarity with default parameters.
     * @param _N size of group
     * @param _exc polarity (excitatory: true, inhibitory: false)
     */
    public IFNeuron(int _N, boolean _exc) {
        id = MANA_Globals.getID();
        this.N = _N;
        this.exc = _exc;
        v_m = new double[N];
        dv_m = new double[N];
        lastSpkTime = new BufferedDoubleArray(N);
        spks = new BoolArray(N);
        inDegree = new int[N];
        excInDegree = new int[N];
        inhInDegree = new int[N];
        outDegree = new int[N];
        i_e = new double[N];
        i_i = new double[N];
        exc_sf = new double[N];
        Arrays.fill(exc_sf, 1);
        inh_sf = new double[N];
        Arrays.fill(inh_sf, 1);
        Arrays.fill(v_m, default_leak_rev_mV);
        i_bg = new DataWrapper(N, true, default_i_bg);
        xyzCoors=new double[_N][3];
    }
    @Override
    public BoolArray getSpikes() {
        return spks;
    }

    @Override
    public int[] getOutDegree() {
        return outDegree;
    }

    @Override
    public int getSize() {
        return N;
    }

    @Override
    public boolean isExcitatory(){
        return exc;
    }

    @Override
    public double[][] getCoordinates(boolean trans) {
        if (trans) {
            double[][] xyzCpy = new double[3][getSize()];
            for (int ii = 0; ii < N; ++ii) {
                xyzCpy[0][ii] = xyzCoors[ii][0];
                xyzCpy[1][ii] = xyzCoors[ii][1];
                xyzCpy[2][ii] = xyzCoors[ii][2];
            }
            return xyzCpy;
        } else {
            return  xyzCoors;
        }
    }
    @Override
    public void setCoor(int index, double x, double y, double z) {
        xyzCoors[index][0] = x;
        xyzCoors[index][1] = y;
        xyzCoors[index][2] = z;
    }

    @Override
    public void setCoor(int index, double[] xyz) {
        setCoor(index, xyz[0], xyz[1], xyz[2]);
    }


    public void setCoors(double[] xs, double[] ys, double[] zs){
        xyzCoors = new double[N][3];
        for(int ii=0; ii<xs.length; ++ii) {
            setCoor(ii, xs[ii], ys[ii], zs[ii]);
        }
    }

    @Override
    public int getID() {
        return id;
    }

}
