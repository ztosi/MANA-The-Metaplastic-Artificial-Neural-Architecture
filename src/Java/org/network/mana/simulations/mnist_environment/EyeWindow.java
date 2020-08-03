package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.ImageWorld.TiledImArray;

import java.util.concurrent.ThreadLocalRandom;

public class EyeWindow {

    private int x=14;
    private int y=14;

    private double xa = 14;
    private double ya = 14;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public final int eye_height;
    public final int eye_width;
    public final int numPx;

    private double alpha = 1.0;

    private byte[] data;

    public EyeWindow(final int width, final int height) {
        this.eye_width = width;
        this.eye_height = height;
        numPx = width*height;
        data = new byte[width*height];
    }

    /**
     * Moves the window over the image according to a dx and dy, which are floating point. Since
     * the window can only move in a discrete manner some integer number of pixels, decimal values
     * are treated probabilistically. e.g. for a dx*timeStep of 1.1, the window has a 90% probability of
     * moving one pixel right and a 10% probability of moving two pixels right.
     * @param dx
     * @param dy
     * @param timeStep
     */
    public void update(MNISTImage image, double dx, double dy, double timeStep) {
        dx *= timeStep;
        dy *= timeStep;

        ya += dy;
        xa += dx;

        y = (int)Math.floor(ya);
        x = (int)Math.floor(xa);

//
//        double xSgn = Math.signum(dx);
//        double ySgn = Math.signum(dy);
//        dx = Math.abs(dx);
//        dy = Math.abs(dy);
//
//        if(ThreadLocalRandom.current().nextDouble() < dx - Math.floor(dx)) {
//            dx = xSgn * Math.ceil(dx);
//        } else {
//            dx = xSgn * Math.floor(dx);
//        }
//        if(ThreadLocalRandom.current().nextDouble() < dy - Math.floor(dy)) {
//            dy = ySgn * Math.ceil(dy);
//        } else {
//            dy = ySgn * Math.floor(dy);
//        }
        x+=(int)dx;
        y+=(int)dy;
        if(xa<0) xa = image.cols + xa;
        if(ya<0) ya = image.rows + ya;
        if(xa >= image.cols) xa = xa-image.cols;
        if(ya >= image.rows) ya = ya-image.rows;
        if(x<0) x = image.cols + x;
        if(y<0) y = image.rows + y;
        if(x >= image.cols) x = x-image.cols;
        if(y >= image.rows) y = y-image.rows;
        image.getWindow(y, x, eye_height, eye_width, data);
//        for(int ii=0, n=data.length; ii<n; ++ii) {
//            data[ii] *= alpha;
//        }
    }

    public byte[] getData() {
        return data;
    }

}
