package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.Matrices.InterleavedSparseMatrix;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.utils.BufferedDoubleArray;
import Java.org.network.mana.utils.Utils;

public final class MexHatSTDP implements  STDP {

    public double wPlus = 1.8;

    public double wMinus = 1.8;

    private double sig = 22;

    private double sigSq;

    private double nrmTerm;

    public double lRate = 1E-3;

    public double a = 5;

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
    public void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time){
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);
        for (int ii = start; ii < end; ii += wts.getInc()) {
            wts.getRawData()[ii + 1] = mexicanHatWindow(sigSq, nrmTerm,
                    wPlus, wMinus,
                    time - lastArrs.values[laLoc], lRate, wts.getRawData()[ii]);
            laLoc += lastArrs.getInc();
        }
    }

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    public void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt) {
        wts.getRawData()[dataPack[1] + 1] = mexicanHatWindow(sigSq, nrmTerm,
                wPlus, wMinus,
                (dataPack[0]*dt) - lastSpkTimes.getData(dataPack[3]), lRate, wts.getRawData()[dataPack[1]]);
    }

    public static double mexicanHatWindow(double sigmaSq, double normTerm, double wplus,
                                          double wminus, double delta_t, double lrate, double wt) {
        double dw = 10 * mexicanHatFunction(delta_t, sigmaSq, normTerm);
        if (dw < 0) {
            dw *= -wminus;
        } else {
            dw *= wplus * Math.pow(-wt+20, 0.25)/twentRt;
        }
        return dw * lrate;
    }

    public static double mexicanHatFunction(double x, double sigmaSq, double normTerm) {
        double x_nrm_sq = (x * x) / sigmaSq;
        return normTerm * (1 - x_nrm_sq) * Utils.expLT0Approx(-0.5 * x_nrm_sq);
    }

    public void setSigma(double sig) {
        this.sig = sig;
        sigSq = sig * sig;
        nrmTerm = 2/(Math.sqrt(3*sig) * Math.pow(Math.PI, 0.25));
    }

    public double getSigma() {
        return sig;
    }

}