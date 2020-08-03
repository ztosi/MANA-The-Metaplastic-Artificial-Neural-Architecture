package Java.org.network.mana.layouts;

import Java.org.network.mana.base_components.SpikingNeuron;

import java.util.concurrent.ThreadLocalRandom;

public class RandomLayout extends Layout {

    public RandomLayout(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        super(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    public RandomLayout(double[] xlims, double[] ylims, double[] zlims) {
        this(xlims[0], xlims[1], ylims[0], ylims[1], zlims[0], zlims[1]);
    }

    @Override
    public void layout(SpikingNeuron neus) {

        for(int ii=0, n=neus.getSize(); ii<n; ++ii) {
            double x = ThreadLocalRandom.current().nextDouble(xMin, xMax);
            double y = ThreadLocalRandom.current().nextDouble(yMin, yMax);
            double z = ThreadLocalRandom.current().nextDouble(zMin, zMax);
            neus.setCoor(ii, x, y, z);
        }

    }
}
