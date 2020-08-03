package Java.org.network.mana.ImageWorld;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class TiledImArray {

    private double[] tiledIms;

    private int tilesX;

    private int tilesY;

    private int width;
    private int height;

    private int tileHeight;
    private int tileWidth;

    public TiledImArray(int tilesX, int tilesY, int tileWidth, int tileHeight) {
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.tileHeight = tileHeight;
        this.tileWidth = tileWidth;
        width = tileWidth * tilesX;
        height = tileHeight * tilesY;
        tiledIms = new double[width*height];
    }

    public void replaceTile(double[] vals, int tileInd) {
        int tx = (tileInd%tilesX)*tileWidth*height;
        int txEd = ((tileInd%tilesX)+1)*tileWidth*height;
        int valInd = 0;
        for(int ii = tx; ii<txEd; ii+=height) {
            System.arraycopy(vals, valInd, tiledIms, ii+(tileInd/tilesY * tileHeight), tileHeight);
        }
    }

    public double[] getWindow(int x, int y, int szX, int szY, double[] win) {

        int x_st = x-szX/2;
        int x_ed = x+szX/2;
        int y_st = y-szY/2;
        int y_ed = y+szY/2;

        int winPtr = 0;
        for(int xx=x_st; xx<x_ed; ++xx) {
             int ii = xx;
             if(ii < 0) {
                 ii = width+ii;
             }
             if(ii >= width) {
                 ii = ii-width;
             }
             int indSt = ii*height;
             if(y_st < 0) {
                System.arraycopy(tiledIms, indSt+y_st+height, win, winPtr, -y_st);
                System.arraycopy(tiledIms, indSt, win, winPtr-y_st, szY+y_st);
             } else if(y_ed >= height) {
                System.arraycopy(tiledIms, indSt+y_st, win, winPtr, height-y_st);
                System.arraycopy(tiledIms, indSt, win, winPtr+y_ed-height, y_ed-height);
             } else {
                 System.arraycopy(tiledIms, indSt+y_st, win, winPtr, szY);
             }
             winPtr += szY;
        }
        return win;
    }

    public void im2Dat(BufferedImage im) {
        for(int ii=0; ii< im.getWidth(); ++ii) {
            for(int jj=0; jj<im.getHeight(); ++jj) {
                tiledIms[ii*im.getHeight() + jj] = rgb2double(im.getRGB(ii,jj));
            }
        }
    }

    public static double rgb2double(int rgb) {
        int mask = 0xFF;
        double val = (double) (rgb & mask);
        val += (double) ((rgb & mask << 8)>>8);
        val += (double) ((rgb & mask << 16)>>16);
        val /= 765;
        return val;
    }

    public static BufferedImage dat2Im(double[] dat, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, TYPE_INT_ARGB);
        for(int ii=0; ii<width; ii++) {
            for(int jj=0; jj<height; ++jj) {
                float intensity = (float)(dat[ii*height+jj]);
                Color c = new Color(intensity, intensity, intensity);
                image.setRGB(ii, jj, c.getRGB());
            }
        }
        return  image;
    }

    public int getHeight() {
        return  height;
    }

    public int getWidth() {
        return width;
    }

    public static void main(String[] args) {

        try {
            BufferedImage im = ImageIO.read(new File("myImage.png"));
            TiledImArray tim = new TiledImArray(1, 1, im.getHeight(), im.getWidth());
            tim.im2Dat(im);
            im = dat2Im(tim.tiledIms, tim.width, tim.height);

            JPanel theFrame = new JPanel();
            FlowLayout fl = new FlowLayout();
            theFrame.setLayout(fl);
            BufferedImage scim = new BufferedImage(500, 500, im.getType());
            Graphics2D g = scim.createGraphics();
            AffineTransform scale = AffineTransform.getScaleInstance(500.0/im.getWidth(), 500.0/im.getHeight());
            g.drawRenderedImage(im, scale);
            JLabel bob = new JLabel(new ImageIcon(scim));

            //bob.setLocation(0, 0);
            //bob.setSize(500, 500);
            bob.setVisible(true);

            double [] data = new double[250000];
            tim.getWindow(650, 0, 500, 500, data);

            BufferedImage winIm = dat2Im(data, 500, 500);
            BufferedImage scWinIm = new BufferedImage(500, 500, winIm.getType());
            AffineTransform scale2 = AffineTransform.getScaleInstance(1, 1);
            Graphics2D wing = scWinIm.createGraphics();
            wing.drawRenderedImage(winIm, scale2);
            JLabel bob2 = new JLabel((new ImageIcon(scWinIm)));
            theFrame.add(bob2, fl);
            //bob2.setLocation(500, 0);
            //bob2.setSize(500, 500);
            bob2.setVisible(true);
            theFrame.add(bob, fl);

            theFrame.setVisible(true);

            JFrame realFrame = new JFrame();

            realFrame.setLocation(200, 100);
            realFrame.setSize(1200, 500);
            realFrame.add(theFrame);

            realFrame.setVisible(true);
            realFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
           // realFrame.pack();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }


    }

}
