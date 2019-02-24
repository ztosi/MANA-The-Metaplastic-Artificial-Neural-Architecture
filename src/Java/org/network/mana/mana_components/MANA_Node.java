package Java.org.network.mana.mana_components;

import Java.org.network.mana.base_components.neurons.InputNeurons;
import Java.org.network.mana.base_components.neurons.Neuron;
import Java.org.network.mana.base_components.sparse.InterleavedSparseAddOn;
import Java.org.network.mana.base_components.sparse.InterleavedSparseMatrix;
import Java.org.network.mana.base_components.synapses.ConnectSpecs;
import Java.org.network.mana.base_components.synapses.STDP;
import Java.org.network.mana.enums.ConnectRule;
import Java.org.network.mana.enums.SynapseType;
import Java.org.network.mana.exec.Updatable;
import Java.org.network.mana.functions.MHPFunctions;
import Java.org.network.mana.functions.StructuralPlasticity;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.Utils;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MANA_Nodes are comprised of a set of source neurons (either input neurons or other MANA neurons) and target neurons
 * and contain all the weights and other relevant synapse data for the connections between that source and target. MANA
 * Java.org.network.exec.mana_components are the work-horse and each one comprises a separate update task to be sent to a worker thread. They are
 * responsible for applying all weight changes (from STDP and normalization and scaling), as well as scheduling
 * all spikes based on source spikes and the delay along their synapses. Short term plasticity is also calulated here
 * as are the contributions of source neurons to meta homeostatic plasticity.
 *
 * @author Zoë Tosi
 */
public class MANA_Node implements Updatable {

    /** The sector this node belongs to/is managed by. Must have the same target neurons. */
    public final MANA_Sector parent_sector;

    /**
     * Stores all the data associated with executing MANA Updates. Holds onto and keeps properly arranged, weights,
     * short term plasticity parameters, etc.
     */
    private MANAMatrix synMatrix;

    /**
     * Holds onto the meta homeostatic contributions of each of the incoming neurons to the target neuron's target
     * firing rate.
     */
    private InterleavedSparseAddOn pfrLoc;

    /**
     * Event (AP) source and the neurons (targets of APs/synapses)
     *  whose afferent synapses we opperate on
     */
    public final Neuron srcData;

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
     * external input Java.org.network.exec.mana_components (and therefore provide no contribution to
     *  meta-homeostatic plastcity)
     */
    public final boolean inputIsExternal;

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
     *  and even other Java.org.network.exec.mana_components in the same sector
     */
    public final int height;

    /** Whether or not synaptic normalization is turned on. */
    public boolean normalizationOn = true;

    /**
     *  The "type" of node this is in terms of its synapses,
     *   does it connect excitatory neurons to excitatory
     *   neurons or inhibitory neurons to excitatory neurons,
     *   and so on.
     */
    public final SynapseType type;

    /** The sum of all synaptic weights impinging on each target in this node (locally).*/
    private final double [] localSums;

    /**
     * Sum of all currents to each target neuron from all sources on a given update. This is where you accumulate
     * local inputs to each neuron.
     * */
    private final double [] locCurrents;

    /**
     * The normalization values of all the target neurons. I.e. the number, for each neuron that each incoming weight
     * should be normalized to.
     */
    private final double [] normVals;

    /** Keeps track of who has hit their norm value and thus is subject to synaptic normalization. **/
    private final BoolArray normFlags;

    public boolean synPlasticityOn = true;

    private STDP stdpRule;

    /**
     * Whether or not stuctural plasticity has changed the connectivity structure of the synapses associated with this
     * node.
     */
    private boolean structureChanged = false;

    /**
     * Keeps track of whether or not the node has been updated during this iteration.
     */
    public AtomicBoolean updated = new AtomicBoolean(false);

    /**
     * All spikes produced by inputs are transformed into all the data necessary for the target to do whatever
     * operations it needs to. That data is stored here, which ensures that spike events to be received by targets
     * are sorted by arrival time and then target neuron index (no sense in jumping all over the place).
     */
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

    /**
     * Builds a mana node connecting a source and target neuron set.
     * @param parent -- the parents sector within which this node resides
     * @param srcNeu source neurons
     * @param tarNeu target neurons -- must be a MANANeuron
     * @param specs How to go about connecting the neurons in this group initially.
     * @param stdpRule the STDP rule to use on the synapses
     * @param isTransUnit - Whether or not this node connects srcs and targs that reside in different MANA_Units
     * @return
     */
    public static MANA_Node buildNodeAndConnections(MANA_Sector parent, Neuron srcNeu, MANANeurons tarNeu,
                                                    ConnectSpecs specs, STDP stdpRule, boolean isTransUnit) {
        //int srcOffset, int tarOffset) {
        MANAMatrix synMat = new MANAMatrix(srcNeu, tarNeu,
                specs.maxDist, specs.maxDly, specs);
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit, synMat.type);
        tmp.synMatrix = synMat;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node buildNodeFromCOO(MANA_Sector parent, Neuron srcNeu, MANANeurons tarNeu,
                                             COOManaMat cooMat, STDP stdpRule,
                                             boolean isTransUnit) {
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit,
                SynapseType.getSynType(srcNeu.isExcitatory(), tarNeu.isExcitatory()));
        tmp.synMatrix = new MANAMatrix(cooMat, srcNeu, tarNeu);
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node buildNodeFromMatrix(MANA_Sector parent, Neuron srcNeu, MANANeurons tarNeu,
                                                MANAMatrix synMatrix, STDP stdpRule,
                                                boolean isTransUnit)  {
        MANA_Node tmp = new MANA_Node(srcNeu, tarNeu, parent, isTransUnit, synMatrix.type);
        tmp.synMatrix = synMatrix;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new InterleavedSparseAddOn(synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    private MANA_Node(Neuron srcNeu, MANANeurons tarNeu, MANA_Sector parent, boolean isTransUnit,
                      SynapseType type)  {
        this.parent_sector = parent;
        this.srcData = srcNeu;
        this.targData = tarNeu;
        this.isTransUnit = isTransUnit;
        this.type = type;
        normVals = srcNeu.isExcitatory() ? targData.normValsExc : targData.normValsInh;
        normFlags = srcNeu.isExcitatory() ? targData.excSNon : targData.inhSNon;
        height = srcNeu.getSize();
        width = tarNeu.getSize();
        inputIsExternal = srcData instanceof InputNeurons;
        locCurrents = new double[width];
        localSums = new double[width];
    }


    /**
     * Perform all node level updates, including processing arriving action
     * potentials (including UDF short term plasticity and pre- triggered STDP),
     * processing all action potentials in member neurons (perform post-STDP
     * on the afferents within this node), adding all changes in weights to their
     * respective weights, reporting the new totals
     *
     * @param time
     * @param dt
     */
    @Override public void update(final double time, final double dt) {
        if(!updated.compareAndSet(false, true)) {
            throw new IllegalStateException("Multiple threads trying to update the same node");
        }

        if(!parent_sector.isInitialized()) {
            throw  new IllegalStateException("Node updates cannot be performed until initialization has been done on parent sector.");
        }

        // Check for pre-synaptic spikes, schedule the events along synapses of neurons that have,
        for (int ii = 0; ii < height; ++ii) {
            if (srcData.getSpikes().get(ii)) {
                synMatrix.calcSpikeResponses(ii, time);
                synMatrix.addEvents(ii, time, dt, evtQueue);
            }
        }

        if (synPlasticityOn) {
            double [] sectorSums = parent_sector.getSectorSums(srcData.isExcitatory());
            // Synaptic normalization & scaling
            if (normalizationOn) {
                if (!targData.getAllNrmOn(srcData.isExcitatory())) {
                    for (int ii = 0; ii < width; ++ii) {
                        if (normFlags.get(ii)) {
                            synMatrix.scaleWeights(ii, normVals[ii] / sectorSums[ii]);
                        }
                    }
                } else {
                    for (int ii = 0; ii < width; ++ii) {
                        synMatrix.scaleWeights(ii, normVals[ii] / sectorSums[ii]);
                    }
                }
            }

            // Calculate new dws for synapses tied to arriving events, add their currents to the correct target
            synMatrix.processEventsSTDP(evtQueue, locCurrents, stdpRule,
                    targData.getLastSpkTimes(), time, dt);

            // Check for post-synaptic spikes and adjust synapses incoming to them accordingly.
            for (int ii = 0; ii < width; ++ii) {
                if (targData.getSpikes().get(ii)) {
                    stdpRule.postTriggered(synMatrix.getWeightsTOrd(),
                            synMatrix.gettOrdLastArrivals(), ii, time, dt);
                }
            }
        } else {
            synMatrix.processEvents(evtQueue, locCurrents, time, dt);
        }

        // Add dws to ws--update synaptic weights
        synMatrix.updateWeights();

        if (normalizationOn) {
            synMatrix.calcAndGetSums(localSums);
        }

        if (!inputIsExternal && targData.mhpOn
                && !(targData.allInhSNon && targData.allExcSNon)) { //&& (srcData.isExcitatory()==targData.isExcitatory())) {
            //    if((int)(time/dt) % (int)(1/dt) == 0) {
            for (int ii = 0; ii < width; ++ii) {
                if(!(targData.excSNon.get(ii) && targData.inhSNon.get(ii)) ) {
                    if (!(targData.excSNon.get(ii) && targData.inhSNon.get(ii))) {
                        MHPFunctions.mhpStage1(targData.estFR, targData.prefFR, ((MANANeurons) srcData).estFR, ii,
                                pfrLoc, srcData.isExcitatory());
                        MHPFunctions.mhpStage2(ii, MHPFunctions.getFp(targData.fVals[ii]),
                                MHPFunctions.getFm(targData.fVals[ii]), pfrLoc);
                    }
                }
                //      }
            }
        }

        // Last thread working on a node in the sector has to update the sector...
        if (parent_sector.countDown.decrementAndGet() == 0) {
            parent_sector.update(time, dt);
        } else if (parent_sector.countDown.get() < 0) {
            throw new IllegalStateException("Sector countdown can never be less than 0.");
        }

    }

    /**
     * Adds and removes synapses according to a set of rules. This is a very expensive operation because the sparse matrix
     * objects backing this node have to be completely rebuilt.
     * @param maxInD
     * @param maxOutD
     * @param lambda
     * @param maxDist
     * @param time - simulation time
     */
    public void structuralPlasticity(int maxInD, int maxOutD, double lambda, double maxDist, double time) {
        double max = StructuralPlasticity.pruneTechnique == StructuralPlasticity.SPTechnique.GLOBAL_MAX ?
        parent_sector.parent.getMaxofType(time, srcData.isExcitatory(), targData.isExcitatory()): 0; // TODO: Lol at this convoluted nonsense
        synMatrix = StructuralPlasticity.pruneGrow(this, srcData, targData, maxOutD, maxInD,
                lambda, ConnectRule.getConProbBase(srcData.isExcitatory(),
                        targData.isExcitatory())/2, maxDist, time, max);
        pfrLoc = new InterleavedSparseAddOn(synMatrix.getWeightsTOrd(), 1);
        evtQueue.clear(); // TODO: This is very bad! Figure out a better way!
        structureChanged = true;
    }

// TODO
//    public void removeEvent(int absTarOrdIndex) {
//        evtQueue.stream().
//        Iterator<int[]> evtIterator = evtQueue.iterator();
//        while(evtIterator.hasNext()) {
//            int[] evt = evtIterator.next();
//            if()
//        }
//    }


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

    public MANAMatrix getSynMatrix() {
        return  synMatrix;
    }

    public boolean isUpdated() {
        return updated.get();
    }

}

