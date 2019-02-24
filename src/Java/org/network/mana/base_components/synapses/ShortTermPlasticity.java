package Java.org.network.mana.base_components.synapses;

import Java.org.network.mana.enums.SynapseType;

import java.util.concurrent.ThreadLocalRandom;

public class ShortTermPlasticity {

    public static void getPSR_UDF(int index, double time, double [] data) {
        double isi = -((time + data[index]) - data[index+1]); // time + delay - lastArrival
        if(isi > 0) {
            //    System.exit(1);
            throw new IllegalStateException("Anomalous ISI");
        }
        data[index+5] = data[index+2] + (data[index+5] * (1-data[index+2]) //U + (u * (1-U))*exp(-isi/F)
                * Math.exp(isi/data[index+4]));
        data[index+6] = 1 + ((data[index+6] - (data[index+5] * data[index+6]) - 1)
                * Math.exp(isi/data[index+3]));

    }

    // Outbound values... delay, lastArr, U, D, F, u, R
    public static void setSourceDefaults(final double [] sData, int start, SynapseType type) {
        ThreadLocalRandom localRand = ThreadLocalRandom.current();
        double [] meanVals = type.getDefaultUDFMeans();
        sData[2+start] = Math.abs(localRand.nextGaussian()*meanVals[0]/2 + meanVals[0]);
        sData[3+start] = Math.abs(localRand.nextGaussian()*meanVals[1]/2 + meanVals[1]);
        sData[4+start] = Math.abs(localRand.nextGaussian()*meanVals[2]/2 + meanVals[2]);
        sData[6+start] = 1;
    }
}
