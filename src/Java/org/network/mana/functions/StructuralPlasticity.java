package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Matrices.COOManaMat;
import Java.org.network.mana.base_components.Matrices.SynapseMatrix;
import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.base_components.SynapseData;
import Java.org.network.mana.base_components.enums.Ordering;
import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.nodes.MANA_Node;
import Java.org.network.mana.utils.SrcTarDataPack;
import Java.org.network.mana.utils.SrcTarPair;
import Java.org.network.mana.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;

public class StructuralPlasticity {

    public static final double DEF_Thresh= 0.05;

    //public static final double DEF_EXC_THRESH = 0.05;
    //public static final double DEF_INH_THRESH = 0.05;
    public static final double DEF_CON_CONST = 0.0025;
    public static final double P_ADD_MIN = 0.01;
    //public static final double RT_LOG_PA_MIN = Math.sqrt(-Math.log(P_ADD_MIN));
    //public static final double DEF_PG_INTERVAL = 2500; // ms
    public static final double MAX_ADD_RATIO = 0.01;
    private static double thresh = DEF_Thresh;

    public static SynapseMatrix pruneGrow(MANA_Node node, SpikingNeuron src, MANANeurons tar,
                                          int noOutP, int noInP, double lambda, double c_x,
                                          double maxDist, double time, double maxWt) {
        COOManaMat coo = new COOManaMat(node.getSynMatrix(), Ordering.SOURCE);
        boolean rec = src == tar;
        ListIterator<SrcTarDataPack> dataIter = coo.data.listIterator();
        int[] inDegs = tar.getProperInDegrees(src);
        int maxAdd = (int) (tar.N * MAX_ADD_RATIO) + 1;
        int [] noAdded = new int[src.getSize()];
        double mx = maxWt;
        int noRemoved=0;
        int addcount = 0;
        List<SrcTarDataPack> toRemove = new ArrayList<>();
        List<SrcTarDataPack> toAdd = new ArrayList<>();

       // thresh = exc ? thresh : thresh * 2;
        for(int ii=0; ii<src.getSize(); ++ii) { // TODO it makes more sense for this to be in the opposite order and iterated that way
            for(int jj=0; jj<tar.getSize(); ++jj) {
                if(rec && ii==jj) {
                    continue;
                }
                if (dataIter.hasNext()) {
                    SrcTarDataPack datum = coo.data.get(dataIter.nextIndex());
                    if (datum.coo.src == ii && datum.coo.tar == jj) {
                        SrcTarDataPack dat = dataIter.next();
                        if (pruneDecision(src.getOutDegree()[ii],
                                noOutP, inDegs[jj],
                                noInP, datum.values[0], mx)) {
                            dataIter.remove();
                            //toRemove.add(dat);
                            noRemoved++;
                        }
                        continue;
                    }
                }
               // if (noAdded[ii] < maxAdd) { // We have not added the maximum number of allowed synapses from this source
                    double newDly = growDecision(src.getCoordinates(false)[ii], tar.getCoordinates(false)[jj],
                            //(0.1 * Math.exp(-inDegs[jj]/5.0))*
                                      0.2,//SynType.getConProbBase(src.isExcitatory(), tar.isExcitatory())/2,
                            lambda, maxDist);
                    if(newDly > 0) {
                        double[] data = new double[11];
                        data[0] = SynapseData.DEF_NEW_WEIGHT;
                        data[1] = SynapseData.DEF_INIT_WDERIV * SynapseData.E_LR;
                        SynType.setSourceDefaults(data, 2, SynType.getSynType(src.isExcitatory(),
                                tar.isExcitatory()));
                        data[2] = newDly;
                        data[3] = 0; // as far as UDF is concerned this has never spiked, redundant, but important
                        data[9] = time; // As far as STDP is concerned we're going to pretend a spike arrived now
                        noAdded[ii]++;
                        SrcTarDataPack newDatum = new SrcTarDataPack(new SrcTarPair(ii, jj), data);
                        dataIter.add(newDatum);
                        addcount++;
                    }
               // }

            }
        }
        coo.data.sort(Ordering.orderTypeTupleComp(Ordering.TARGET));
        int tarLinInd = 0;
        for(SrcTarDataPack tup : coo.data) {
            tup.values[10] = tarLinInd++;
        }

        System.out.println("REMOVED: " + noRemoved);
        System.out.println("ADDED: " + addcount);

        coo.data.sort(Ordering.orderTypeTupleComp(Ordering.SOURCE));
        //return new MANAMatrix(mat.getOffsetSrc(), mat.getOffsetTar(), coo, src, tar);
        return new SynapseMatrix(coo, src, tar);

    }

    private static final double lgcnst = Math.log(0.0005);

    public static boolean pruneDecision(int srcOutDegree,
                                        int outPoss, int tarInDegree,
                                        int inPoss, double wVal, double maxWt) {
        if(wVal < SynapseData.MIN_WEIGHT){
            return true;
        } else
        if  (wVal > maxWt * DEF_Thresh) {
            return false;
        } else {
            //double a = lgcnst/(maxWt/2);
            //double p = 0.1*Math.exp(a*wVal) * (((double)tarInDegree)/inPoss) * (double)srcOutDegree/outPoss;
            double p = (double)srcOutDegree/outPoss* Math.pow((double)tarInDegree/inPoss,2);
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
