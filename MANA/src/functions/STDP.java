package functions;

public interface STDP {
    void postTriggered(int neuNo, double time);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(int[] dataPack, double[] lastSpkTimes, double dt);
}
