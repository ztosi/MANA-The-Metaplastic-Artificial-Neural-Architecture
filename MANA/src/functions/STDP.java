package functions;

import base_components.Matrices.InterleavedSparseMatrix;
import base_components.Matrices.InterleavedSparseAddOn;
import utils.BufferedDoubleArray;

public interface STDP {

    double DEF_LEARNING_RATE = 1E-6;

    void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt);
}
