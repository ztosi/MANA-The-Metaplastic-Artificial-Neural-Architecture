package functions;

import base_components.MANANeurons;
import base_components.Matrices.COOManaMat;
import base_components.Matrices.MANAMatrix;
import base_components.Neuron;
import base_components.SynapseData;
import base_components.enums.Ordering;
import base_components.enums.SynType;
import utils.SrcTarDataPack;
import utils.SrcTarPair;
import utils.Utils;

import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;

public class StructuralPlasticity {

    public static final double DEF_Thresh= 0.05;

    //public static final double DEF_EXC_THRESH = 0.05;
    //public static final double DEF_INH_THRESH = 0.05;
    public static final double DEF_CON_CONST = 0.4;
    public static final double P_ADD_MIN = 0.01;
    //public static final double RT_LOG_PA_MIN = Math.sqrt(-Math.log(P_ADD_MIN));
    //public static final double DEF_PG_INTERVAL = 2500; // ms
    public static final double MAX_ADD_RATIO = 0.01;


    public static MANAMatrix pruneGrow(MANAMatrix mat, Neuron src, MANANeurons tar,
                                       int noOutP, int noInP, double lambda, double c_x,
                                       double maxDist, double time) {
        COOManaMat coo = new COOManaMat(mat, Ordering.SOURCE);
        boolean rec = src == tar;
        ListIterator<SrcTarDataPack> dataIter = coo.data.listIterator();
        int[] inDegs = tar.getProperInDegrees(src);
        int maxAdd = (int) (tar.N * MAX_ADD_RATIO) + 1;
        int [] noAdded = new int[src.getSize()];
        for(int ii=0; ii<src.getSize(); ++ii) { // TODO it makes more sense for this to be in the opposite order and iterated that way
            for(int jj=0; jj<tar.getSize(); ++jj) {
                if(rec && ii==jj) {
                    continue;
                }
                if (dataIter.hasNext()) {
                    SrcTarDataPack datum = dataIter.next();
                    if (datum.coo.src == ii && datum.coo.tar == jj) {
                        if (pruneDecision(src.getOutDegree()[ii],
                                noOutP, inDegs[jj],
                                noInP, datum.values[0])) {
                            dataIter.remove();
                        }
                        continue;
                    }
                }
                if (noAdded[ii] < maxAdd) { // We have not added the maximum number of allowed synapses from this source
                    double newDly = growDecision(src.getCoordinates()[ii], tar.getCoordinates()[jj],
                            lambda, c_x, maxDist);
                    if(newDly > 0) {
                        double[] data = new double[11];
                        data[0] = SynapseData.DEF_NEW_WEIGHT;
                        data[1] = SynapseData.DEF_INIT_WDERIV;
                        SynType.setSourceDefaults(data, 2, SynType.getSynType(src.isExcitatory(),
                                tar.isExcitatory()));
                        data[2] = newDly;
                        data[3] = 0; // as far as UDF is concerned this has never spiked, redundant, but important
                        data[9] = time; // As far as STDP is concerned we're going to pretend a spike arrived now
                        noAdded[ii]++;
                        SrcTarDataPack newDatum = new SrcTarDataPack(new SrcTarPair(ii, jj), data);
                        dataIter.add(newDatum);
                    }
                }

            }
        }
        coo.data.sort(Ordering.orderTypeTupleComp(Ordering.TARGET));
        int tarLinInd = 0;
        for(SrcTarDataPack tup : coo.data) {
            tup.values[10] = tarLinInd++;
        }
        coo.data.sort(Ordering.orderTypeTupleComp(Ordering.SOURCE));
        //return new MANAMatrix(mat.getOffsetSrc(), mat.getOffsetTar(), coo, src, tar);
        return new MANAMatrix(coo, src, tar);

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

    public static double growDecision(double[] xyz1, double xyz2[], double c_x, double lambda, double maxDist) {
        double dist = Utils.euclidean(xyz1, xyz2);
        double prob = c_x * Math.exp(-(dist*dist)/(lambda*lambda));
        if (ThreadLocalRandom.current().nextDouble() < prob) {
            return (dist/maxDist)*SynapseData.MAX_DELAY;
        }
        return -1;
    }

}
