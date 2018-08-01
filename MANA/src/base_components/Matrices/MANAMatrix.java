package base_components.Matrices;

import base_components.*;
import base_components.enums.ConnectRule;
import base_components.enums.Ordering;
import base_components.enums.SynType;
import functions.STDP;
import utils.SrcTarDataPack;
import utils.SrcTarPair;
import utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MANAMatrix {

    protected SynapseMatrix weightsTOrd;

    protected SynapseMatrix outDataSOrd;

    /** An addon set of values containing more target ordered data. */
    protected SynMatDataAddOn tOrdLastArrivals;

    /**
     * A Map from source to target, specifically if you lookup the values when
     * source ordered the integer in this at the same coordinate gives the location
     * of the value when target ordered. Traverse this lookup map to rearrange a
     * source-ordered set into a target ordered one. It has #synapses number of elements
     * and for accesses must be multipled by nilFac to get the absolute index.
     */
    protected int[] srcToTargLookup;

    public final SynType type;

    private int nnz;

    public final int noSrc;

    public final int noTar;

    public final Neuron src;

    public final MANANeurons tar;

    /**
     * Makes a MANAMatrix from the COO ordered tuples of all synapse values...
     * @param offsetSrc
     * @param offsetTar
     * @param cooMat
     * @param src
     * @param tar
     */
    public MANAMatrix(int offsetSrc, int offsetTar, COOManaMat cooMat, Neuron src, MANANeurons tar) {
        //Collections.sort(cooMat.data, Ordering.orderTypeTupleComp(Ordering.SOURCE));
        noSrc = src.getSize();
        noTar = tar.getSize();
        this.src = src;
        this.tar = tar;
        type = SynType.getSynType(src.isExcitatory(), tar.isExcitatory());
        int [] targRange = {0, cooMat.tarILF};
        weightsTOrd = new SynapseMatrix(cooMat.data, targRange, noTar, noSrc,
                offsetTar, offsetSrc, Ordering.TARGET);
        tOrdLastArrivals = new SynMatDataAddOn(weightsTOrd, 1);
       // pfrBuffers = new SynMatDataAddOn(weightsTOrd, 1);
        int cnt=0;
        for(SrcTarDataPack tup : cooMat.data) {
            tOrdLastArrivals.values[cnt] = tup.values[tup.values.length-2];
            // Assign the now TARGET ORDERED cooMat.data linear (target ordered) indices
            tup.values[tup.values.length-1] = cnt++;
        }
        int [] srcRange = {cooMat.tarILF, cooMat.tarILF+cooMat.srcILF};
        // This will source order sort cooMat.data!
        // So now the target ordered linear indices will be in source order...
        outDataSOrd = new SynapseMatrix(cooMat.data, srcRange, noSrc, noTar,
                offsetSrc, offsetTar, Ordering.SOURCE);
        srcToTargLookup = new int[cooMat.data.size()];
        cnt = 0;
        // Copy the target linear indices that are source ordered to our lookup table
        for(SrcTarDataPack tup : cooMat.data) {
            srcToTargLookup[cnt++] = (int) tup.values[tup.values.length-1];
        }
    }

    /**
     * Using a lot of defaults
     * @param offsetSrc
     * @param offsetTar
     * @param src
     * @param tar
     * @param maxDist
     * @param maxDly
     * @param params the connection parameters for random
     *              (1 parameter) or distance based random (2 parameters)
     */
    public MANAMatrix(int offsetSrc, int offsetTar,
                      Neuron src, MANANeurons tar,
                      double maxDist, double maxDly,
                      ConnectRule cRule, double[] params) {
        this.src = src;
        this.tar = tar;
        List<SrcTarDataPack> targCOOTup = new LinkedList<>();
        List<SrcTarDataPack> srcCOOTup = new LinkedList<>();
        type = SynType.getSynType(src.isExcitatory(), tar.isExcitatory());
        this.noSrc = src.getSize();
        this.noTar = tar.N;
        System.out.println();
        for (int ii=0; ii<src.getSize(); ii++) {
//            boolean skp = true;
            for(int jj=0; jj<tar.N; jj++) {
//                System.out.print("  ");
                if (src == tar && ii==jj) {
//                    System.out.print(".");
                    continue;
                }
                if (cRule == ConnectRule.Random) {
                    if (ThreadLocalRandom.current().nextDouble() >= params[0]) {
//                        System.out.print(".");
                        continue;
                    }
                } else if (cRule == ConnectRule.Distance) {
                    double dist = Utils.euclidean(src.getCoordinates()[ii], tar.xyzCoors[jj]);
                    double cProb = params[0] * Math.exp(-((dist*dist)/(params[1] *params[1])));
                    if (ThreadLocalRandom.current().nextDouble() >= cProb) {
//                        System.out.print(".");
                        continue;
                    }
                }
//                System.out.print("X");
                SrcTarPair coo = new SrcTarPair(ii, jj);
                double[] tarData = {SynapseData.DEF_NEW_WEIGHT, 0};
                SrcTarDataPack tarDatPack = new SrcTarDataPack(coo, tarData);
                double[] srcData = new double[7];
                SynType.setSourceDefaults(srcData, type);
                srcData[0] = maxDly * Utils.euclidean(src.getCoordinates()[ii], tar.xyzCoors[jj])/maxDist;
                SrcTarDataPack srcDatPack = new SrcTarDataPack(coo, srcData);

                targCOOTup.add(tarDatPack);
                srcCOOTup.add(srcDatPack);
            }
//            System.out.println(" ");
        }
        int[] tRange = {0, 2};
        int[] sRange = {0, 7};
        weightsTOrd = new SynapseMatrix(targCOOTup, tRange, src.getSize(), tar.N,
                offsetSrc, offsetTar, Ordering.TARGET);
        outDataSOrd = new SynapseMatrix(srcCOOTup, sRange, tar.N, src.getSize(),
                offsetTar, offsetSrc, Ordering.SOURCE);
        nnz = weightsTOrd.getNnz();
        tOrdLastArrivals = new SynMatDataAddOn(weightsTOrd, 1);
        //pfrBuffers = new SynMatDataAddOn(weightsTOrd, 1);
        assert(nnz == outDataSOrd.getNnz());

        srcToTargLookup = Utils.getSortKey(targCOOTup,
                Ordering.orderTypeTupleComp(Ordering.SOURCE));



        // Outbound values... delay, lastArr, U, D, F, u, R
       // outDataSOrd = new SynapseMatrix()

    }

    public void spike(int noSrc, double time) {
        int start = outDataSOrd.getStartIndex(noSrc);
        int end = outDataSOrd.getEndIndex(noSrc);
        for(int ii=start; ii<end; ii+=outDataSOrd.getInc()) {
            SynType.getPSR_UDF(ii, time, outDataSOrd.getRawData());
        }
    }

    // data pack is {arrTime, rel tar ind, udfMultiplier, abs tar ind}

    public void addEvents(int noSrc, double time, double dt, PriorityQueue<int []> eventQ) {
        int start = outDataSOrd.getStartIndex(noSrc);
        int end = outDataSOrd.getEndIndex(noSrc);
        int inc = outDataSOrd.getInc();
        double [] vals = outDataSOrd.getRawData();
        for(int ii=start; ii<end; ii+=inc) {
            int [] evt = new int[4];
            evt[0] = (int) ((time+vals[ii])/dt);
            evt[1] = srcToTargLookup[ii] * weightsTOrd.getInc();
            evt[2] = Float.floatToIntBits((float) (vals[ii+inc-1] * vals[ii+inc-2]));
            evt[3] = outDataSOrd.getRawOrdIndices()[ii/inc];
            eventQ.add(evt);
        }
    }

    public void processEvents(PriorityQueue<int[]> eventQ, double[] incCur, double time, double dt) {
        while(eventQ.peek()[0]*dt <= time) {
            int[] event = eventQ.poll();
            incCur[event[3]] += weightsTOrd.getRawOrdIndices()[event[1]]
                    * Float.intBitsToFloat(event[2]);
        }
    }

    public void processEventsSTDP(PriorityQueue<int[]> eventQ, double[] incCur, STDP stdpRule,
                                  double[] lastSpkTimes, double time, double dt) {
        while(eventQ.peek()[0]*dt <= time) {
            int[] event = eventQ.poll();
            incCur[event[3]] += weightsTOrd.getRawOrdIndices()[event[1]]
                    * Float.intBitsToFloat(event[2]);
            stdpRule.preTriggered(event, lastSpkTimes, dt);
        }
    }

    public void normWts(double[] normVals) {
        weightsTOrd.mulFromArray(normVals, 0);
    }

    public void updateWeights() {
        weightsTOrd.addDw2W();
    }

    public double[] getWeightSums(double[] localWtSums) {
        weightsTOrd.sumIncoming(localWtSums, 0);
        return localWtSums;
    }

    //public List<SrcTarDataPack> getTuples(Ordering )


    public void spike(int srcInd) {

    }

    public void mhpStage1() {

    }

    public static void main(String [] args) {
        int numN = 10;
        MANANeurons src = new MANANeurons(numN, true,
                Utils.getUniformRandomArray(numN, 0, 100),
                Utils.getUniformRandomArray(numN, 0, 100),
                Utils.getUniformRandomArray(numN, 0, 100));
        MANANeurons tar = new MANANeurons(numN, true,
                Utils.getUniformRandomArray(numN, 0, 100),
                Utils.getUniformRandomArray(numN, 0, 100),
                Utils.getUniformRandomArray(numN, 0, 100));
        double [] parms = {0.1};
        MANAMatrix mm = new MANAMatrix(0,0, src, tar,
                Math.sqrt(30000), 20, ConnectRule.Random, parms);

        // TODO: Actually use JUnit instead of being lazy... so lazy!
        System.out.println("Dummy. It's a place for a breakpoint! :D");

    }


}
