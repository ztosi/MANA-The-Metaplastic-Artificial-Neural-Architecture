package Java.org.network.mana.base_components.synapses;

import Java.org.network.mana.base_components.sparse.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.sparse.InterleavedSparseMatrix;
import Java.org.network.mana.globals.Default_Parameters;
import Java.org.network.mana.utils.BufferedDoubleArray;

public interface STDP {

    double DEF_LEARNING_RATE = 1E-6;

    static STDP getDefaultSTDP(boolean srcExc, boolean tarExc) {
        if (srcExc) {
            if (tarExc)
                return new HebSTDP(Default_Parameters.eeTauPlus, Default_Parameters.eeTauMinus, Default_Parameters.eeWPlus, Default_Parameters.eeWMinus, DEF_LEARNING_RATE);
            else
                return new HebSTDP(Default_Parameters.eiTauPlus, Default_Parameters.eiTauMinus, Default_Parameters.eiWPlus, Default_Parameters.eiWMinus, DEF_LEARNING_RATE);
        } else {
            if (tarExc)
                return new MexHatSTDP(Default_Parameters.ieWPlus, Default_Parameters.ieWMinus, Default_Parameters.ieSigma, DEF_LEARNING_RATE);
            else
                return new MexHatSTDP(Default_Parameters.iiWPlus, Default_Parameters.iiWMinus, Default_Parameters.iiSigma, DEF_LEARNING_RATE);
        }
    }

    void postTriggered(InterleavedSparseMatrix wts, InterleavedSparseAddOn lastArrs, int neuNo, double time, double dt);

    // data pack is {arrTime, rel tar ind, udfMultiplier}

    void preTriggered(InterleavedSparseMatrix wts, int[] dataPack, BufferedDoubleArray lastSpkTimes, double dt);
}
