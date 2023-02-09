package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.Matrices.InterleavedSparseMatrix;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.utils.BufferedDoubleArray;

public interface STDP {

    void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt, boolean src_exc, boolean tar_exc);
}