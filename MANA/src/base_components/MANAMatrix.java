package base_components;

import base_components.enums.OrderType;
import base_components.enums.SynType;
import utils.SrcTarDataPack;
import utils.SrcTarPair;
import utils.Utils;

import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class MANAMatrix {

    private SynapseMatrix weightsTOrd;

    private SynapseMatrix outDataSOrd;

    public final SynType type;

    /**
     * Using a lot of defaults
     * @param offsetSrc
     * @param offsetTar
     * @param src
     * @param tar
     * @param maxDist
     * @param maxDly
     */
    public MANAMatrix(int offsetSrc, int offsetTar,
                      MANANeurons src, MANANeurons tar, double maxDist, double maxDly) {

        double [][] delays = Utils.getDelays(src.xyzCoors, tar.xyzCoors, src==tar, maxDist, maxDly);
        LinkedList<SrcTarDataPack> targCOOTup = new LinkedList<>();
        LinkedList<SrcTarDataPack> srcCOOTup = new LinkedList<>();
        type = SynType.getSynType(src.isExcitatory(), tar.isExcitatory());

        for (int ii=0; ii<src.N; ii++) {
            for(int jj=0; jj<tar.N; jj++) {
                if (src == tar && ii==jj) continue;
                SrcTarPair coo = new SrcTarPair(ii, jj);
                double[] tarData = {SynapseData.DEF_NEW_WEIGHT, 0};
                SrcTarDataPack tarDatPack = new SrcTarDataPack(coo, tarData);
                double[] srcData = new double[7];
                setSourceDefaults(srcData);
                srcData[0] = delays[ii][jj];
                SrcTarDataPack srcDatPack = new SrcTarDataPack(coo, srcData);

                targCOOTup.add(tarDatPack);
                srcCOOTup.add(srcDatPack);
            }
        }

        weightsTOrd = new SynapseMatrix(targCOOTup, src.N, tar.N,
                offsetSrc, offsetTar, OrderType.TARGET);
        outDataSOrd = new SynapseMatrix(srcCOOTup, tar.N, src.N,
                offsetTar, offsetSrc, OrderType.SOURCE);



        // Outbound values... delay, lastArr, U, D, F, u, R
       // outDataSOrd = new SynapseMatrix()


    }

    private void setSourceDefaults(final double [] sData) {
        ThreadLocalRandom localRand = ThreadLocalRandom.current();
        double [] meanVals = type.getDefaultUDFMeans();
        sData[2] = Math.abs(localRand.nextGaussian()*meanVals[0]/2 + meanVals[0]);
        sData[3] = Math.abs(localRand.nextGaussian()*meanVals[1]/2 + meanVals[1]);
        sData[4] = Math.abs(localRand.nextGaussian()*meanVals[2]/2 + meanVals[2]);
        sData[6] = 1;
    }



}
