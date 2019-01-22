package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseMatrix;
import Java.org.network.mana.utils.BufferedDoubleArray;

public final class HebSTDP implements STDP {

    public double tauPlus = 20;

    public double tauMinus = 100;

    public double wPlus = 5;

    public double wMinus = 1;

    public double lRate = 1E-5;

    public HebSTDP() {
    }

    public HebSTDP(double tauPlus, double tauMinus,
                   double wPlus, double wMinus, double lRate) {
        this.lRate = lRate;
        this.wMinus = wMinus;
        this.wPlus = wPlus;
        this.tauPlus = tauPlus;
        this.tauMinus = tauMinus;
    }

    @Override
    public void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time) {
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            wts.getRawData()[ii+1] = lRate* wPlus;
        }
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            wts.getRawData()[ii+1] *= Math.exp((lastArrs.values[laLoc]-time)/tauPlus);
            laLoc += lastArrs.getInc();
        }

    }

    // data pack is {arrTime, rel tar ind, udfMultiplier, abs tar ind}

    @Override
    public void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt) {
        wts.getRawData()[dataPack[1]+1] = -lRate * wMinus
                * Math.exp((lastSpkTimes.getData(dataPack[3])-(double)dataPack[0]*dt)/tauMinus);
    }


}
