package Java.org.network.mana.layouts;

import Java.org.network.mana.base_components.SpikingNeuron;

import java.util.concurrent.ThreadLocalRandom;

public class LatticeLayout extends Layout {

    private int numX;
    private int numY;
    private int numZ;

    private LatticeLayout(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        super(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    private LatticeLayout(double[] xlims, double[] ylims, double[] zlims) {
        this(xlims[0], xlims[1], ylims[0], ylims[1], zlims[0], zlims[1]);
    }

    public LatticeLayout(int size, double[] xlims, double[] ylims, double[] zlims) {
        this(xlims, ylims, zlims);
        int sideLen = (int) Math.ceil(Math.pow(size, 1.0/3));
        numX = sideLen;
        numY = sideLen;
        numZ = sideLen;
    }

    public LatticeLayout(int[] dims, double[] xlims, double[] ylims, double[] zlims) {
        this(xlims, ylims, zlims);
        numX = dims[0];
        numY = dims[1];
        numZ = dims[2];
    }

    @Override
    public void layout(SpikingNeuron neus) {
        int index = 0;
        double spacingX = (xMax-xMin)/(numX-1);
        double spacingY = (yMax-yMin)/(numY-1);
        double spacingZ = (zMax-zMin)/(numZ-1);
        double x = xMin, y = yMin, z=zMin;
        for(int kk=0; kk<numZ; ++kk) {
            for(int jj=0; jj<numY; ++jj) {
                for (int ii=0; ii<numX; ++ii) {
                    if(index >= neus.getSize()) return;
                    neus.setCoor(index, x, y, z);
                    x+= spacingX;
                    index++;
                }
                y += spacingY;
            }
            z+= spacingZ;
        }
    }
}
