package Java.org.network.mana.utils;

import java.util.ArrayList;

public final class SrcTarDataPack {

    public final SrcTarPair coo;
    public final double [] values;
    public final ArrayList<int[]> events;

    public SrcTarDataPack(SrcTarPair coo, double[] values) {
        this.coo = coo;
        this.values = values;
        this.events= new ArrayList<int[]>();
    }
}
