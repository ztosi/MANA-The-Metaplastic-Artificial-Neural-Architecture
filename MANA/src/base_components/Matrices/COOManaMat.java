package base_components.Matrices;

import utils.SrcTarDataPack;
import utils.SrcTarPair;

import java.util.LinkedList;
import java.util.List;

/**
 * A class that converts a MANAMatrix into a set of COO tuples...
 * this allows one to easily add/remove synapses, then remake
 * a MANAMatrix from the new values.
 */
public class COOManaMat {
    public final List<SrcTarDataPack> data;
    public final int srcILF;
    public final int tarILF;

    public COOManaMat(int srcILF, int tarILF) {
        data = new LinkedList<>();
        this.srcILF=srcILF;
        this.tarILF=tarILF;
    }

    public COOManaMat(MANAMatrix mat) {
        data = new LinkedList<>();
        int [] srcPtrs = mat.outDataSOrd.getRawPtrs();
        double [] tordVals = mat.weightsTOrd.getRawData();
        double [] sordVals = mat.outDataSOrd.getRawData();
        int [] tOrdInds = mat.outDataSOrd.getRawOrdIndices();
        int [] map = mat.srcToTargLookup;
        srcILF = mat.outDataSOrd.nILFac;
        tarILF = mat.weightsTOrd.nILFac;
        int totData = srcILF+tarILF + 2; // +2 for target last arrival and linear index
        int src=-1;
        // Traversing in source-major order
        for(int ii=0; ii<mat.noSrc; ++ii) {
            src++;
            for(int jj = srcPtrs[ii]; jj < srcPtrs[ii+1]; ++jj) { //linear indices
                int tOrderIndex = mat.srcToTargLookup[jj];
                double [] tmpData = new double[totData];
                // copy in target ordered values using the lookup table since we're
                // traversing source-ordered
                System.arraycopy(tordVals, tOrderIndex*tarILF,
                        tmpData, 0, tarILF);
                // copy in source ordered values for the same synapse
                System.arraycopy(sordVals, jj, tmpData, tarILF, srcILF);
                tmpData[tmpData.length-2] = mat.tOrdLastArrivals.values[tOrderIndex];
                // Attach the linear index when target ordered to support target
                // ordered add on sparse values...
                tmpData[tmpData.length-1] = tOrderIndex;

                SrcTarDataPack tmpDatPack =
                        new SrcTarDataPack(new SrcTarPair(src, tOrdInds[jj]), tmpData);
                data.add(tmpDatPack);
            }
        }
    }
}
