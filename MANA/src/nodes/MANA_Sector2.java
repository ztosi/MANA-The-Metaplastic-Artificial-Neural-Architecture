package nodes;

import base_components.MANANeurons;
import base_components.Neuron;
import base_components.enums.DampFunction;
import base_components.enums.SynType;
import functions.STDP;
import utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MANA_Sector2 implements Syncable {

    public double [] secExcSums;
    public double [] secInhSums;
    public BoolArray snExcOn;
    public BoolArray snInhOn;
    public double [] pfrAccum;
    public BoolArray spkBuffer;

    public boolean synPlasticityOn = true;

    public SpikeTimeData spkDat;

    public final AtomicInteger countDown;

    public final MANANeurons target;

    public final Map<Neuron, MANA_Node2> childNodes = new TreeMap<Neuron, MANA_Node2>(
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

    public boolean allExcSNon = false;
    public boolean allInhSNon = false;

    public final MANA_Unit parent;


    public static MANA_Sector2 buildEmptySector(MANANeurons target, MANA_Unit parent) {
        MANA_Sector2 sec = new MANA_Sector2(target, parent);
        return sec;
    }

    private MANA_Sector2(MANANeurons target, MANA_Unit parent) {
        this.target = target;
        this.parent = parent;
        countDown = new AtomicInteger(0);
        secExcSums = new double[target.N];
        secInhSums = new double[target.N];
        spkBuffer = new BoolArray(target.N);
        pfrAccum = new double[target.N];
    }


    /**
     * Adds a source neuron group and automatically constructs a MANA Node connecting
     * that group to the target of the sector using connection specifications supplied by
     * the caller
     * @param src
     * @param specs
     */
    public MANA_Node2 add(MANANeurons src, ConnectSpecs specs) {
        MANA_Node2 newEntry = MANA_Node2.buildNodeAndConnections(
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
    public MANA_Node2 add(MANANeurons src, ConnectSpecs specs, DampFunction damp, STDP rule) {
        MANA_Node2 newEntry = MANA_Node2.buildNodeAndConnections(
                this, src, target, specs,damp, rule,
                !parent.targets.contains(src));
        childNodes.put(src, newEntry);
        return  newEntry;
    }

    public void add(MANA_Node2 node) {
        if(node.targData != target) {
            throw new IllegalArgumentException("Cannot add a pre-built node to a sector that" +
                    " does not have the same target neurons.");
        }
        childNodes.put(node.srcData, node);
    }



    public void update(final double time, final double dt) {

        // Determine incoming currents
        for(MANA_Node2 node : childNodes.values()) {
            if (node.srcData.isExcitatory()) {
                node.addAndClearLocCurrent(target.i_e);
            } else {
                node.addAndClearLocCurrent(target.i_i);
            }
        }

        // Sum over the weights
        if(synPlasticityOn) {
            Arrays.fill(secExcSums, 0);
            Arrays.fill(secInhSums, 0);
            for (MANA_Node2 node : childNodes.values()) {
                if (node.srcData.isExcitatory()) {
                    node.accumulateLocalWtSums(secExcSums);
                } else {
                    node.accumulateLocalWtSums(secInhSums);
                }
            }
        }

        if (target.mhpOn) {
            for (MANA_Node2 node : childNodes.values()) {
               node.accumulatePFRSums(pfrAccum);
            }
        }

        target.performFullUpdate(spkBuffer, pfrAccum, time, dt);
        Arrays.fill(pfrAccum, 0);

    }

    public void recountInDegrees() {
        int[] excInDs = new int[target.N];
        int[] inhInDs = new int[target.N];

        for(MANA_Node2 node : childNodes.values()) {
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

}
