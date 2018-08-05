package nodes;

import base_components.MANANeurons;
import base_components.Neuron;
import base_components.enums.DampFunction;
import base_components.enums.SynType;
import functions.STDP;
import utils.ConnectSpecs;
import utils.SpikeTimeData;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MANA_Sector2 {

    public double [] secExcSums;
    public double [] secInhSums;
    public boolean [] snExcOn;
    public boolean [] snInhOn;
    public double [] pfrLTDAccum;
    public double [] pfrLTPAccum;
    public double [] lastSpkTimeBuffer;
    public double [] estFRBuffer;
    public boolean [] spkBuffer;

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



    public void updateNoSync(final double time, final double dt) {
        try {
        //    gatherChildData(time, dt);
         //   updateTargetNeurons(dt, time);
          //  countDown.set(numNodes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
