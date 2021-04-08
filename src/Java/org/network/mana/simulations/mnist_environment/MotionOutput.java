package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.base_components.LIFNeurons;
import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.execution.tasks.Syncable;
import rate_coders.SigmoidFilter;

public class MotionOutput implements Syncable {

    private double [] dX;
    public float [] position;


    private SigmoidFilter filterPos;
    private SigmoidFilter filterNeg;
    private int groups;
    private int group_size;

    public MotionOutput(LIFNeurons neuPos, LIFNeurons neuNeg, int groups) {
        filterPos = new SigmoidFilter(neuPos);
        filterNeg = new SigmoidFilter(neuNeg);
        this.groups = groups;
        dX = new double[groups];
        position = new float[groups];
        group_size = neuPos.getSize()/groups;
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
        for(int ii=0, n=dX.length; ii<n; ++ii) {
            position[ii] += dX[ii];
        }
    }

    public void update(double time, double dt, double [] target_position) {
        filterPos.update(time, dt);
        filterNeg.update(time, dt);
        calcDX();

        for(int ii=0, n=dX.length; ii<n; ++ii) {
            position[ii] += (float)dX[ii]*dt*0.001;
        }
        for(int ii=0, n=dX.length; ii<n; ++ii) {
            double diff = target_position[ii] - position[ii];
            double pos_ibg = filterPos.neurons.i_bg.get(ii);
            double neg_ibg = filterNeg.neurons.i_bg.get(ii);
            ((MANANeurons)filterPos.neurons).prefFR[ii] = 20.0/(1.0+Math.exp(-diff));
            ((MANANeurons)filterNeg.neurons).prefFR[ii] = 20.0/(1.0+Math.exp(diff));
        }

    }

    public double[] calcDX() {
        int kk=0;
        for(int ii=0; ii<groups; ++ii) {
            dX[ii] = 0;
            for(int jj=0; jj<group_size; ++jj) {
                dX[ii] += filterPos.getFilterVals()[kk] - filterNeg.getFilterVals()[kk];
                kk++;
            }
            dX[ii] /= (double)group_size;
        }
        return dX;
    }


    public double getdX(int dim) {
        return dX[dim];
    }

}
