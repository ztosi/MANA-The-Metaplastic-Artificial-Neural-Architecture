package Java.org.network.mana.ImageWorld;

import java.util.concurrent.ThreadLocalRandom;

public class Eye { //TODO: make a retina

    private TiledImArray imageWorld;

    private int x;
    private int y;

    public final int eye_height;
    public final int eye_width;

    private double alpha = 1.0;

    public Eye(int width, int height, TiledImArray imageWorld) {
        this.eye_width = width;
        this.eye_height = height;
        this.imageWorld = imageWorld;
    }

    /**
     * Moves the window over the image according to a dx and dy, which are floating point. Since
     * the window can only move in a discrete manner some integer number of pixels, decimal values
     * are treated probabilistically. e.g. for a dx*timeStep of 1.1, the window has a 90% probability of
     * moving one pixel right and a 10% probability of moving two pixels right.
     * @param data
     * @param dx
     * @param dy
     * @param timeStep
     */
    public void update(double[] data, double dx, double dy, double timeStep) {
        dx *= timeStep;
        dy *= timeStep;
        double xSgn = Math.signum(dx);
        double ySgn = Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        if(ThreadLocalRandom.current().nextDouble() < dx - Math.floor(dx)) {
            dx = xSgn * Math.ceil(dx);
        } else {
            dx = xSgn * Math.floor(dx);
        }
        if(ThreadLocalRandom.current().nextDouble() < dy - Math.floor(dy)) {
            dy = ySgn * Math.ceil(dy);
        } else {
            dy = ySgn * Math.floor(dy);
        }
        x+=(int)dx;
        y+=(int)dy;
        if(x<0) x = imageWorld.getWidth() + x;
        if(y<0) y = imageWorld.getHeight() + y;
        if(x >= imageWorld.getWidth()) x = x-imageWorld.getWidth();
        if(y >= imageWorld.getWidth()) y = y-imageWorld.getHeight();
        imageWorld.getWindow(x, y, eye_width, eye_height, data);
//        for(int ii=0, n=data.length; ii<n; ++ii) {
//            data[ii] *= alpha;
//        }
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getAlpha() {
        return alpha;
    }

}
