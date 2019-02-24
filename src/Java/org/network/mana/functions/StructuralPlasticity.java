package Java.org.network.mana.functions;

import Java.org.network.mana.base_components.neurons.Neuron;
import Java.org.network.mana.base_components.sparse.SrcTarDataPack;
import Java.org.network.mana.base_components.sparse.SrcTarPair;
import Java.org.network.mana.base_components.synapses.ShortTermPlasticity;
import Java.org.network.mana.enums.ConnectRule;
import Java.org.network.mana.enums.Ordering;
import Java.org.network.mana.enums.SynapseType;
import Java.org.network.mana.globals.Default_Parameters;
import Java.org.network.mana.mana_components.COOManaMat;
import Java.org.network.mana.mana_components.MANAMatrix;
import Java.org.network.mana.mana_components.MANANeurons;
import Java.org.network.mana.mana_components.MANA_Node;
import Java.org.network.mana.utils.Utils;

import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;

public class StructuralPlasticity {

    private static double thresh = Default_Parameters.DEF_Thresh;
    public enum SPTechnique {
        GLOBAL_MAX, LOCAL_MAX
    }
    public static SPTechnique pruneTechnique = SPTechnique.GLOBAL_MAX; // TODO: make this better than a static var....

    public static MANAMatrix pruneGrow(MANA_Node node, Neuron src, MANANeurons tar,
                                       int noOutP, int noInP, double lambda, double c_x,
                                       double maxDist, double time, double maxWt) {
        COOManaMat coo = new COOManaMat(node.getSynMatrix(), Ordering.SOURCE);
        boolean rec = src == tar;
        ListIterator<SrcTarDataPack> dataIter = coo.data.listIterator();
        int[] inDegs = tar.getProperInDegrees(src);
        int [] noAdded = new int[src.getSize()];

        int noRemoved=0;
        int addcount = 0;
        try {
  //          double [] threshVals = pruneTechnique == SPTechnique.LOCAL_MAX ?
   //                 node.parent_sector.getThreshWeight(DEF_Thresh, time, node.srcData.isExcitatory()) : null;

//            List<SrcTarDataPack> toRemove = new ArrayList<>();
//            List<SrcTarDataPack> toAdd = new ArrayList<>();

            // thresh = exc ? thresh : thresh * 2;
            for (int ii = 0; ii < src.getSize(); ++ii) { // TODO it makes more sense for this to be in the opposite order and iterated that way
                for (int jj = 0; jj < tar.getSize(); ++jj) {
                    if (rec && ii == jj) {
                        continue;
                    }
                    if (dataIter.hasNext()) {
                        SrcTarDataPack datum = coo.data.get(dataIter.nextIndex());
                        if (datum.coo.src == ii && datum.coo.tar == jj) {
                            SrcTarDataPack dat = dataIter.next();
                            //double val = threshVals[jj]/DEF_Thresh;
                            if (pruneDecision(src.getOutDegree()[ii],
                                    noOutP, inDegs[jj],
                                    noInP, datum.values[0], pruneTechnique == SPTechnique.LOCAL_MAX ? 0//DEF_Thresh*(1-Math.sqrt(datum.values[0]/val))
                                            : maxWt * (time >4000 && time < 11000 ? 0.6 : Default_Parameters.DEF_Thresh))) {
                                dataIter.remove();
                                //toRemove.add(dat);
                                noRemoved++;
                            }
                            continue;
                        }
                    }
                    // if (noAdded[ii] < maxAdd) { // We have not added the maximum number of allowed synapses from this source
                    double newDly = growDecision(src.getCoordinates(false)[ii], tar.getCoordinates(false)[jj],
                            Default_Parameters.NEW_SYN_CONST //(0.05 * Math.exp(-inDegs[jj] / 5.0))
                                    * ConnectRule.getConProbBase(src.isExcitatory(), tar.isExcitatory()) + Default_Parameters.DEF_CON_CONST,
                            lambda, maxDist);
                    if (newDly > 0) {
                        double[] data = new double[11];
                        data[0] = Default_Parameters.DEF_NEW_WEIGHT;
                        data[1] = Default_Parameters.DEF_INIT_WDERIV * Default_Parameters.STDP_TIME_CONST;
                        ShortTermPlasticity.setSourceDefaults(data, 2, SynapseType.getSynType(src.isExcitatory(),
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
            for (SrcTarDataPack tup : coo.data) {
                tup.values[10] = tarLinInd++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("REMOVED: " + noRemoved);
        System.out.println("ADDED: " + addcount);

        coo.data.sort(Ordering.orderTypeTupleComp(Ordering.SOURCE));
        //return new MANAMatrix(mat.getOffsetSrc(), mat.getOffsetTar(), coo, src, tar);
        return new MANAMatrix(coo, src, tar);

    }

    public static boolean pruneDecision(int srcOutDegree,
                                        int outPoss, int tarInDegree,
                                        int inPoss, double wVal, double thresh) {
        if(wVal < Default_Parameters.MIN_WEIGHT){
            return true;
        } else if  (wVal > thresh) {
            return false;
        } else {
            double p = (double)srcOutDegree/outPoss * Math.pow((double)tarInDegree/inPoss,2);
            return ThreadLocalRandom.current().nextDouble() < p;
        }
    }

    public static double growDecision(double[] xyz1, double xyz2[], double c_x, double lambda, double maxDist) {
        double dist = Utils.euclidean(xyz1, xyz2);
        double prob = c_x * Math.exp(-(dist*dist)/(lambda*lambda));
        if (ThreadLocalRandom.current().nextDouble() < prob) {
            return (dist/maxDist)* Default_Parameters.MAX_DELAY;
        }
        return -1;
    }

}
