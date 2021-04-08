package Java.org.network.mana.simulations.mnist_environment;

public class MNISTImage {

    public static final int HEIGHT = 28;
    public static final int WIDTH = 28;
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

        pixels = new byte[HEIGHT*WIDTH];

        System.arraycopy(inputStream, offset, pixels, 0, rows*cols);
        for(int ii=0; ii<pixels.length; ++ii) {
            if(pixels[ii] >=0) {
                pixels[ii] -= 0x7F;
            } else {
                pixels[ii] += 0x7F;
            }
        }
    }

    public byte[] getDownSample(int out_size) {
        int kernel_size = HEIGHT - out_size + 1;
        byte[] ds = new byte[out_size*out_size];
        double [] intensity = new double[out_size*out_size];
        for(int ii=0; ii< out_size; ++ii) {
            for(int jj=0; jj<out_size; ++jj) {
                int k_value = 0x0;
                for(int kk=ii; kk<(ii+kernel_size); ++kk) {
                    for(int ll=jj; ll<(jj+kernel_size); ++ll) {
                        k_value += 128+(int)getPixel(kk, ll);
                    }
                }
                int mean = (k_value/(kernel_size*kernel_size));
                double mnSq = (mean*mean)/65536.0;
                intensity[ii*out_size +jj] = mnSq;
            }
        }
        double max = Double.MIN_VALUE;
        for(int ii=0;ii<out_size*out_size;++ii) {
            if(intensity[ii] > max) {
                max = intensity[ii];
            }
        }
        for(int ii=0;ii<out_size*out_size;++ii) {
            intensity[ii] /= max;
            intensity[ii] = (intensity[ii]*253)-125; // to make sure there's no under/over
            ds[ii] = (byte)(intensity[ii]);
        }
        return ds;
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

    public void make_landscape() {
        for(int ii=0; ii<rows; ++ii) {
            for(int jj=0; jj<cols; ++jj) {
                //TODO
            }
        }
    }

    public static double getIntensity(MNISTImage image, int ii, int jj, double gamma) {
        return gamma * (image.getPixel(ii,jj)/255.0 + 0.5);
    }
}
