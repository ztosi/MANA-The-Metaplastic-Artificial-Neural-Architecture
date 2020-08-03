package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.utils.Utils;

public class MNISTImage {

    private byte[] pixels;
    private long[] vecField;

    public final int rows;
    public final int cols;

    public final int label;

    private float[] xfilt = new float[]{1, 2, 1, 0, -1, -2, -1};
    private float[] yfilt = new float[]{-1, -2, -1, 0, 1, 2, 1};

    public MNISTImage(byte[] inputStream, int offset, int label) {
        this.label=label;

        rows = (int)inputStream[11];
        cols = (int)inputStream[15];

        pixels = new byte[28*28];

        System.arraycopy(inputStream, offset, pixels, 0, rows*cols);
        for(int ii=0; ii<pixels.length; ++ii) {
            if(pixels[ii] >=0) {
                pixels[ii] -= 0x7F;
            } else {
                pixels[ii] += 0x7F;
            }
        }
    }



    public byte[] getWindow(int x, int y, int szX, int szY, byte[] win) {

        int x_st = x-szX/2;
        int x_ed = x+szX/2;
        int y_st = y-szY/2;
        int y_ed = y+szY/2;

        int winPtr = 0;
        for(int xx=x_st; xx<x_ed; ++xx) {
            int ii = xx;
            if(ii < 0) {
                ii = cols+ii;
            }
            if(ii >= cols) {
                ii = ii-cols;
            }
            int indSt = ii*rows;
            if(y_st < 0) {
                System.arraycopy(pixels, indSt+y_st+rows, win, winPtr, -y_st);
                System.arraycopy(pixels, indSt, win, winPtr-y_st, szY+y_st);
            } else if(y_ed >= rows) {
                System.arraycopy(pixels, indSt+y_st, win, winPtr, rows-y_st);
                System.arraycopy(pixels, indSt, win, winPtr+y_ed-rows, y_ed-rows);
            } else {
                System.arraycopy(pixels, indSt+y_st, win, winPtr, szY);
            }
            winPtr += szY;
        }
        return win;
    }

    public byte[] getPixels() {
        return pixels;
    }

    public byte getPixel(int ii, int jj) {
        return pixels[ii*cols + jj];
    }

    public static double getIntensity(MNISTImage image, int ii, int jj, double gamma) {
        return gamma * (image.getPixel(ii,jj)/255.0 + 0.5);
    }
}
