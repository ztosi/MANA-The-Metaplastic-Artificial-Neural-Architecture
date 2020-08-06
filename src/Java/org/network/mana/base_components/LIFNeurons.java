package Java.org.network.mana.base_components;

import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.*;

import java.util.Arrays;

public class LIFNeurons implements SpikingNeuron {

    public final int id;

    public static final double init_v_m = -55;
    public static final double init_thresh = -50;
    public static final double default_v_l = -70;
    public static final double default_r_m = 1.0;
    public static final double default_i_bg = 18.5;
    public static final double default_noiseVar = 0.2;

    public static final double default_exc_tau_m = 30;
    public static final double default_inh_tau_m = 20;
    public static final double default_exc_ref_p = 3;
    public static final double default_inh_ref_p = 2;

    /* Neuron Properties */
    public double [] v_m;
    public double [] dv_m;

    public double [] i_e;
    public double [] i_i;
    public volatile BufferedDoubleArray lastSpkTime;
    public volatile BoolArray spks;

    public DataWrapper thresh;
    public DataWrapper r_m;
    public DataWrapper tau_m;
    public DataWrapper v_l;
    public DataWrapper i_bg;
    public double ref_p;
    public DataWrapper v_reset;

    // Adaptation
    public DataWrapper tau_w;
    public double [] adapt;
    public double [][] xyzCoors;

    public final int N;
    public final boolean exc;
    public double adaptJump = 1;
    public int[] inDegree;
    public int[] excInDegree;
    public int[] inhInDegree;
    public int[] outDegree;

    /**
     * Creates MANA neurons of the specified polarity with default parameters.
     * @param _N size of group
     * @param _exc polarity (excitatory: true, inhibitory: false)
     */
    public LIFNeurons(int _N, boolean _exc, double[] xCoor, double[] yCoor, double[] zCoor) {
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
    public LIFNeurons(int _N, boolean _exc) {
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
        adapt = new double[N];
        thresh = new DataWrapper(N, true, init_thresh);
        r_m = new DataWrapper(N, true, default_r_m);
        v_l = new DataWrapper(N, true, default_v_l);
        i_bg = new DataWrapper(N, true, default_i_bg);
        v_reset = new DataWrapper(N, true, init_v_m);
        tau_w = new DataWrapper(Utils.getRandomArray(Utils.ProbDistType.UNIFORM, 10, 200, N));
        //tau_w = new DataWrapper(N, true, 144);

        if(exc) {
            ref_p = default_exc_ref_p;
//            tau_m = new DataWrapper(N, true, default_exc_tau_m);
			tau_m = new DataWrapper(Utils.getRandomArray(Utils.ProbDistType.NORMAL, 29, 3, N));
            adaptJump = 15;
        } else {
            ref_p = default_inh_ref_p;
//            tau_m = new DataWrapper(N, true, default_inh_tau_m);
			tau_m = new DataWrapper(Utils.getRandomArray(Utils.ProbDistType.NORMAL, 20, 3, N));
            adaptJump = 10;
        }
        Arrays.fill(v_m, init_v_m);
        xyzCoors=new double[_N][3];
    }


    @Override
    public void update(double dt, double time, BoolArray spkBuffer) {
        for(int ii=0; ii<N; ++ii) {
            int sgn = Utils.checkSign((lastSpkTime.getData(ii)+ref_p)-time);
//			dv_m[ii] += exc_sf[ii] * i_e[ii] + i_bg.get(ii) * sgn;
//			dv_m[ii] -= inh_sf[ii] * i_i[ii] * sgn;
            dv_m[ii] += i_e[ii] + i_bg.get(ii) * sgn;
            dv_m[ii] -= 5*i_i[ii] * sgn;
        }
        for(int ii=0; ii<N; ++ii) {
            dv_m[ii] -= adapt[ii];
        }
        for(int ii=0; ii<N; ++ii) {
            i_e[ii] -= dt * i_e[ii]/ SynType.ExcTau;

        }
        for(int ii=0; ii<N; ++ii) {
            i_i[ii] -= dt * i_i[ii]/SynType.InhTau;
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
            spkBuffer.set(ii, v_m[ii] >= thresh.get(ii) && (time > lastSpkTime.getData(ii)+ref_p));
        }

        for(int ii=0; ii<N; ++ii) {
            if(spkBuffer.get(ii)) {
                spikeAction(ii, time);
            }
        }
    }

    protected void spikeAction(int neuNo, double time) {
        lastSpkTime.setBuffer(neuNo, time);
        if(lastSpkTime.getBuffered(neuNo) - lastSpkTime.getData(neuNo) < ref_p) {
            throw new IllegalStateException("Refractory periods not being respected.");
        }
        v_m[neuNo] = v_reset.get(neuNo);
        adapt[neuNo] += adaptJump;
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

    public void setCoor(int index, double x, double y, double z) {
        xyzCoors[index][0] = x;
        xyzCoors[index][1] = y;
        xyzCoors[index][2] = z;
    }

    public void setCoor(int index, double[] xyz) {
        setCoor(index, xyz[0], xyz[1], xyz[2]);
    }

    @Override
    public int getID() {
        return id;
    }

//    public static class Dumb{
//         int bob = 1;
//         String sam = "sam";
//         public void doSomthing() {
//             likeThis();
//         }
//
//         protected void likeThis() {
//             bob++;
//         }
//
//    }
//
//    public static class Dumber extends  Dumb {
//        int sam = 2;
//        @Override
//        protected void likeThis() {
//            System.out.println("I'm being called");
//        }
//    }
//
//    public static void main(String[] args) {
//
//        Dumb d = new Dumb();
//        System.out.println(d.bob);
//        d.doSomthing();
//        System.out.println(d.bob);
//
//        Dumber dd = new Dumber();
//        System.out.println(dd.bob);
//        dd.doSomthing();
//        System.out.println(dd.bob);
//        System.out.println(dd.sam);
//        System.out.println(dd);
//
//
//    }

}
