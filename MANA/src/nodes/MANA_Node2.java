package nodes;

import base_components.*;
import base_components.Matrices.COOManaMat;
import base_components.Matrices.MANAMatrix;
import base_components.Matrices.SynMatDataAddOn;
import base_components.enums.DampFunction;
import base_components.enums.SynType;
import functions.MHPFunctions;
import functions.STDP;
import functions.StructuralPlasticity;
import utils.BoolArray;
import utils.ConnectSpecs;

import java.util.PriorityQueue;

public class MANA_Node2 {

    /** The sector this node belongs to/is managed by. Must have the same target neurons. */
    public final MANA_Sector2 parent_sector;

    /** Index among other nodes in the sector. */
    public int sector_index;

    private MANAMatrix synMatrix;

    private SynMatDataAddOn pfrLoc;

    /**
     * Event (AP) source and the neurons (targets of APs/Synapses)
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
     * external input nodes (and therefore provide no contribution to
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
     *  and even other nodes in the same sector
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

    private boolean allNrmOn = false;
    private boolean allMHPOff = true;

    private PriorityQueue<int[]> evtQueue = new PriorityQueue<>((int[] a, int[] b) -> { // Sort by arrival time then absolute target index
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

    public static MANA_Node2 buildNodeAndConnections(MANA_Sector2 parent, Neuron srcNeu, MANANeurons tarNeu,
                                                     ConnectSpecs specs, DampFunction dampener,
                                                     STDP stdpRule, boolean isTransUnit) {
                                                     //int srcOffset, int tarOffset) {
        MANAMatrix synMat = new MANAMatrix(srcNeu, tarNeu,
                specs.maxDist, specs.maxDly, specs.rule, specs.parms);
        MANA_Node2 tmp = new MANA_Node2(srcNeu, tarNeu, parent, isTransUnit);
        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new SynMatDataAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node2 buildNodeFromCOO(MANA_Sector2 parent, Neuron srcNeu, MANANeurons tarNeu,
                                              COOManaMat cooMat, DampFunction dampener, STDP stdpRule,
                                              boolean isTransUnit) {
        MANA_Node2 tmp = new MANA_Node2(srcNeu, tarNeu, parent, isTransUnit);
        tmp.synMatrix = new MANAMatrix(cooMat, srcNeu, tarNeu);
        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new SynMatDataAddOn(tmp.synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    public static MANA_Node2 buildNodeFromMatrix(MANA_Sector2 parent, Neuron srcNeu, MANANeurons tarNeu,
                                                 MANAMatrix synMatrix, DampFunction dampener, STDP stdpRule,
                                                 boolean isTransUnit)  {
        MANA_Node2 tmp = new MANA_Node2(srcNeu, tarNeu, parent, isTransUnit);
        tmp.synMatrix = synMatrix;
        tmp.dampener = dampener;
        tmp.stdpRule = stdpRule;
        tmp.pfrLoc = new SynMatDataAddOn(synMatrix.getWeightsTOrd(), 1);
        return tmp;
    }

    private MANA_Node2(Neuron srcNeu, MANANeurons tarNeu, MANA_Sector2 parent, boolean isTransUnit)  {
        this.parent_sector = parent;
        this.srcData = srcNeu;
        this.targData = tarNeu;
        this.isTransUnit = isTransUnit;
        normVals = tarNeu.isExcitatory() ? targData.normValsExc : targData.normValsInh;
        normFlags = tarNeu.isExcitatory() ? parent.snExcOn : parent.snInhOn;
        sectorSums = tarNeu.isExcitatory() ? parent.secExcSums : parent.secInhSums;
        type = synMatrix.type;
        height = srcNeu.getSize();
        width = tarNeu.getSize();
        inputIsExternal = srcData instanceof  InputNeurons;
        locCurrents = new double[width];
        localSums = new double[width];
    }


    public void structuralPlasticity(int maxInD, int maxOutD, double lambda, double maxDist, double time) {
        synMatrix = StructuralPlasticity.pruneGrow(synMatrix, srcData, targData, maxOutD, maxInD,
                lambda, SynType.getConProbBase(srcData.isExcitatory(),
                        targData.isExcitatory()), maxDist, time);
        pfrLoc = new SynMatDataAddOn(synMatrix.getWeightsTOrd(), 1);
    }


    /**
     * Perform all node level updates, including processing arriving action
     * potentials (including UDF short term plasticity and pre- triggered STDP),
     * processing all action potentials in member neurons (perform post-STDP
     * on the afferents within this node), adding all changes in weights to their
     * respective weights, reporting the new tota
     * @param time
     * @param dt
     */
    public void update(final double time, final double dt) {

        // Check for pre-synaptic spikes, schedule the events along synapses of neurons that have,
        for(int ii=0; ii<height; ++ii) {
            if(srcData.getSpikes().get(ii)) {
                synMatrix.calcSpikeResponses(ii, time);
                synMatrix.addEvents(ii, time, dt, evtQueue);
            }
        }

        if (synPlasticityOn) {

            // Synaptic normalization & scaling
            if(normalizationOn) {
                if (!allNrmOn) {
                    allNrmOn = true;
                    for (int ii = 0; ii < width; ++ii) {
                        allNrmOn &= normFlags.get(ii);
                        if (normFlags.get(ii)) {
                            synMatrix.scaleWeights(ii, normVals[ii]/sectorSums[ii]);
                        }
                    }
                } else {
                    for (int ii = 0; ii < width; ++ii) {
                        synMatrix.scaleWeights(ii, normVals[ii]/sectorSums[ii]);
                    }
                }
            }

            // Calculate new dws for synapses tied to arriving events, add their currents to the correct target
            synMatrix.processEventsSTDP(evtQueue, locCurrents, stdpRule,
                    targData.lastSpkTime, time, dt);

            // Check for post-synaptic spikes and adjust synapses incoming to them accordingly.
            for(int ii=0; ii<width; ++ii) {
                if (targData.getSpikes().get(ii)) {
                    stdpRule.postTriggered(synMatrix.getWeightsTOrd(),
                            synMatrix.gettOrdLastArrivals(), ii, time);
                }
            }
        } else {
            synMatrix.processEvents(evtQueue, locCurrents, time, dt);
        }

        // If dampening is used, dampen
        dampener.dampen(synMatrix.getWeightsTOrd().getRawData(), SynapseData.MAX_WEIGHT, 0);
        // Add dws to ws--update synaptic weights
        synMatrix.updateWeights();

        if (normalizationOn) {
            synMatrix.calcAndGetSums(localSums);
        }

        if(!inputIsExternal && targData.mhpOn) {
            for(int ii=0; ii<width; ++ii) {
                MHPFunctions.mhpStage0(targData.estFR, targData.prefFR, ((MANANeurons)srcData).estFR, ii, pfrLoc);
            }
            for(int ii=0; ii<width; ++ii) {
                MHPFunctions.mhpStage1(ii, pfrLoc);
            }
            for(int ii=0; ii<width; ++ii) {
                MHPFunctions.mhpStage2(ii, MHPFunctions.getFp(targData.fVals[ii]),
                        MHPFunctions.getFm(targData.fVals[ii]), pfrLoc);
            }
        }

        // Last thread working on a node in the sector has to update the sector...
        if(parent_sector.countDown.decrementAndGet() == 0) {
            parent_sector.update(time, dt);
        }

    }

    public int[] getLocalInDegrees() {
        int [] inD = new int[width];
        synMatrix.inDegrees(inD);
        return inD;
    }

    public void accumInDegrees(final int [] inDs) {
        synMatrix.inDegrees(inDs);
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

}

