package nodes;

import base_components.*;
import base_components.Matrices.COOManaMat;
import base_components.Matrices.MANAMatrix;
import base_components.enums.DampFunction;
import base_components.enums.SynType;
import functions.STDP;
import utils.ConnectSpecs;

import java.util.PriorityQueue;

public class MANA_Node2 {

    /** The sector this node belongs to/is managed by. Must have the same target neurons. */
    public MANA_Sector parent_sector;

    /** Index among other nodes in the sector. */
    public int sector_index;

    private MANAMatrix synMatrix;

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

    /**
     * Can be used to selectively disable the contributions of the
     * source neurons for this node to the Meta-homeostatic plasticity
     * calculations for the target neurons.
     */
    public boolean useMHP=true;

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

    private DampFunction dampener;

    /**
     *  The "type" of node this is in terms of its synapses,
     *   does it connect excitatory neurons to excitatory
     *   neurons or inhibitory neurons to excitatory neurons,
     *   and so on.
     */
    public final SynType type;

    /** The sum of all synaptic weights impinging on each target in this node (locally).*/
    public double [] localSums;

    public double [] locCurrents;

    public double[] localPFRDep;

    private STDP stdpRule;

    public int[] localInDegrees;
    public int[] localOutDegrees;

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

    public static MANA_Node2 buildNode(Neuron srcNeu, MANANeurons tarNeu, ConnectSpecs specs,
                                       DampFunction dampener, STDP stdpRule, boolean isTransUnit,
                                       int tarOffset, int srcOffset) {
        MANAMatrix synMat = new MANAMatrix(srcOffset, tarOffset, srcNeu, tarNeu,
                specs.maxDist, specs.maxDly, specs.rule, specs.parms);
        MANA_Node2 tmp = new MANA_Node2(srcNeu, tarNeu, synMat, stdpRule, isTransUnit);
        tmp.dampener = dampener;
        return tmp;
    }

    public static MANA_Node2 buildNode(Neuron srcNeu, MANANeurons tarNeu, COOManaMat cooMat,
                                       DampFunction dampener, STDP stdpRule, boolean isTransUnit,
                                       int tarOffset, int srcOffset) {
        MANAMatrix synMat = new MANAMatrix(srcOffset, tarOffset, cooMat, srcNeu, tarNeu);
        MANA_Node2 tmp = new MANA_Node2(srcNeu, tarNeu, synMat, stdpRule, isTransUnit);
        tmp.dampener = dampener;
        return tmp;
    }


    public MANA_Node2(Neuron srcNeu, MANANeurons tarNeu, MANAMatrix synMatrix, STDP stdpRule,
                      boolean isTransUnit)  {
        this.srcData = srcNeu;
        this.targData = tarNeu;
        this.synMatrix = synMatrix;
        this.isTransUnit = isTransUnit;
        type = synMatrix.type;
        height = srcNeu.getSize();
        width = tarNeu.getSize();
        inputIsExternal = srcData instanceof  InputNeurons;
        this.stdpRule = stdpRule;
        initBasic();
    }


    /**
     * Initializes all the basic values that don't really change depending on the constructor.
     */
    private void initBasic() {
        localOutDegrees = new int[height];
        localInDegrees = new int[width];
        // Initializing all the 1-D arrays (representing source or target data...)
        localSums = new double[width];
        localPFRDep = new double[width];
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

        for(int ii=0; ii<height; ++ii) {
            if(srcData.getSpikes()[ii]) {
                synMatrix.spike(ii, time);
                synMatrix.addEvents(ii, time, dt, evtQueue);
                stdpRule.postTriggered(ii, time);
            }
        }

        synMatrix.processEventsSTDP(evtQueue, locCurrents, stdpRule,
                targData.lastSpkTime, time, dt);

        synMatrix.updateWeights();

        

    }








}

