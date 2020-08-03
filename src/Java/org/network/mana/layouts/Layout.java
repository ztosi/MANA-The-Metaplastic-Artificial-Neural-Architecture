package Java.org.network.mana.layouts;

import Java.org.network.mana.base_components.SpikingNeuron;

public abstract class Layout  {

    protected double xMax = 0, xMin = 0;
    protected double yMax = 0, yMin = 0;
    protected double zMax = 0, zMin = 0;

    public Layout(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        this.xMax = xMax;
        this.xMin = xMin;
        this.yMax = yMax;
        this.yMin = yMin;
        this.zMax = zMax;
        this.zMin = zMin;
    }

    public Layout(double[] xlims, double[] ylims, double[] zlims) {
        this(xlims[0], xlims[1], ylims[0], ylims[1], zlims[0], zlims[1]);
    }

    public abstract void layout(SpikingNeuron neus);


}
