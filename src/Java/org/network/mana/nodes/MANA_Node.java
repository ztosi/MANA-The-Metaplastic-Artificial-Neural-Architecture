package Java.org.network.mana.nodes;

import Java.org.network.mana.base_components.InputNeurons;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Matrices.COOManaMat;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.Matrices.InterleavedSparseMatrix;
import Java.org.network.mana.base_components.Matrices.SynapseMatrix;
import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.base_components.enums.DampFunction;
import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.execution.tasks.Updatable;
import Java.org.network.mana.functions.MHPFunctions;
import Java.org.network.mana.functions.STDP;
import Java.org.network.mana.functions.StructuralPlasticity;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.ConnectSpecs;
import Java.org.network.mana.utils.SrcTarPair;
import Java.org.network.mana.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MANA Java.org.network.mana.nodes are comprised of a set of source neurons (either input neurons or other MANA neurons) and target neurons
 * and contain all the weights and other relevant synapse data for the connections between that source and target. MANA
 * Java.org.network.mana.nodes are the work-horse and each one comprises a separate update task to be sent to a worker thread. They are
 * responsible for applying all weight changes (from STDP and normalization and scaling), as well as scheduling
 * all spikes based on source spikes and the delay along their synapses. Short term plasticity is also calulated here
 * as are the contributions of source neurons to meta homeostatic plasticity.
 *
 * @author Zoë Tosi
 */
public class MANA_Node implements Updatable {

    /** The sector this node belongs to/is managed by. Must have the same target neurons. */
    public final MANA_Sector parent_sector;

    /** Index among other Java.org.network.mana.nodes in the sector. */
    public int sector_index;

    private SynapseMatrix synMatrix;

    private InterleavedSparseAddOn pfrLoc;

    /**
     * Event (AP) source and the neurons (targets of APs/Synapses)
     *  whose afferent synapses we opperate on
     */
    public final SpikingNeuron srcData;

    /** The neurons whose inputs from the specific srcData this node handles,
     * most opperations are done on a by-target-node basis making them the primary
     * data this node works on and its primary basis of organization
     *  (perhaps alongside srcData, but srcData despite co-defining the node does
     *  not exert as much influence on its organization).
     */
    public final MANANeurons targData;

    /**
     * True if and only if the neurons that are the source of
     *  the synapses covered by this node are experimenter-driven
     * external input Java.org.network.mana.nodes (and therefore provide no contribution to
     *  meta-homeostatic plastcity)
     */
    public boolean inputIsExternal;

    /** True if and only if the synapses in this node are "trans-unit" meaning
     * they connect groups of neurons belonging to distinct MANA units. Biologically
     * these would be roughly equivalent to non-local white-matter synapses which
     * connect distant cortical columns.
     */
    public final boolean isTransUnit;

    /** Width-> number of target neurons; always same as sector width */
    public final int width;
    /**
     * Height -> number of source neurons, can be different from width
     *  and even other Java.org.network.mana.nodes in the same sector
     */
    public final int height;

    public boolean normalizationOn = true;

   // public final int srcOffset;

   // public final int tarOffset;

    private DampFunction dampener;

    /**
     *  The "type" of node this is in terms of its synapses,
     *   does it connect excitatory neurons to excitatory
     *   neurons or inhibitory neurons to excitatory neurons,
     *   and so on.
     */
    public final SynType type;

    /** The sum of all synaptic weights impinging on each target in this node (locally).*/
    private final double [] localSums;

    private final double [] locCurrents;

    private final double [] normVals;

    private final double [] sectorSums;

    private final BoolArray normFlags;

    public boolean synPlasticityOn = true;

    private STDP stdpRule;

    private boolean structureChanged = false;

    private PriorityBlockingQueue<int[]> evtQueue = new PriorityBlockingQueue<>(5000,
            (int[] a, int[] b) -> { // Sort by arrival time then absolute target index
        if(a[0] < b[0]) {
            return -1;
        } else if(a[0] == b[0]) {
            if(a[1] < b[1]) {
                return -1;
            } else if(a[1] > b[1]) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    });

    public static MANA_Node buildNodeAndConnections(MANA_Sector parent, SpikingNeuron srcNeu, MANANeurons tarNeu,
                                                    ConnectSpecs specs, DampFunction dampener,
                                                    STDP stdpRule, boolean isTransUnit) {
                                                     //int srcOffset, int tarOffset) {
        SynapseMatrix synMat = new SynapseMatrix(srcNeu, tarNeu,
                specs.maxDist, specs.maxDly, specs);
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit, synMat);
        tmp.synMatrix = synMat;
        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node buildNodeFromCOO(MANA_Sector parent, SpikingNeuron srcNeu, MANANeurons tarNeu,
                                             COOManaMat cooMat, DampFunction dampener, STDP stdpRule,
                                             boolean isTransUnit) {
        SynapseMatrix  synMat= new SynapseMatrix(cooMat, srcNeu, tarNeu);
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit, synMat);

        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node buildNodeFromMatrix(MANA_Sector parent, SpikingNeuron srcNeu, MANANeurons tarNeu,
                                                SynapseMatrix synMatrix, DampFunction dampener, STDP stdpRule,
                                                boolean isTransUnit)  {
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit, synMatrix);
        tmp.synMatrix = synMatrix;
        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    private MANA_Node(SpikingNeuron srcNeu, MANANeurons tarNeu, MANA_Sector parent, boolean isTransUnit,
                      SynapseMatrix synMat)  {
        this.parent_sector = parent;
        this.srcData = srcNeu;
        this.targData = tarNeu;
        this.isTransUnit = isTransUnit;
        this.type = synMat.type;
        this.synMatrix = synMat;
        normVals = srcNeu.isExcitatory() ? targData.normValsExc : targData.normValsInh;
        normFlags = srcNeu.isExcitatory() ? targData.excSNon : targData.inhSNon;
        sectorSums = srcNeu.isExcitatory() ? parent.secExcSums : parent.secInhSums;
        height = srcNeu.getSize();
        width = tarNeu.getSize();
        inputIsExternal = srcData instanceof InputNeurons;
        locCurrents = new double[width];
        localSums = new double[width];
        int [] inD = srcNeu.isExcitatory() ? tarNeu.excInDegree : tarNeu.inhInDegree;
        accumInDegrees(inD);
        accumInDegrees(tarNeu.inDegree);
        accumOutDegrees(srcNeu.getOutDegree());
    }


    public void structuralPlasticity(int maxInD, int maxOutD, double lambda, double maxDist, double time) {
        double max = srcData.isExcitatory() ?
                parent_sector.parent.getMaxExcLazy(time) :
                parent_sector.parent.getMaxInhLazy(time);
        synMatrix = StructuralPlasticity.pruneGrow(this, srcData, targData, maxOutD, maxInD,
                SynType.getLambdaBase(srcData.isExcitatory(), targData.isExcitatory(), lambda),
                SynType.getConProbBase(srcData.isExcitatory(),
                        targData.isExcitatory())/2, maxDist, time, max, srcData.isExcitatory() ? targData.exc_sf
                        : targData.inh_sf);
        pfrLoc = new InterleavedSparseAddOn(synMatrix.getWeightsTOrd(), 1);
        for(int[] evt : evtQueue) {
            evt[1]=-1; // invalidate
        }
        //evtQueue.clear(); // TODO: This is very bad! Figure out a better way!
        structureChanged = true;
    }

    private HashMap<SrcTarPair, List<int[]>> keyEvents() {
        HashMap<SrcTarPair, List<int[]>> eventDict = new HashMap<SrcTarPair, List<int[]>>();



        return eventDict;
    }

    public SynapseMatrix getSynMatrix() {
        return  synMatrix;
    }

//    public void removeEvent(int absTarOrdIndex) {
//        evtQueue.stream().
//        Iterator<int[]> evtIterator = evtQueue.iterator();
//        while(evtIterator.hasNext()) {
//            int[] evt = evtIterator.next();
//            if()
//        }
//    }

    public AtomicBoolean updated = new AtomicBoolean(false);

    /**
     * Perform all node level updates, including processing arriving action
     * potentials (including UDF short term plasticity and pre- triggered STDP),
     * processing all action potentials in member neurons (perform post-STDP
     * on the afferents within this node), adding all changes in weights to their
     * respective weights, reporting the new tota
     *
     * @param time
     * @param dt
     */
    public void update(final double time, final double dt) {
        if(!updated.compareAndSet(false, true)) {
            throw new IllegalStateException("Multiple threads trying to update the same node");
        }

        if(!parent_sector.isInitialized()) {
            throw  new IllegalStateException("Node updates cannot be performed until initialization has been done on parent sector.");
        }



        // Check for pre-synaptic spikes, schedule the events along synapses of neurons that have,
        try {
            for (int ii = 0; ii < height; ++ii) {
                if (srcData.getSpikes().get(ii)) {
                    synMatrix.calcSpikeResponses(ii, time);
                    synMatrix.addEvents(ii, time, dt, evtQueue);
                }
            }


            if (synPlasticityOn) {

                // Synaptic normalization & scaling
                if (normalizationOn && time > 20000) {
                //    if (!targData.getAllNrmOn(srcData.isExcitatory())) {
                        for (int ii = 0; ii < width; ++ii) {
                            if (normFlags.get(ii)) {
                                synMatrix.scaleWeights(ii, normVals[ii] / sectorSums[ii]);
                            }
                        }
                    synMatrix.clip(20, 0);
//                    } else {
//                        for (int ii = 0; ii < width; ++ii) {
//                            synMatrix.scaleWeights(ii, normVals[ii] / sectorSums[ii]);
//                        }
//                    }
                }

                // Calculate new dws for synapses tied to arriving events, add their currents to the correct target
                synMatrix.processEventsSTDP(evtQueue, locCurrents, stdpRule,
                        targData.lastSpkTime, time, dt, srcData.isExcitatory(), targData.isExcitatory());

                // Check for post-synaptic spikes and adjust synapses incoming to them accordingly.
                for (int ii = 0; ii < width; ++ii) {
                    if (targData.getSpikes().get(ii)) {
                        stdpRule.postTriggered(synMatrix.getWeightsTOrd(),
                                synMatrix.gettOrdLastArrivals(), ii, time);
                    }
                }
            } else {
                synMatrix.processEvents(evtQueue, locCurrents, time, dt);
            }

            dampener.dampen(synMatrix.getWeightsTOrd().getRawData(), 20, 0);
            // If dampening is used, dampen
            //    dampener.dampen(synMatrix.getWeightsTOrd().getRawData(), SynapseData.MAX_WEIGHT, 0);
            // Add dws to ws--update synaptic weights
            synMatrix.updateWeights();
            //synMatrix.getWeightsTOrd().

            if (normalizationOn) {
                synMatrix.calcAndGetSums(localSums);
            }

            if (targData.getMhpOn() && (!inputIsExternal
//                    && ((srcData.isExcitatory() && targData.isExcitatory())
//                    || (!srcData.isExcitatory() && !targData.isExcitatory()))
                  //  && !(targData.allInhSNon && targData.allExcSNon)
                     && srcData instanceof MANANeurons)) { //&& (srcData.isExcitatory()==targData.isExcitatory())) {
                   // && srcData.isExcitatory()) {
            //    if((int)(time/dt) % (int)(1/dt) == 0) {
                    for (int ii = 0; ii < width; ++ii) {
//                        if(!(targData.excSNon.get(ii) && targData.inhSNon.get(ii)) ) {
                          //  if (!(targData.excSNon.get(ii) && targData.inhSNon.get(ii))) {
                                MHPFunctions.mhpStochastic_1((MANANeurons) srcData, targData, ii, pfrLoc);
//                                MHPFunctions.mhpStage1(targData.estFR, targData.prefFR, ((MANANeurons) srcData).estFR, ii,
//                                        pfrLoc, srcData.isExcitatory());
//                                MHPFunctions.mhpStage2(ii, MHPFunctions.getFp(targData.fVals[ii]),
//                                        MHPFunctions.getFm(targData.fVals[ii]), pfrLoc);
                          //  }
//                            }
              //      }
                }
//            for(int ii=0; ii<width; ++ii) {
//                if(!(targData.excSNon.get(ii) && targData.inhSNon.get(ii)))
//                    MHPFunctions.mhpStage1(ii, pfrLoc);
//            }
//            for(int ii=0; ii<width; ++ii) {
//                if(!(targData.excSNon.get(ii) && targData.inhSNon.get(ii)))
//                    MHPFunctions.mhpStage2(ii, MHPFunctions.getFp(targData.fVals[ii]),
//                            MHPFunctions.getFm(targData.fVals[ii]), pfrLoc);
//            }
            }

            // Last thread working on a node in the sector has to update the sector...
    //            System.out.println(parent_sector.countDown.get());
            synchronized (parent_sector.countDown) {
                if (parent_sector.countDown.decrementAndGet() == 0) {
                    parent_sector.update(time, dt);
                } else if (parent_sector.countDown.get() < 0) {
                    throw new IllegalStateException("Sector countdown can never be less than 0.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int[] getLocalInDegrees() {
        int [] inD = new int[width];
        synMatrix.inDegrees(inD);
        return inD;
    }

    public void randomizeWeights(Utils.ProbDistType pdist, double[] params) {
        synMatrix.getWeightsTOrd().randomize(pdist, params, 0);
    }

    public boolean getStructureChanged() {
        return  structureChanged;
    }

    public void structureChangedOff() {
        structureChanged = false;
    }

    public void accumInDegrees(final int [] inDs) {
            synMatrix.inDegrees(inDs);
    }

    public void accumOutDegrees(final int [] oDs) {
        synMatrix.outDegrees(oDs);
    }

    public void accumulatePFRSums(final double[] pfrDt) {
        if(srcData instanceof InputNeurons) { //|| !srcData.isExcitatory() || !targData.isExcitatory()) {
            return;
        }
//        if((srcData.isExcitatory() && targData.isExcitatory())
////                || (!srcData.isExcitatory() && !targData.isExcitatory())) {
////            return;
////        }
//        if(!srcData.isExcitatory()) {
////            for(int ii=0, n = pfrDt.length; ii<n; ++ii) {
////                pfrDt[ii] *= 5;
////            }
//            //pfrLoc.scale(5, 0);
//            for(int ii=0; ii<pfrLoc.values.length; ++ii) {
//                if(pfrLoc.values[ii] > 0) {
//                    pfrLoc.values[ii] = 0;
//                } else {
//                    pfrLoc.values[ii] *= 4;
//                }
//            }
//        }
//        if(srcData.isExcitatory()) {
//            for(int ii=0; ii<pfrLoc.values.length; ++ii) {
//                if(pfrLoc.values[ii] < 0) {
//                    pfrLoc.values[ii] = 0;
//                }
//            }
//        }

//        if((srcData.isExcitatory() && !targData.isExcitatory())
//        || (!srcData.isExcitatory() && targData.isExcitatory())) {
////            for(int ii=0, n = pfrDt.length; ii<n; ++ii) {
////                pfrDt[ii] *= -1;
////            }
//            pfrLoc.scale(-1, 0);
//        }
//        if(srcData.isExcitatory() != targData.isExcitatory()) {
//            double pfConst = 0.5;
////            if(srcData.isExcitatory()) {
////                pfConst = 0.2;
////            } else {
////                pfConst = 0.2;
////            }
//            for(int ii=0; ii<pfrLoc.values.length; ++ii) {
//                pfrLoc.values[ii] *= pfConst;
//            }
//        }
        pfrLoc.accumSums(pfrDt, 0);
    }

    public void accumulateLocalWtSums(final double[] sectorSums){
        for(int ii=0; ii< width; ++ii) {
            sectorSums[ii] += localSums[ii];
        }
    }

    public void addAndClearLocCurrent(final double[] neuronCurrents) {
        for(int ii=0; ii<width; ++ii) {
            neuronCurrents[ii] += locCurrents[ii];
            locCurrents[ii] = 0;
        }
    }

    public double[] calcLocalSums() {
        return synMatrix.calcAndGetSums(localSums);
    }

    public double[] calcAndGetWtSums(double [] ret) {
        return synMatrix.calcAndGetSums(ret);
    }

    public double[] calcAndAccumWtSums(double [] ret) {
       synMatrix.calcAndGetSums(localSums);
       for(int ii=0; ii<width; ++ii) {
           ret[ii] += localSums[ii];
       }
       return ret;
    }

    public int getNNZ() {
        return synMatrix.getWeightsTOrd().getNnz();
    }

    public InterleavedSparseMatrix getWeightMatrix() {
        return synMatrix.getWeightsTOrd();
    }

    public void getWeightValues(double[] vals, int absShift) {
        synMatrix.getWeightsTOrd().getValues(vals, absShift, 0);
    }

    @Override
    public void setUpdated(boolean updated) {
        this.updated.set(updated);
    }

    @Override
    public boolean isUpdated() {
        return updated.get();
    }
}

