package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.execution.tasks.Syncable;
import rate_coders.SigmoidFilter;

public class MotionOutput implements Syncable {

    private double [] dX;

    private SigmoidFilter filterPos;
    private SigmoidFilter filterNeg;



    public MotionOutput(SpikingNeuron neuPos, SpikingNeuron neuNeg) {
        filterPos = new SigmoidFilter(neuPos);
        filterNeg = new SigmoidFilter(neuNeg);
        dX = new double[filterNeg.size()];
    }


    public MotionOutput(SigmoidFilter filterPos, SigmoidFilter filterNeg) {
        if(filterNeg.size() != filterPos.size()) {
            throw new IllegalArgumentException("Positive and negative rate coders must be the same length");
        }
        this.filterPos = filterPos;
        this.filterNeg = filterNeg;
        dX = new double[filterNeg.size()];
    }

    public void update(double time, double dt) {
        filterPos.update(time, dt);
        filterNeg.update(time, dt);
        calcDX();
    }

    public double[] calcDX() {
        for(int ii=0,n=dX.length; ii<n; ++ii) {
            dX[ii] = filterPos.getFilterVals()[ii] - filterNeg.getFilterVals()[ii];
        }
        return dX;
    }

    public double getdX(int dim) {
        return dX[dim];
    }

}
