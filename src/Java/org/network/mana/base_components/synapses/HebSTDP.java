package Java.org.network.mana.base_components.synapses;

import Java.org.network.mana.base_components.sparse.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.sparse.InterleavedSparseMatrix;
import Java.org.network.mana.utils.BufferedDoubleArray;

import java.util.concurrent.ThreadLocalRandom;

public final class HebSTDP implements STDP {

    public double tauPlus = 20;

    public double tauMinus = 100;

    public double wPlus = 5;

    public double wMinus = 1;

    public double lRate = 1E-3;

    public static final double twentRt = Math.pow(20, 1.0/4.0);

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
    public void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time, double dt) {
        int start = wts.getStartIndex(neuNo);
        int laLoc = wts.getStartIndex(neuNo, lastArrs.getInc());
        int end = wts.getEndIndex(neuNo);

        for(int ii = start; ii<end; ii+=wts.getInc()) {
            if(wts.getRawData()[ii] > 20) {
                wts.getRawData()[ii] = 20;
            }
        }

        for(int ii = start; ii<end; ii+=wts.getInc()) {
            wts.getRawData()[ii+1] = lRate * dt* wPlus * Math.pow(-wts.getRawData()[ii]+20, 0.25)/twentRt;
        }
        for(int ii = start; ii<end; ii+=wts.getInc()) {
            wts.getRawData()[ii+1] *= Math.exp((lastArrs.values[laLoc]-time)/tauPlus);
                  //  * ( 0.1 * ThreadLocalRandom.current().nextGaussian() + 1);
            laLoc += lastArrs.getInc();
        }

    }

    // data pack is {arrTime, rel tar ind, udfMultiplier, abs tar ind}

    @Override
    public void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt) {
        wts.getRawData()[dataPack[1]+1] = -lRate * wMinus * dt
                * Math.exp((lastSpkTimes.getData(dataPack[3])-(double)dataPack[0]*dt)/tauMinus);
       //         * ( 0.1 * ThreadLocalRandom.current().nextGaussian() + 1);
//        if(wts.getRawData()[dataPack[1]]+1 < (-lRate * wMinus)) {
//            System.out.println("problem");
//        }
            if(wts.getRawData()[dataPack[1]] < 0) {
                wts.getRawData()[dataPack[1]] = 0;
            }
    }


}
