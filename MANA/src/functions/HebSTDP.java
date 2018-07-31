package functions;

import base_components.SynMatDataAddOn;
import base_components.SynapseMatrix;

public final class HebSTDP implements STDP {

    private SynapseMatrix wts;

    private SynMatDataAddOn lastArrs;

    private double [] lastSpkTimes;

    public double tauPlus = 20;

    public double tauMinus = 100;

    public double wPlus = 5;

    public double wMinus = 1;

    public double lRate = 1E-6;

    private double [] values;

    public HebSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs,
                   final double [] lastSpkTimes) {
        this.wts = wts;
        this.lastArrs = lastArrs;
        this.lastSpkTimes = lastSpkTimes;
        values = wts.getValues();
    }

    public HebSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs,
                   final double [] lastSpkTimes,
                   double tauPlus, double tauMinus,
                   double wPlus, double wMinus, double lRate) {
        this(wts, lastArrs, lastSpkTimes);
        this.lRate = lRate;
        this.wMinus = wMinus;
        this.wPlus = wPlus;
        this.tauPlus = tauPlus;
        this.tauMinus = tauMinus;
    }

    public void reInit(SynapseMatrix wts, SynMatDataAddOn lastArrs) {
        this.wts = wts;
        this.lastArrs = lastArrs;
        values = wts.getRawData();
    }

    public void postTriggered(int neuNo, double time) {
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            values[ii+1] = lRate* wPlus;
        }
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            values[ii+1] *= Math.exp((lastArrs.values[laLoc]-time)/tauPlus);
            laLoc += lastArrs.getInc();
        }

    }

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    public void preTriggered(int tarNo, float[] dataPack) {
        values[(int)dataPack[1]*wts.getInc()+1] = -lRate * wMinus
                * Math.exp((lastSpkTimes[tarNo+wts.offsetMajor]-dataPack[0])/tauMinus);
    }


}
