package Java.org.network.mana.nodes;

import Java.org.network.mana.base_components.InputNeurons;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.Neuron;
import Java.org.network.mana.base_components.enums.DampFunction;
import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.functions.STDP;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.ConnectSpecs;
import Java.org.network.mana.utils.SpikeTimeData;
import Java.org.network.mana.utils.Syncable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Java.org.network.mana.mana sector consists of a set of target MANA neurons and all the MANA Java.org.network.mana.nodes connecting that target to a source.
 * It is responsible for updating the target neurons as well as applying any operations requiring knowledge of
 * ALL incoming connections and/or source neurons that cannot be performed at the node-level. This includes things
 * like synaptic normalization. A sector update can only occur once all child node updates have completed and a sector
 * synchronize can only occur when all sectors have completed their updates. A sector synchronize consists of pushing
 * all values in buffers to their respective main locations. That is, all data in the target neurons which other
 * Java.org.network.mana.nodes/sectors might still be using.
 *
 * @author Zoë Tosi
 */
public class MANA_Sector implements Syncable {

    public double [] secExcSums;
    public double [] secInhSums;
    public BoolArray snExcOn;
    public BoolArray snInhOn;
    public double [] pfrAccum;
    public BoolArray spkBuffer;
    public final String id;

    public boolean synPlasticityOn = true;

    public SpikeTimeData spkDat;

    public final AtomicInteger countDown;

    public final MANANeurons target;

    private boolean initialized = false;

    public final Map<Neuron, MANA_Node> childNodes = new TreeMap<Neuron, MANA_Node>(
            (Neuron a, Neuron b) -> {
                if (a==b) {
                    return 0;
                }
                if (a.getID() < b.getID()) {
                    return  -1;
                } else if (a.getID() > b.getID()) {
                    return 1;
                } else if (a.getID() == b.getID()) {
                    throw new IllegalStateException("Either multiple neuron groups have been assigned " +
                            "the same ID or you are trying to add a " +
                            "source neuron group which already exists here.");
                } else {
                    return 0;
                }
            });



    public final MANA_Unit parent;


    public static MANA_Sector buildEmptySector(MANANeurons target, MANA_Unit parent) {
        MANA_Sector sec = new MANA_Sector(target, parent);
        return sec;
    }

    private MANA_Sector(MANANeurons target, MANA_Unit parent) {
        this.target = target;
        this.parent = parent;
        countDown = new AtomicInteger(0);
        secExcSums = new double[target.N];
        secInhSums = new double[target.N];
        snExcOn = new BoolArray(target.N);
        snInhOn = new BoolArray(target.N);
        spkBuffer = new BoolArray(target.N);
        pfrAccum = new double[target.N];
        spkDat = new SpikeTimeData(target.N);
        id = "s"+target.id;
    }


    /**
     *
     */
    public void init() {
        Arrays.fill(target.inDegree, 0);
        for(MANA_Node node : childNodes.values()) {
            if(node.srcData.isExcitatory()) {
                node.accumulateLocalWtSums(secExcSums);
            }
        }
        recountInDegrees();
        target.setSatC(secExcSums);
        initialized = true;
    }

    /**
     * Adds a source neuron group and automatically constructs a MANA Node connecting
     * that group to the target of the sector using connection specifications supplied by
     * the caller
     * @param src
     * @param specs
     */
    public MANA_Node add(Neuron src, ConnectSpecs specs) {
        MANA_Node newEntry = MANA_Node.buildNodeAndConnections(
                this, src, target, specs, DampFunction.DEF_DAMPENER,
                SynType.getDefaultSTDP(src.isExcitatory(), target.isExcitatory()),
                !parent.targets.contains(src));
        childNodes.put(src, newEntry);
        countDown.set(childNodes.size());
        initialized = false;
        return  newEntry;
    }

    /**
     * Adds a source neuron group and automatically constructs a MANA Node connecting
     * that group to the target of the sector using connection specifications supplied by
     * the caller
     * @param src
     * @param specs
     */
    public MANA_Node add(Neuron src, ConnectSpecs specs, DampFunction damp, STDP rule) {
        MANA_Node newEntry = MANA_Node.buildNodeAndConnections(
                this, src, target, specs,damp, rule,
                !parent.targets.contains(src));
        childNodes.put(src, newEntry);
        countDown.set(childNodes.size());
        initialized = false;
        return  newEntry;
    }

    public void add(MANA_Node node) {
        if(node.targData != target) {
            throw new IllegalArgumentException("Cannot add a pre-built node to a sector that" +
                    " does not have the same target neurons.");
        }
        childNodes.put(node.srcData, node);
        countDown.set(childNodes.size());
    }

    public void update(final double time, final double dt) {
        if(!initialized) {
            throw  new IllegalStateException("Sector updates cannot be performed until initialization has been done.");
        }

        // Determine incoming currents & check for structural changes
        boolean structChanged = false;
        for(MANA_Node node : childNodes.values()) {
            if(!node.updated.get()) {
                throw new IllegalStateException("Not all child nodes were updated.");
            }
            structChanged |= node.getStructureChanged();
            node.structureChangedOff();
            if (node.srcData.isExcitatory()) {
                node.addAndClearLocCurrent(target.i_e);
            } else {
                node.addAndClearLocCurrent(target.i_i);
            }
        }

        // If the structure changed recalculate relevant values like in-degree, etc.
        if(structChanged) {
            recountInDegrees();
        }

        // Sum over the weights
        if(synPlasticityOn) {
            Arrays.fill(secExcSums, 0);
            Arrays.fill(secInhSums, 0);
            for (MANA_Node node : childNodes.values()) {
                if (node.srcData.isExcitatory()) {
                    node.accumulateLocalWtSums(secExcSums);
                } else {
                    node.accumulateLocalWtSums(secInhSums);
                }
            }
        }

        // Check whose incoming synaptic currents have exceeded their norm values and
        // turn on normalization for them
        target.updateTriggers(secExcSums, secInhSums);

        if (target.mhpOn && !(target.allExcSNon && target.allInhSNon)) {
            for (MANA_Node node : childNodes.values()) {
               node.accumulatePFRSums(pfrAccum);
            }
        }

        target.performFullUpdate(spkBuffer, pfrAccum, time, dt);
//        System.out.println("Spikes for: " + id);
//        for(int ii=0; ii<this.getWidth(); ++ii) {
//            if(spkBuffer.get(ii)) {
//                System.out.print("  " + ii + "  ");
//            }
//        }
//        System.out.println();
        //if(!(target.allExcSNon && target.allInhSNon))
            Arrays.fill(pfrAccum, 0);

    }

    public void recountInDegrees() {
       Arrays.fill(target.excInDegree, 0);
       Arrays.fill(target.inhInDegree, 0);

        for(MANA_Node node : childNodes.values()) {
            if(node.srcData.isExcitatory()) {
                node.accumInDegrees(target.excInDegree);
            } else {
                node.accumInDegrees(target.inhInDegree);
            }
        }
        for(int ii=0; ii<target.N; ++ii) {
            target.inDegree[ii] = target.excInDegree[ii] + target.inhInDegree[ii];
        }
    }

    public void synchronize() {
        if (!initialized) {
            throw new IllegalStateException("Sector synchronization cannot be performed until initialization has been done.");
        }
        target.spks.copyInto(spkBuffer);
        spkBuffer.clear();
        target.estFR.pushBufferShallow();
        target.lastSpkTime.pushBufferDeep();
        spkDat.pushSpks(target.spks); // record spiking data
        //   System.out.println(id + "SYNCHRONIZED");

        if (!countDown.compareAndSet(0, childNodes.size())) {
            throw new IllegalStateException("Synchronization cannot be called unless the count-down timer is zero indicating that all child nodes have been updated.");
        }
    }

    public int getWidth() {
        return target.N;
    }

    public boolean isInitialized() {
        return initialized;
    }

}
