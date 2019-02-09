package Java.org.network.mana.base_components.Matrices;

import Java.org.network.mana.base_components.enums.ConnectRule;
import Java.org.network.mana.base_components.enums.Ordering;
import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Neuron;

import Java.org.network.mana.functions.STDP;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MANAMatrix {

    /**
     * Interleaved in the following way { weights, dw } contains weight information.
     */
    protected InterleavedSparseMatrix weightsTOrd;

    /**
     * Interleaved in the following way { delay, lastArr, U, D, F, u, R }, and arranged in
     * source-major order (data pertaining to target neurons are contiguous and ordered). Contains
     * all the necessary information to calculate UDF and spike arrivals.
     */
    protected InterleavedSparseMatrix outDataSOrd;

    /** An addon set of values containing more target ordered data. */
    protected InterleavedSparseAddOn tOrdLastArrivals;

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
     * @param cooMat
     * @param src
     * @param tar
     */
  //  public MANAMatrix(int offsetSrc, int offsetTar, COOManaMat cooMat, Neuron src, MANANeurons tar) {
    public MANAMatrix(COOManaMat cooMat, Neuron src, MANANeurons tar) {

            //Collections.sort(cooMat.data, Ordering.orderTypeTupleComp(Ordering.SOURCE));
        noSrc = src.getSize();
        noTar = tar.getSize();
        this.src = src;
        this.tar = tar;
        type = SynType.getSynType(src.isExcitatory(), tar.isExcitatory());
        int [] targRange = {0, cooMat.tarILF};
        weightsTOrd = new InterleavedSparseMatrix(cooMat.data, targRange, noTar, noSrc,
                //offsetTar, offsetSrc,
                Ordering.TARGET);
        tOrdLastArrivals = new InterleavedSparseAddOn(weightsTOrd, 1);
       // pfrBuffers = new InterleavedSparseAddOn(weightsTOrd, 1);
        int cnt=0;
        for(SrcTarDataPack tup : cooMat.data) {
            tOrdLastArrivals.values[cnt] = tup.values[tup.values.length-2];
            // Assign the now TARGET ORDERED cooMat.data linear (target ordered) indices
            tup.values[tup.values.length-1] = cnt++;
        }
        int [] srcRange = {cooMat.tarILF, cooMat.tarILF+cooMat.srcILF};
        // This will source order sort cooMat.data!
        // So now the target ordered linear indices will be in source order...
        outDataSOrd = new InterleavedSparseMatrix(cooMat.data, srcRange, noSrc, noTar,
                //offsetSrc, offsetTar,
                Ordering.SOURCE);
        srcToTargLookup = new int[cooMat.data.size()];
        cnt = 0;
        // Copy the target linear indices that are source ordered to our lookup table
        for(SrcTarDataPack tup : cooMat.data) {
            srcToTargLookup[cnt++] = (int) tup.values[tup.values.length-1];
        }
    }

    /**
     * Outgoing data in {@link #outDataSOrd} is interleaved in the following way
     * { delay, lastArr, U, D, F, u, R }, while {@link #weightsTOrd} is interleaved as
     * { weight, dw }.
     * Using a lot of defaults
     * @param src
     * @param tar
     * @param maxDist
     * @param maxDly
     */
    public MANAMatrix(Neuron src, MANANeurons tar,
                      double maxDist, double maxDly,
                      ConnectSpecs cSpecs) {
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
                if (cSpecs.rule == ConnectRule.Random) {
                    if (ThreadLocalRandom.current().nextDouble() >= cSpecs.parms[0]) { // TODO: This isn't how this is used... do something _NOT_ dumb with prob dists...
//                        System.out.print(".");
                        continue;
                    }
                } else if (cSpecs.rule == ConnectRule.Distance) {
                    double dist = Utils.euclidean(src.getCoordinates(false)[ii], tar.xyzCoors[jj]);
                    double cProb = cSpecs.parms[0] * Math.exp(-((dist*dist)/(cSpecs.parms[1] * cSpecs.parms[1])));
                    if (ThreadLocalRandom.current().nextDouble() >= cProb) {
//                        System.out.print(".");
                        continue;
                    }
                } else if (cSpecs.rule == ConnectRule.Distance2) {
                    double dist = Utils.euclidean(src.getCoordinates(false)[ii], tar.xyzCoors[jj]);
                    int loc;
                    if(src.isExcitatory()) {
                        if(tar.isExcitatory()) {
                            loc = 0;
                        } else {
                            loc = 1;
                        }
                    } else {
                        if(tar.isExcitatory()) {
                            loc = 2;
                        } else {
                            loc = 3;
                        }
                    }
                    if(dist > cSpecs.parms[loc] ||
                            ThreadLocalRandom.current().nextDouble() > cSpecs.parms[cSpecs.parms.length-1]) {
                        continue;
                    }
                }
//                System.out.print("X");
                SrcTarPair coo = new SrcTarPair(ii, jj);
                double[] tarData = {cSpecs.getNewWt(), 0};
                SrcTarDataPack tarDatPack = new SrcTarDataPack(coo, tarData);
                double[] srcData = new double[7];
                SynType.setSourceDefaults(srcData, 0, type);
                srcData[0] = MANA_Globals.dt * (int)(maxDly * Utils.euclidean(src.getCoordinates(false)[ii],
                        tar.xyzCoors[jj])/(maxDist*MANA_Globals.dt));
                SrcTarDataPack srcDatPack = new SrcTarDataPack(coo, srcData);

                targCOOTup.add(tarDatPack);
                srcCOOTup.add(srcDatPack);
            }
//            System.out.println(" ");
        }
        int[] tRange = {0, 2};
        int[] sRange = {0, 7};
        weightsTOrd = new InterleavedSparseMatrix(targCOOTup, tRange, src.getSize(), tar.N,
                //offsetSrc, offsetTar,
                Ordering.TARGET);
        outDataSOrd = new InterleavedSparseMatrix(srcCOOTup, sRange, tar.N, src.getSize(),
                //offsetTar, offsetSrc,
                Ordering.SOURCE);
        nnz = weightsTOrd.getNnz();
        tOrdLastArrivals = new InterleavedSparseAddOn(weightsTOrd, 1);
        //pfrBuffers = new InterleavedSparseAddOn(weightsTOrd, 1);
        assert(nnz == outDataSOrd.getNnz());

        srcToTargLookup = Utils.getSortKey(targCOOTup,
                Ordering.orderTypeTupleComp(Ordering.SOURCE));



        // Outbound values... delay, lastArr, U, D, F, u, R
       // outDataSOrd = new InterleavedSparseMatrix()

    }

    /**
     * At the moment a source neuron spikes calculates the UDF-psp contribution
     * and sets the outgoing last arrival time to when that spike will arrive given
     * the synapse. Does this for all local outgoing synapses from the source neuron
     * @param noSrc
     * @param time
     */
    public void calcSpikeResponses(int noSrc, double time) {
        int start = outDataSOrd.getStartIndex(noSrc);
        int end = outDataSOrd.getEndIndex(noSrc);
        for(int ii=start; ii<end; ii+=outDataSOrd.getInc()) {
            SynType.getPSR_UDF(ii, time, outDataSOrd.getRawData()); // calculate UDF
            outDataSOrd.getRawData()[ii+1] = outDataSOrd.getRawData()[ii]+time; // Sets the new last arrival time to when this calcSpikeResponses will arrive.
        }
    }

    /**
     * Based on their arrival times, adds event data (what is necessary
     * to know when and where a spike will arrive and how much of a contribution it'll make). Performs
     * this for all local outgoing synapses from a given neuron. This function directly populates
     * the event queue and therefor performs all the necessary event encoding.
     * Order for events is: {arrTime, rel tar ind, udfMultiplier, abs tar ind} with udfMultiplier being
     * a float represented as int bits.
     * @param noSrc index of the source neuron
     * @param time simulation clock
     * @param dt integration time step
     * @param eventQ the node-local synaptic event queue
     */
    public void addEvents(int noSrc, double time, double dt, PriorityBlockingQueue<int []> eventQ) {
        int start = outDataSOrd.getStartIndex(noSrc);
        int end = outDataSOrd.getEndIndex(noSrc);
        int inc = outDataSOrd.getInc();
        double [] vals = outDataSOrd.getRawData();
        try {
            for (int ii = start; ii < end; ii += inc) {
                int[] evt = new int[4];
                evt[0] = (int) ((time + vals[ii]) / dt);
                evt[1] = srcToTargLookup[ii/inc] * weightsTOrd.getInc();
                evt[2] = Float.floatToIntBits((float) (10 * vals[ii + inc - 1] * vals[ii + inc - 2]));
                if(Float.intBitsToFloat(evt[2]) > 200) {

                    throw new IllegalStateException("Unusual UDF Response");
                }
                evt[3] = outDataSOrd.getRawOrdIndices()[ii / inc];
                eventQ.add(evt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    public int getOffsetTar() {
//        return weightsTOrd.offsetMajor;
//    }

//    public int getOffsetSrc() {
//        return weightsTOrd.offsetMinor;
//    }

    /**
     * Processes synaptic events that is, queued spikes which have an arrival time, destination,
     * and which contribute a specific current value. Perform STDP and add the PSP to an array
     * meant to contain the local total incoming currents to each target neuron.
     * @param eventQ the node-local event queue of calcSpikeResponses events to be processed, events are stored as integer arrays
     *               {arrivalTime/dt, absolute index,
     *               post synaptic response (float encoded in int bits), target number}
     * @param incCur the local incoming total currents to each target neuron
     * @param stdpRule the STDP rule used to perform STDP
     * @param lastSpkTimes the last time each post synaptic cell spiked.
     * @param time current time
     * @param dt simulation delta t
     */
    public void processEventsSTDP(PriorityBlockingQueue<int[]> eventQ, double[] incCur, STDP stdpRule,
                                  BufferedDoubleArray lastSpkTimes, double time, double dt) {
        int [] event = null;
        try {
            while (!eventQ.isEmpty() && eventQ.peek()[0] * dt <= time) {
                event = eventQ.poll();
                incCur[event[3]] += weightsTOrd.getRawData()[event[1]]
                        * Float.intBitsToFloat(event[2]);
                stdpRule.preTriggered(weightsTOrd, event, lastSpkTimes, dt);
                // TODO: Fix the event thing to make it not dependent on increment in wts mat, so that callers can apply their own offsets without having to know wts
                tOrdLastArrivals.setValue(event[1]/2, time, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes synapse events i.e. looks in the event queue for all events that arrive (or should have arrived)
     * at this time (given the time of the last pre-synaptic spike and the delay of the synapse)
     * and removed them from the event queue. Each event is then processed: meaning that the
     * appropriate amount of current (the current from the event is stored in the queue) is deposited
     * on the appropriate target neuron.
     *
     * @param eventQ the node local queue containing all synaptic events.
     * @param incCur the incoming currents to each of the neurons (local to a node) where each synapse's contribution is stored
     * @param time current simulation clock
     * @param dt integration time step
     */
    public void processEvents(PriorityBlockingQueue<int[]> eventQ, double[] incCur, double time, double dt) {
        int [] event = null;
        try {
            while (!eventQ.isEmpty() && eventQ.peek()[0] * dt <= time) {
                event = eventQ.poll();
                incCur[event[3]] += weightsTOrd.getRawData()[event[1]]
                        * Float.intBitsToFloat(event[2]);
                // TODO: Fix the event thing to make it not dependent on increment in wts mat, so that callers can apply their own offsets without having to know wts
                tOrdLastArrivals.setValue(event[1]/2, time, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inDegrees(final int[] inD) {
        for(int ii=0; ii<noTar; ++ii) {
            inD[ii] += weightsTOrd.getRawPtrs()[ii+1] - weightsTOrd.getRawPtrs()[ii];
        }
    }

    public void outDegrees(final int[] oD) {
        for(int ii=0; ii<weightsTOrd.getNnz(); ++ii) {
            oD[weightsTOrd.getRawOrdIndices()[ii]]++;
        }
    }

//    public void normWts(double[] normVals) {
//        weightsTOrd.mulFromArray(normVals, 0);
//    }

    public void scaleWts(double[] normVals) {
        weightsTOrd.mulFromArray(normVals, 0);
    }

    public void rightDivWts(double [] div) {
        weightsTOrd.divFromArray(div, 0);
    }

    public void scaleWeights(int noTar, double scale) {
        weightsTOrd.scaleMajor(noTar, scale, 0);
    }

    public double getIncomingSum(int noTar) {
       return weightsTOrd.getMajorSum(noTar, 0);
    }

    public void updateWeights() {
        weightsTOrd.addDw2W();
    }

    public double[] calcAndGetSums(double[] localWtSums) {
        weightsTOrd.sumIncoming(localWtSums, 0);
        return localWtSums;
    }

    public double getMaxWeight() {
        return weightsTOrd.getMax(0);
    }

    //public List<SrcTarDataPack> getTuples(Ordering )


    public void calcSpikeResponses(int srcInd) {

    }

    public void mhpStage1() {

    }

    public InterleavedSparseMatrix getWeightsTOrd() {
        return weightsTOrd;
    }

    public InterleavedSparseAddOn gettOrdLastArrivals() {
        return tOrdLastArrivals;
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
        ConnectSpecs cSpecs = new ConnectSpecs(ConnectRule.Random,
                new double[]{0.2}, Math.sqrt(30000), 20);
        MANAMatrix mm = new MANAMatrix(src, tar,
                Math.sqrt(30000), 20, cSpecs);
        double[] rawVals = mm.weightsTOrd.getRawData();
        for(int ii=0; ii<rawVals.length; ++ii) {
            rawVals[ii] =Math.ceil( Math.random() * 10);
        }

        System.out.println(Arrays.toString(mm.weightsTOrd.getPtrs()));
        System.out.println();
        System.out.println(Arrays.toString(mm.weightsTOrd.getOrdIndices()));
        System.out.println();
        System.out.println(Arrays.toString(mm.weightsTOrd.getValues()));

        int[] srcInd = new int[mm.nnz];
        int[] tarInd = new int[mm.nnz];
        double [] vals = new double[mm.nnz];

        mm.weightsTOrd.getInCOO(srcInd, tarInd, vals, 0);

        double[][] mat = new double[numN][numN];
        int[][] matC = new int[numN][numN];

        for(int ii=0; ii<mm.nnz; ++ii) {
            mat[srcInd[ii]][tarInd[ii]] = vals[ii];
            matC[srcInd[ii]][tarInd[ii]]++;
        }

        System.out.println();

        for(int ii=0; ii<numN; ++ii) {
            System.out.println(Arrays.toString(mat[ii]));
        }

        System.out.println();

        for(int ii=0; ii<numN; ++ii) {
            System.out.println(Arrays.toString(matC[ii]));
        }

        double[] sus = new double[10];
        mm.calcAndGetSums(sus);

        System.out.println();
        System.out.println(Arrays.toString(sus));

        for(int ii=0; ii<numN; ++ii) {
            mm.scaleWeights(ii, 1/sus[ii]);
        }

        System.out.println();
        System.out.println(Arrays.toString(mm.weightsTOrd.getPtrs()));
        System.out.println();
        System.out.println(Arrays.toString(mm.weightsTOrd.getOrdIndices()));
        System.out.println();
        System.out.println(Arrays.toString(mm.weightsTOrd.getValues()));

        srcInd = new int[mm.nnz];
        tarInd = new int[mm.nnz];
        vals = new double[mm.nnz];

        mm.weightsTOrd.getInCOO(srcInd, tarInd, vals, 0);

        mat = new double[numN][numN];
        matC = new int[numN][numN];

        for(int ii=0; ii<mm.nnz; ++ii) {
            mat[srcInd[ii]][tarInd[ii]] = vals[ii];
            matC[srcInd[ii]][tarInd[ii]]++;
        }
        System.out.println();

        for(int ii=0; ii<numN; ++ii) {
            System.out.println(Arrays.toString(mat[ii]));
        }


        COOManaMat coomana = new COOManaMat(mm, Ordering.TARGET);

        MANAMatrix convMana = new MANAMatrix(coomana, src, tar);

        System.out.println();
        System.out.println(Arrays.toString(convMana.weightsTOrd.getPtrs()));
        System.out.println();
        System.out.println(Arrays.toString(convMana.weightsTOrd.getOrdIndices()));
        System.out.println();
        System.out.println(Arrays.toString(convMana.weightsTOrd.getValues()));

        srcInd = new int[convMana.nnz];
        tarInd = new int[convMana.nnz];
        vals = new double[convMana.nnz];

        convMana.weightsTOrd.getInCOO(srcInd, tarInd, vals, 0);

        mat = new double[numN][numN];
        matC = new int[numN][numN];

        for(int ii=0; ii<mm.nnz; ++ii) {
            mat[srcInd[ii]][tarInd[ii]] = vals[ii];
            matC[srcInd[ii]][tarInd[ii]]++;
        }
        System.out.println();

        for(int ii=0; ii<numN; ++ii) {
            System.out.println(Arrays.toString(mat[ii]));
        }


        // TODO: Actually use JUnit instead of being lazy... so lazy!
        System.out.println("Dummy. It's a place for a breakpoint! :D");

    }


}
