package functions;

import base_components.Matrices.SynapseMatrix;
import base_components.Matrices.SynMatDataAddOn;

public interface STDP {
    void postTriggered(SynapseMatrix wts, SynMatDataAddOn lastArrs, int neuNo, double time);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(SynapseMatrix wts, int[] dataPack, double[] lastSpkTimes, double dt);
}
