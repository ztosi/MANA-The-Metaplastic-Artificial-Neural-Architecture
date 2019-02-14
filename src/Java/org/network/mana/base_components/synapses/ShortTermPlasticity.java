package Java.org.network.mana.base_components.synapses;

public class ShortTermPlasticity {

    static final double[] EEUDF = new double[]{0.5, 1100, 50};
    static final double[] EIUDF = new double[]{0.05, 125, 1200};
    static final double[] IEUDF = new double[]{0.25, 700, 20};
    static final double[] IIUDF = new double[]{0.32, 144, 60};

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

}
