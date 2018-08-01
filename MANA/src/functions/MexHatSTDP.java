package functions;

import base_components.SynMatDataAddOn;
import base_components.Matrices.SynapseMatrix;
import utils.Utils;

public final class MexHatSTDP implements  STDP {

    private SynapseMatrix wts;

    private SynMatDataAddOn lastArrs;

    public double wPlus = 1.8;

    public double wMinus = 1.8;

    private double sig = 22;

    private double sigSq;

    private double nrmTerm;

    public double lRate = 1E-6;

    private double[] values;


    public MexHatSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs) {
        this.wts = wts;
        this.lastArrs = lastArrs;
        values = wts.getValues();
        setSigma(sig);
    }

    public MexHatSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs,
                      double wPlus, double wMinus,
                      double sig, double lRate) {
        this(wts, lastArrs);
        this.lRate = lRate;
        this.wMinus = wMinus;
        this.wPlus = wPlus;
        setSigma(sig);
    }

    public void reInit(SynapseMatrix wts, SynMatDataAddOn lastArrs) {
        this.wts = wts;
        this.lastArrs = lastArrs;
        values = wts.getValues();
    }

    // TODO: Break this up...
    public void postTriggered(int neuNo, double time) {
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);
        for (int ii = start; ii < end; ii += wts.getInc()) {
            values[ii + 1] = mexicanHatWindow(sigSq, nrmTerm,
                    wPlus, wMinus,
                    time - lastArrs.values[laLoc], lRate);
            laLoc += lastArrs.getInc();
        }
    }

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    public void preTriggered(int[] dataPack, double[] lastSpkTimes, double dt) {
        values[dataPack[1] * wts.getInc()+1] = mexicanHatWindow(sigSq, nrmTerm,
                wPlus, wMinus,
                (dataPack[0]*dt) - lastSpkTimes[dataPack[3]+wts.offsetMajor], lRate);
    }

    public static double mexicanHatWindow(double sigmaSq, double normTerm, double wplus,
                                          double wminus, double delta_t, double lrate) {
        double dw = mexicanHatFunction(delta_t, sigmaSq, normTerm);
        if (dw < 0) {
            dw *= -wminus;
        } else {
            dw *= wplus;
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