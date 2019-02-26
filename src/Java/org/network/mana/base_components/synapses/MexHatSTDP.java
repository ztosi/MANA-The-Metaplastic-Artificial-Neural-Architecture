package Java.org.network.mana.base_components.synapses;

import Java.org.network.mana.base_components.sparse.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.sparse.InterleavedSparseMatrix;
import Java.org.network.mana.utils.BufferedDoubleArray;

import java.util.concurrent.ThreadLocalRandom;

public final class MexHatSTDP implements  STDP {

    public double wPlus = 1.8;

    public double wMinus = 1.8;

    private double sig = 22;

    private double sigSq;

    private double nrmTerm;

    public double lRate = 1E-3;

    public static double a = 25;

    public static final double twentRt = Math.pow(20, 1.0/4.0);

    public MexHatSTDP() {
        setSigma(sig);
    }

    public MexHatSTDP(double wPlus, double wMinus,
                      double sig, double lRate) {
        this.lRate = lRate;
        this.wMinus = wMinus;
        this.wPlus = wPlus;
        setSigma(sig);
    }

    // TODO: Break this up...
    public void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time, double dt){
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            if(wts.getRawData()[ii] > 20) {
                wts.getRawData()[ii] = 20;
            }
        }
        for (int ii = start; ii < end; ii += wts.getInc()) {
            wts.getRawData()[ii + 1] = dt * mexicanHatWindow(sigSq, nrmTerm,
                    wPlus, wMinus,
                    time - lastArrs.values[laLoc], lRate, wts.getRawData()[ii]);
                   // * ( 0.1 * ThreadLocalRandom.current().nextGaussian() + 1);
            laLoc += lastArrs.getInc();
        }
    }

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    public void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt) {

        int ind;
        if(dataPack[dataPack.length-1] == -1) {
            ind = wts.find(dataPack[3], dataPack[4]);
        } else {
            ind=dataPack[1];
        }
        if(wts.getRawData()[ind] > 20) {
            wts.getRawData()[ind] = 20;
        }
        wts.getRawData()[ind + 1] = dt * mexicanHatWindow(sigSq, nrmTerm,
                wPlus, wMinus, (dataPack[0] * dt) - lastSpkTimes.getData(dataPack[3]), lRate, wts.getRawData()[ind]);
    }

    public static double mexicanHatWindow(double sigmaSq, double normTerm, double wplus,
                                          double wminus, double delta_t, double lrate, double wt) {
        double dw = a * mexicanHatFunction(delta_t, sigmaSq, normTerm);
        if (dw < 0) {
            dw *= -wminus;
        } else {
            dw *= wplus;// * Math.pow(-wt+20, 0.25)/twentRt;
        }
        return dw * lrate;
    }

    public static double mexicanHatFunction(double x, double sigmaSq, double normTerm) {
        double x_nrm_sq = (x * x) / sigmaSq;
        return normTerm * (1 - x_nrm_sq) * Math.exp(-0.5 * x_nrm_sq);
    }

    public void setSigma(double sig) {
        this.sig = sig;
        sigSq = sig * sig;
        nrmTerm = 2/(Math.sqrt(3*sig) * Math.pow(Math.PI, 0.25));
    }

    public double getSigma() {
        return sig;
    }


    public static void main(String [] args) {
        double[] vals = new double[5000];

        vals[0] = -30;
        double sig = 22;
        double sigSq = sig * sig;
        double nrmTerm = 2 / (Math.sqrt(3 * sig) * Math.pow(Math.PI, 0.25));

        for (int ii = 1; ii < 5000; ++ii) {
            vals[ii] = vals[ii - 1] + 60.0 / 5000;
            System.out.print(vals[ii] + " ");
        }
        System.out.println();
        for (int ii = 1; ii < 5000; ++ii) {
            System.out.print(25 * mexicanHatFunction(vals[ii], sigSq, nrmTerm) + " ");

        }
    }

}