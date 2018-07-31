package functions;

public interface STDP {
    void postTriggered(int neuNo, double time);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(int tarNo, float[] dataPack);
}
