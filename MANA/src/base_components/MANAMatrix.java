package base_components;

import base_components.enums.ConnectRule;
import base_components.enums.Ordering;
import base_components.enums.SynType;
import utils.SrcTarDataPack;
import utils.SrcTarPair;
import utils.Utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MANAMatrix {

    private SynapseMatrix weightsTOrd;

    private SynapseMatrix outDataSOrd;

    /** An addon set of values containing more target ordered data. */
    private SynMatDataAddOn tOrdLastArrivals;

    /**
     * A Map from source to target, specifically if you lookup the values when
     * source ordered the integer in this at the same coordinate gives the location
     * of the value when target ordered. Traverse this lookup map to rearrange a
     * source-ordered set into a target ordered one.
     */
    private int[] srcToTargLookup;

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
        double [][] delays = Utils.getDelays(src.getCoordinates(), tar.xyzCoors, src==tar, maxDist, maxDly);
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
                setSourceDefaults(srcData);
                srcData[0] = delays[ii][jj];
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
        assert(nnz == outDataSOrd.getNnz());

        srcToTargLookup = Utils.getSortKey(targCOOTup,
                Ordering.orderTypeTupleComp(Ordering.SOURCE));



        // Outbound values... delay, lastArr, U, D, F, u, R
       // outDataSOrd = new SynapseMatrix()

    }

    private void setSourceDefaults(final double [] sData) {
        ThreadLocalRandom localRand = ThreadLocalRandom.current();
        double [] meanVals = type.getDefaultUDFMeans();
        sData[2] = Math.abs(localRand.nextGaussian()*meanVals[0]/2 + meanVals[0]);
        sData[3] = Math.abs(localRand.nextGaussian()*meanVals[1]/2 + meanVals[1]);
        sData[4] = Math.abs(localRand.nextGaussian()*meanVals[2]/2 + meanVals[2]);
        sData[6] = 1;
    }


    //public List<SrcTarDataPack> getTuples(Ordering )

    /**
     * A class that converts a MANAMatrix into a set of COO tuples...
     * this allows one to easily add/remove synapses, then remake
     * a MANAMatrix from the new values.
     */
    public static class COOManaMat {
        public List<SrcTarDataPack>  data;
        public final int srcILF;
        public final int tarILF;

        public COOManaMat(MANAMatrix mat) {
            data = new LinkedList<>();
            int [] srcPtrs = mat.outDataSOrd.getRawPtrs();
            double [] tordVals = mat.weightsTOrd.getRawData();
            double [] sordVals = mat.outDataSOrd.getRawData();
            int [] tOrdInds = mat.outDataSOrd.getRawOrdIndices();
            int [] map = mat.srcToTargLookup;
            srcILF = mat.outDataSOrd.nILFac;
            tarILF = mat.weightsTOrd.nILFac;
            int totData = srcILF+tarILF + 2; // +2 for target last arrival and linear index
            int src=-1;
            // Traversing in source-major order
            for(int ii=0; ii<mat.noSrc; ++ii) {
                src++;
                for(int jj = srcPtrs[ii]; jj < srcPtrs[ii+1]; ++jj) { //linear indices
                    int tOrderIndex = mat.srcToTargLookup[jj];
                    double [] tmpData = new double[totData];
                    // copy in target ordered values using the lookup table since we're
                    // traversing source-ordered
                    System.arraycopy(tordVals, tOrderIndex*tarILF,
                            tmpData, 0, tarILF);
                    // copy in source ordered values for the same synapse
                    System.arraycopy(sordVals, jj, tmpData, tarILF, srcILF);
                    tmpData[tmpData.length-2] = mat.tOrdLastArrivals.values[tOrderIndex];
                    // Attach the linear index when target ordered to support target
                    // ordered add on sparse values...
                    tmpData[tmpData.length-1] = tOrderIndex;

                    SrcTarDataPack tmpDatPack =
                            new SrcTarDataPack(new SrcTarPair(src, tOrdInds[jj]), tmpData);
                    data.add(tmpDatPack);
                }
            }
        }
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
