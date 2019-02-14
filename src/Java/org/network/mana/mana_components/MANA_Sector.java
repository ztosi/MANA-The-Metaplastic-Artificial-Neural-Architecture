package Java.org.network.mana.mana_components;

import Java.org.network.mana.base_components.SpikeTimeData;
import Java.org.network.mana.base_components.neurons.Neuron;
import Java.org.network.mana.base_components.synapses.ConnectSpecs;
import Java.org.network.mana.base_components.synapses.STDP;
import Java.org.network.mana.base_components.synapses.SynapseProperties;
import Java.org.network.mana.exec.Syncable;
import Java.org.network.mana.exec.Updatable;
import Java.org.network.mana.utils.BoolArray;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Java.org.network.exec.exec sector consists of a set of target MANA neurons and all the MANA Java.org.network.exec.mana_components connecting that target to a source.
 * It is responsible for updating the target neurons as well as applying any operations requiring knowledge of
 * ALL incoming connections and/or source neurons that cannot be performed at the node-level. This includes things
 * like synaptic normalization. A sector update can only occur once all child node updates have completed and a sector
 * synchronize can only occur when all sectors have completed their updates. A sector synchronize consists of pushing
 * all values in buffers to their respective main locations. That is, all data in the target neurons which other
 * Java.org.network.exec.mana_components/sectors might still be using.
 *
 * @author Zoë Tosi
 */
public class MANA_Sector implements Syncable, Updatable {

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
                node.calcAndAccumWtSums(secExcSums);
            }
        }
        recountInDegrees();
        double[] dummy = new double[secExcSums.length];
        System.arraycopy(secExcSums,0, dummy, 0, dummy.length);
        for(int ii=0; ii<dummy.length; ++ii) {
            dummy[ii] += MANANeurons.default_sat_c + 20;
        }
        target.setSatC(dummy);
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
                this, src, target, specs,
                SynapseProperties.getDefaultSTDP(src.isExcitatory(), target.isExcitatory()),
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
    public MANA_Node add(Neuron src, ConnectSpecs specs, STDP rule) {
        MANA_Node newEntry = MANA_Node.buildNodeAndConnections(
                this, src, target, specs, rule,
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


    @Override public void update(final double time, final double dt) {
        if(!initialized) {
            throw  new IllegalStateException("Sector updates cannot be performed until initialization has been done.");
        }

        // Determine incoming currents & check for structural changes
        boolean structChanged = false;
        for(MANA_Node node : childNodes.values()) {
            if(!node.isUpdated()) {
                throw new IllegalStateException("Not all child mana_components were updated.");
            }
            structChanged |= node.getStructureChanged();
            node.structureChangedOff();
            if (node.srcData.isExcitatory()) {
                node.addAndClearLocCurrent(target.getIncExcCurrent());
            } else {
                node.addAndClearLocCurrent(target.getIncInhCurrent());
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

        if (target.mhpOn && !(target.allExcSNon && target.allInhSNon)) {
            for (MANA_Node node : childNodes.values()) {
               node.accumulatePFRSums(pfrAccum);
            }
        }

        target.performFullUpdate(spkBuffer, pfrAccum, secExcSums, secInhSums, time, dt);
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
        target.getSpikes().copyInto(spkBuffer);
        spkBuffer.clear();
        target.estFR.pushBufferShallow();
        target.getLastSpkTimes().pushBufferShallow();
        spkDat.pushSpks(target.getSpks()); // record spiking data

        if (!countDown.compareAndSet(0, childNodes.size())) {
            throw new IllegalStateException("Synchronization cannot be called unless the count-down timer is zero indicating that all child mana_components have been updated.");
        }
    }

    public int getWidth() {
        return target.N;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public double[] getSectorSums(boolean exc) {
        return exc ? secExcSums : secInhSums;
    }

}
