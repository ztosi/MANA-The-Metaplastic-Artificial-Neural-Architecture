package functions;

import base_components.MANANeurons;
import base_components.Matrices.COOManaMat;
import base_components.Matrices.MANAMatrix;
import base_components.Neuron;
import base_components.SynapseData;
import utils.SrcTarDataPack;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

public class StructuralPlasticity {

    public static final double DEF_Thresh= 0.05;

    public static final double DEF_EXC_THRESH = 0.05;
    public static final double DEF_INH_THRESH = 0.05;
    public static final double DEF_CON_CONST = 0.4;
    public static final double P_ADD_MIN = 0.01;
    public static final double RT_LOG_PA_MIN = Math.sqrt(-Math.log(P_ADD_MIN));
    public static final double DEF_PG_INTERVAL = 2500; // ms


    public static MANAMatrix pruneGrow(MANAMatrix mat, Neuron src, MANANeurons tar, int noOutP, int noInP) {
        COOManaMat coo = new COOManaMat(mat);
        boolean rec = src == tar;
        Iterator<SrcTarDataPack> dataIter = coo.data.iterator();
        int[] inDegs = tar.getProperInDegrees(src);
        for(int ii=0; ii<src.getSize(); ++ii) {
            for(int jj=0; jj<tar.getSize(); ++jj) {
                if(rec && ii==jj) {
                    continue;
                }
                if (dataIter.hasNext()) {
                    SrcTarDataPack datum = dataIter.next();
                    if(datum.coo.src == ii && datum.coo.tar == jj) {
                        if(pruneDecision(src.getOutDegree()[ii],
                                noOutP, inDegs[jj],
                                noInP, datum.values[0])) {
                            dataIter.remove();
                        }
                    } else {
                        //TODO
                    }
                } else {
                    //TODO
                }

            }
        }
        return  new MANAMatrix(mat.getOffsetSrc(), mat.getOffsetTar(), coo, src, tar);
    }

    public static boolean pruneDecision(int srcOutDegree,
                                        int outPoss, int tarInDegree,
                                        int inPoss, double wVal) {
        if(wVal > SynapseData.MAX_WEIGHT * DEF_Thresh) {
            return false;
        } else if (wVal < SynapseData.MIN_WEIGHT) {
            return true;
        } else {
            double p = Math.pow((double) srcOutDegree/outPoss ,2) * tarInDegree/inPoss;
            return ThreadLocalRandom.current().nextDouble() < p;
        }
    }

    public static boolean growDecision(double[] xyz1, double xyz2[]) {
        return false; //TODO
    }

}
