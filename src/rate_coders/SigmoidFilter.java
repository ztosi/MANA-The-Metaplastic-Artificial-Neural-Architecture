package rate_coders;

import Java.org.network.mana.base_components.LIFNeurons;
import Java.org.network.mana.execution.tasks.Updatable;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class SigmoidFilter implements Updatable {

    public LIFNeurons neurons;

    private float ceil = 2;
    private float floor = -2;
    private float k = 10f;
    private float b = 0;
    private float leakRate = 60;

    private float[] filterVals;
    private float[] filterInputs;

    private AtomicBoolean updated = new AtomicBoolean(false);

    public SigmoidFilter(LIFNeurons neurons) {
        this.neurons = neurons;
        filterInputs = new float[neurons.getSize()];
        filterVals = new float[neurons.getSize()];
        Arrays.fill(filterInputs, 1);
    }

    public SigmoidFilter(LIFNeurons neurons, float floor, float ceil, float k, float b, float leakRate) {
        this(neurons);
        this.floor = floor;
        this.ceil = ceil;
        this.k = k;
        this.b = b;
        this.leakRate = leakRate;
    }

    @Override
    public void update(final double time, final double dt) {
        for(int ii=0; ii<neurons.getSize(); ++ii) {
            double val = neurons.getSpikes().get(ii) ? 1.0/dt:0;
            filterVals[ii] -= dt*filterVals[ii]/20;
            filterVals[ii] += 0.05*(val + 0.1* ThreadLocalRandom.current().nextDouble());
            filterVals[ii] = filterVals[ii] > 2 ? 2:filterVals[ii];

            //filterInputs[ii] = (float) (dt/leakRate * val + ((1-(dt/leakRate))*filterInputs[ii]));
        }
//        double range = ceil-floor;
//        for(int ii=0; ii<neurons.getSize(); ++ii) {
//            filterVals[ii] = (float)(range/(1+Math.exp(-k*filterInputs[ii]-b))+floor);
//        }
    }

    public float[] getFilterVals(){
        return filterVals;
    }

     // TODO make sure this doesn't cause race conditions
    @Override
    public void setUpdated(boolean updated) {
        this.updated.set(updated);
    }

    @Override
    public boolean isUpdated() {
        return updated.get();
    }

    public void copyTo(float[] cpy) {
        System.arraycopy(filterVals, 0, cpy, 0, 10);
    }

    public int size() {
        return neurons.getSize();
    }
}
