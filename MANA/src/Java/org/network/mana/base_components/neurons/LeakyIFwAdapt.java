package Java.org.network.mana.base_components.neurons;

import Java.org.network.mana.base_components.synapses.SynapseProperties;
import Java.org.network.mana.exec.mana.MANA_Globals;
import Java.org.network.mana.utils.*;

import java.util.Arrays;

public class LeakyIFwAdapt implements  Neuron{

    public static final double init_v_m = -55;
    public static final double init_thresh = -50;
    public static final double default_v_l = -70;
    public static final double default_r_m = 1.0;
    public static final double default_i_bg = 18;
    public static final double default_exc_tau_m = 30;
    public static final double default_inh_tau_m = 20;
    public static final double default_exc_ref_p = 3;
    public static final double default_inh_ref_p = 2;
    public static final double default_inh_adatpJ = 10;
    public static final double default_exc_adaptJ = 15;

    /* Neuron Properties */
    public double [] v_m;
    public double [] dv_m;
    public double [] thresh;
    public double [] i_e;
    public double [] i_i;
    public volatile BufferedDoubleArray lastSpkTime;
    public volatile BoolArray spks;

    // Can be the same or different for all neurons
    public DataWrapper r_m;
    public DataWrapper tau_m;
    public DataWrapper v_l;
    public DataWrapper i_bg;
    public DataWrapper alpha;
    public DataWrapper beta;
    public DataWrapper lowFRBound;
    public double ref_p;
    public DataWrapper v_reset;

    // Adaptation
    public DataWrapper tau_w;
    public double [] adapt;
    public double adaptJump;

    public double [][] xyzCoors;

    public final int N;

    public final boolean exc;

    public final int id;

    private int[] outDegree;

    /**
     * Creates MANA neurons of the specified polarity with default parameters.
     * @param _N size of group
     * @param _exc polarity (excitatory: true, inhibitory: false)
     */
    public LeakyIFwAdapt(int _N, boolean _exc, double[] xCoor, double[] yCoor, double[] zCoor) {
        id = MANA_Globals.getID();
        this.N = _N;
        this.exc = _exc;

        v_m = new double[N];
        dv_m = new double[N];
        thresh = new double[N];
        outDegree = new int[N];

        lastSpkTime = new BufferedDoubleArray(N);
        spks = new BoolArray(N);

        i_e = new double[N];
        i_i = new double[N];
        adapt = new double[N];
        r_m = new DataWrapper(N, true, default_r_m);
        v_l = new DataWrapper(N, true, default_v_l);
        i_bg = new DataWrapper(N, true, default_i_bg);
        v_reset = new DataWrapper(N, true, init_v_m);
        tau_w = new DataWrapper(N, true, 144);

        if(exc) {
            ref_p = default_exc_ref_p;
            tau_m = new DataWrapper(N, true, default_exc_tau_m);
//			tau_m = new DataWrapper(Utils.getRandomArray(Utils.ProbDistType.NORMAL, 23, 1.5, N));
            adaptJump = 15;
        } else {
            ref_p = default_inh_ref_p;
            tau_m = new DataWrapper(N, true, default_inh_tau_m);
//			tau_m = new DataWrapper(Utils.getRandomArray(Utils.ProbDistType.NORMAL, 26, 2.5, N));
            adaptJump = 10;
        }


        Arrays.fill(thresh, init_thresh);
        Arrays.fill(v_m, init_v_m);
        xyzCoors=new double[_N][3];
        for(int ii=0; ii< _N; ++ii) {
            xyzCoors[ii][0] = xCoor[ii];
            xyzCoors[ii][1] = yCoor[ii];
            xyzCoors[ii][2] = zCoor[ii];
        }
    }

    /**
     * Updates the equations governing the neurons' memberane potentials
     * and adaptations and determines which neurons calcSpikeResponses on the next time-step
     * as a result, storing those in buffers.
     * @param dt
     * @param time
     * @param spkBuffer
     */
    @Override
    public void update(double dt, double time, BoolArray spkBuffer) {
        for(int ii=0; ii<N; ++ii) {
            int sgn = Utils.checkSign((lastSpkTime.getData(ii)+ref_p)-time);
            dv_m[ii] += i_e[ii] + i_bg.get(ii) * sgn;
            dv_m[ii] -= i_i[ii] * sgn;
        }
        for(int ii=0; ii<N; ++ii) {
            dv_m[ii] -= adapt[ii];
        }
        for(int ii=0; ii<N; ++ii) {
            i_e[ii] -= dt * i_e[ii]/ SynapseProperties.ExcTau;

        }
        for(int ii=0; ii<N; ++ii) {
            i_i[ii] -= dt * i_i[ii]/ SynapseProperties.InhTau;
        }
        if(!(r_m.isCompressed() && r_m.get(0)==1)){
            for(int ii=0; ii<N; ++ii) {
                dv_m[ii] *= r_m.get(ii);
            }
        }
        for(int ii=0; ii<N; ++ii) {
            dv_m[ii] += (v_l.get(ii)-v_m[ii]);
        }
        for(int ii=0; ii<N; ++ii) {
            dv_m[ii] *= dt/tau_m.get(ii);
        }

        for (int ii = 0; ii < N; ++ii) {
            v_m[ii] += dv_m[ii];
            if (Double.isNaN(v_m[ii])) {
                System.out.println(" NaN v");
                break;
            }
        }
        for(int ii=0; ii<N; ++ii) {
            adapt[ii] -= dt*adapt[ii]/tau_w.get(ii);
        }

        for(int ii=0; ii<N; ++ii) {
            spkBuffer.set(ii, v_m[ii] >= thresh[ii] && (time > lastSpkTime.getData(ii)+ref_p));
        }

        for(int ii=0; ii<N; ++ii) {
            if(spkBuffer.get(ii)) {
                lastSpkTime.setBuffer(ii, time);
                if(lastSpkTime.getBuffered(ii) - lastSpkTime.getData(ii) < ref_p) {
                    throw new IllegalStateException("Refractory periods not being respected.");
                }
                v_m[ii] = v_reset.get(ii);
                adapt[ii] += adaptJump;
            }
        }
    }

    public BoolArray getSpikes() {
        return spks;
    }

    public int getSize() {
        return  N;
    }

    public boolean isExcitatory() {
        return  exc;
    }

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

    public int getID() {
        return id;
    }

    public int [] getOutDegree() {
        return outDegree;
    }

}
