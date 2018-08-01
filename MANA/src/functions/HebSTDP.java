package functions;

import base_components.SynMatDataAddOn;
import base_components.Matrices.SynapseMatrix;

public final class HebSTDP implements STDP {

    private SynapseMatrix wts; // TODO: Make this a MANANode and have it access it through that... or pass it as an arg...

    private SynMatDataAddOn lastArrs;

    public double tauPlus = 20;

    public double tauMinus = 100;

    public double wPlus = 5;

    public double wMinus = 1;

    public double lRate = 1E-6;

    private double [] values;

    public HebSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs) {
        this.wts = wts;
        this.lastArrs = lastArrs;
        values = wts.getValues();
    }

    public HebSTDP(SynapseMatrix wts, SynMatDataAddOn lastArrs,
                   final double [] lastSpkTimes,
                   double tauPlus, double tauMinus,
                   double wPlus, double wMinus, double lRate) {
        this(wts, lastArrs);
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

    // data pack is {arrTime, rel tar ind, udfMultiplier, abs tar ind}

    public void preTriggered(int[] dataPack, double[] lastSpkTimes, double dt) {
        values[dataPack[1]*wts.getInc()+1] = -lRate * wMinus
                * Math.exp((lastSpkTimes[dataPack[3]+wts.offsetMajor]-(double)dataPack[0]*dt)/tauMinus);
    }


}
