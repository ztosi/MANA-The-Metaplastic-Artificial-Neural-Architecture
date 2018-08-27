package utils;

public class WeightData {
    public final int [] srcInds;
    public final int [] tarInds;
    public final double [] values;

    public WeightData(int nnz) {
        srcInds = new int[nnz];
        tarInds = new int[nnz];
        values = new double[nnz];
    }
}
