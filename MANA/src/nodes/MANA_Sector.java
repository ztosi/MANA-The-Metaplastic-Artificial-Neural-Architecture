package nodes;

import base_components.MANANeurons;
import base_components.Neuron;
import base_components.enums.DampFunction;
import base_components.enums.SynType;
import functions.STDP;
import utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A mana sector consists of a set of target MANA neurons and all the MANA nodes connecting that target to a source.
 * It is responsible for updating the target neurons as well as applying any operations requiring knowledge of
 * ALL incoming connections and/or source neurons that cannot be performed at the node-level. This includes things
 * like synaptic normalization. A sector update can only occur once all child node updates have completed and a sector
 * synchronize can only occur when all sectors have completed their updates. A sector synchronize consists of pushing
 * all values in buffers to their respective main locations. That is, all data in the target neurons which other
 * nodes/sectors might still be using.
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

    public final Map<Neuron, MANA_Node> childNodes = new TreeMap<Neuron, MANA_Node>(
            (Neuron a, Neuron b) -> {
                if (a.getID() < b.getID()) {
                    return  -1;
                } else if (a.getID() > b.getID()) {
                    return 1;
                } else {
                    throw new IllegalStateException("Either multiple neuron groups have been assigned " +
                            "the same ID or you are trying to add a " +
                            "source neuron group which already exists here.");
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
        spkBuffer = new BoolArray(target.N);
        pfrAccum = new double[target.N];
        spkDat = new SpikeTimeData(target.N);
        id = "s"+target.id;
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
        return  newEntry;
    }

    public void add(MANA_Node node) {
        if(node.targData != target) {
            throw new IllegalArgumentException("Cannot add a pre-built node to a sector that" +
                    " does not have the same target neurons.");
        }
        childNodes.put(node.srcData, node);
        countDown.set(childNodes.size()-1);
    }



    public void update(final double time, final double dt) {

        // Determine incoming currents & check for structural changes
        boolean structChanged = false;
        for(MANA_Node node : childNodes.values()) {
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
                target.inDegree[ii] = target.excInDegree[ii] + target.inDegree[ii];
            }
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
        spkDat.pushSpks(target.spks); // record spiking data
        if(!(target.allExcSNon && target.allInhSNon))
            Arrays.fill(pfrAccum, 0);

    }

    public void recountInDegrees() {
        int[] excInDs = new int[target.N];
        int[] inhInDs = new int[target.N];

        for(MANA_Node node : childNodes.values()) {
            if(node.srcData.isExcitatory()) {
                node.accumInDegrees(excInDs);
            } else {
                node.accumInDegrees(inhInDs);
            }
        }
        for(int ii=0; ii<target.N; ++ii) {
            target.inDegree[ii] = excInDs[ii] + inhInDs[ii];
        }
    }

    public void synchronize() {
        target.spks.copyInto(spkBuffer);
        target.estFR.pushBufferShallow();
        target.lastSpkTime.pushBufferDeep();
        countDown.set(childNodes.size());
    }

    public int getWidth() {
        return target.N;
    }

}
