package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.base_components.LIFNeurons;
import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.execution.tasks.Syncable;
import Java.org.network.mana.layouts.LatticeLayout;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EyeInput implements SpikingNeuron, Syncable {

    public static final Utils.ProbDistType def_pd = Utils.ProbDistType.NORMAL;
    public static final int DEFAULT_EXPOSURE_TIME = 5000;

    public final int id;

    public LIFNeurons neurons; // current injection scheme
    private int[] outDegree;
    public double[][] xyzCoors;
    public EyeWindow eye;
    private int exposureTime = DEFAULT_EXPOSURE_TIME;
    private MNISTImage currentImage;
    private HashMap<Integer, ArrayList<MNISTImage>> images = new HashMap<>();
    private double gamma = 40;
    {
        for(int ii=0; ii<10; ++ii) {
            images.put(ii, new ArrayList<>());
        }
    }
    public BoolArray spkBuffer;

    private boolean headless = false;

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean getHeadless() {
        return headless;
    }

    public int iters = 0;
    private JFrame frame;
    private BufferedImage im;
    private JPanel theFrame;
    private BufferedImage bob;

    private volatile double dx = 0, dy = 0;

    public static EyeInput buildEyeInput (EyeWindow eye,
                                          HashMap<Integer, ArrayList<MNISTImage>> images,
                                          double xInit,
                                          double yInit,
                                          double zInit) {

        EyeInput inNeu = new EyeInput(eye, images);
        new LatticeLayout(new int[]{eye.eye_width, eye.eye_height, 1},
                new double[]{xInit, xInit+(20*(eye.eye_width-1))},
                new double[]{yInit, yInit+(20*(eye.eye_height-1))},
                new double[]{zInit, zInit}).layout(inNeu.neurons);
        return inNeu;

    }

    public static EyeInput buildInpNeuronsFromLocations (EyeWindow eye,
                                                         HashMap<Integer, ArrayList<MNISTImage>> images,
                                                         double[] xCoors,
                                                         double[] yCoors,
                                                         double[] zCoors) {
        EyeInput inNeu = new EyeInput(eye, images);
        inNeu.neurons.xyzCoors = new double[inNeu.getSize()][3];
        for (int ii = 0; ii < inNeu.getSize(); ++ii) {
            inNeu.neurons.xyzCoors[ii][0] = xCoors[ii];
            inNeu.neurons.xyzCoors[ii][1] = yCoors[ii];
            inNeu.neurons.xyzCoors[ii][2] = zCoors[ii];
        }
        return inNeu;
    }


    private EyeInput(EyeWindow inp, HashMap<Integer, ArrayList<MNISTImage>> images) {
        id = MANA_Globals.getID();
        eye = inp;
        down_samp_size = (int)(2*eye.eye_width);
        int numDs = down_samp_size*down_samp_size;
        neurons = new LIFNeurons(eye.numPx+numDs, true);
        neurons.adaptJump = 0;
        this.images = images;
        this.outDegree = new int[numDs+eye.numPx];
        currentImage = images.get(0).get(0);
        neurons.i_bg.makeUncompressible();
        xyzCoors = new double[neurons.getSize()][3];
        intense = new float[down_samp_size*down_samp_size];
        spkBuffer = new BoolArray(numDs+eye.numPx);
        eyeIntense = new float[eye.numPx];

//        frame = new JFrame("Current Image");
    }
    private byte[] down_samp = null;
    private byte[] px = null;
    private int [][] c_change = null;
    private int down_samp_size;
    private float [] intense;
    private float [] eyeIntense;
    BufferedImage intenseOut;
    BufferedImage eyewindow;
    boolean show_spks = false;
//    private int[] been = new int[28*28];
    /**
     * Updates the eye input. Randomly selects a new MNIST image if the time interval lapses.
     * Moves the eye window over the MNIST image with a given dx, dy. Pixels are assumed to
     * range from -128-127. Causes neurons with little input to randomly fire with some small
     * probably proportional to the intensity under them.
     * @param dt
     * @param time
     */
    public void update(double dt, double time, BoolArray wow) {
        //BoolArray spkBuffer = neurons.spks;
        int exposureIts = (int) (exposureTime / dt);
        if (theFrame == null && !headless) {
            theFrame = new JPanel();
        }
        if (frame == null && !headless) {
            frame = new JFrame("Current Image");
        }

        int x = eye.getX();
        int y = eye.getY();
        if (iters % exposureIts == 0) {
            //  eye.setX(14);
            // eye.setY(14);
            int label = ThreadLocalRandom.current().nextInt(10);
            int ex = ThreadLocalRandom.current().nextInt(images.get(label).size());
            currentImage = images.get(label).get(ex);
            down_samp = currentImage.getDownSample(down_samp_size);
            px = currentImage.getPixels();
            c_change = new int[px.length][3];
            for (int ii = 0; ii < px.length; ++ii) {
                c_change[ii] = new int[]{px[ii], px[ii], px[ii]};
            }
        }
//            been = new int[28*28];
//            synchronized (been) {
//                Arrays.fill(been, 10);
//            }

//            synchronized(been) {
//                incBeen(x, y);
//                if (getBeen(x,y) >= 199) {
//                    setBeen(x, y, 199);
//                }
//            }

        for (int ii = 0; ii < down_samp_size; ii++) {
            for (int jj = 0; jj < down_samp_size; ++jj) {
                int ind = ii * down_samp_size + jj;
                intense[ind] += -dt*intense[ind]/10 + (neurons.spks.get(ind+eye.numPx) ? 50:0);
                if(intense[ind] > 254) {
                    intense[ind]=254;
                }
                if(intense[ind]<0) {
                    intense[ind]=0;
                }
//                if(neurons.spks.get(ind+2*eye.numPx)) {
//                    System.out.println("SPK");
//                }
                double intensity = (down_samp[ind]+128) / 255.0;
                int neurind = ind+eye.numPx;
                //neurons.i_bg.setAt(15+intensity*gamma, ind);
                neurons.i_bg.setAt(15 + (intensity) * 60, neurind);

            }
        }

        if (!headless) {

            int ind = y*28 + x;
            if (c_change[ind][0] < 127) {
                c_change[ind][0]++;
            } else {
                c_change[ind][0] = 127;
            }
            if (c_change[ind][1] > -127) {
                c_change[ind][1]--;
            } else {
                c_change[ind][1] = -127;
            }
            if (c_change[ind][2] > -127) {
                c_change[ind][2]--;
            } else {
                c_change[ind][2] = -127;
            }

            if (iters % exposureIts == 0) {
                im = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);
                for (int ii = 0; ii < px.length; ++ii) {
                    Color c = new Color((int)px[ii] + 128, (int)px[ii] + 128, (int)px[ii] + 128);
                    //                System.out.println(c.getBlue() + "b");
                    //System.out.println(c.toString());
                    im.setRGB(ii % 28, ii / 28, c.getRGB());
                    //                System.out.println((im.getRGB(ii/28, ii%28) &0xFF) + "bim");
                }

                BufferedImage lowRes = new BufferedImage(down_samp_size, down_samp_size, BufferedImage.TYPE_INT_RGB);
                for (int ii = 0; ii < down_samp.length; ++ii) {
                    Color c = new Color(down_samp[ii] + 128, down_samp[ii] + 128, down_samp[ii] + 128);
                    lowRes.setRGB(ii % down_samp_size, ii / down_samp_size, c.getRGB());
                }
                BufferedImage ioLowRes = new BufferedImage(down_samp_size, down_samp_size, BufferedImage.TYPE_INT_RGB);
                for(int ii=0; ii<intense.length; ++ii) {
                    Color c = new Color((int)intense[ii], (int)intense[ii],(int)intense[ii]);
                    ioLowRes.setRGB(ii%down_samp_size, ii/down_samp_size, c.getRGB());
                }

                theFrame.removeAll();
                GridLayout lay = new GridLayout(2,2);
                theFrame.setLayout(lay);
                AffineTransform af = AffineTransform.getScaleInstance(20, 20);
                bob = new BufferedImage(560, 560, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bob.createGraphics();
                g.drawRenderedImage(im, af);
                JLabel bobL = new JLabel(new ImageIcon(bob));
                bobL.setVisible(true);
                theFrame.add(bobL);
                AffineTransform af2 = AffineTransform.getScaleInstance(560 / down_samp_size, 560 / down_samp_size);
                BufferedImage bob2 = new BufferedImage(560, 560, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = bob2.createGraphics();
                g2.drawRenderedImage(lowRes, af2);
                JLabel bobLR = new JLabel(new ImageIcon(bob2));
                bobLR.setVisible(true);
                theFrame.add(bobLR);
                if(show_spks) {
                    AffineTransform af3 = AffineTransform.getScaleInstance(560 / down_samp_size, 560 / down_samp_size);
                    intenseOut = new BufferedImage(560, 560, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g3 = intenseOut.createGraphics();
                    g3.drawRenderedImage(ioLowRes, af3);
                    JLabel inten = new JLabel(new ImageIcon(intenseOut));
                    inten.setVisible(true);
                    theFrame.add(inten);
                    eyewindow = new BufferedImage(560, 560, BufferedImage.TYPE_INT_RGB);
                    JLabel ew = new JLabel(new ImageIcon(eyewindow));
                    ew.setVisible(true);
                    theFrame.add(ew);
                }
                theFrame.setVisible(true);
                theFrame.repaint();
                frame.setLocation(200, 200);
                frame.setSize(3*560, 560);
                frame.add(theFrame);
                frame.pack();
                frame.setVisible(true);
                //im = bob;
            }
            int blck_size = 560/28;
            Color c = new Color(c_change[ind][0]+ 128, c_change[ind][1] + 128, c_change[ind][2] + 128);
            for(int ii=0; ii<blck_size; ++ii) {
                for(int jj=0; jj<blck_size; ++jj) {
                    bob.setRGB( x*20+jj, y*20+ii, c.getRGB());
                }
            }
            if(show_spks) {
                for (int ii = 0; ii < intense.length; ++ii) {
                    Color c2 = new Color((int) intense[ii], (int) intense[ii], (int) intense[ii]);
                    int col = ii % down_samp_size;
                    int row = ii / down_samp_size;
                    for (int kk = 0; kk < blck_size; ++kk) {
                        for (int jj = 0; jj < blck_size; ++jj) {
                            intenseOut.setRGB(col * 20 + jj, row * 20 + kk, c2.getRGB());
                        }
                    }
                }
                int blockSz = 560 / eye.eye_width;
                for (int ii = 0; ii < eye.eye_height; ++ii) {
                    for (int jj = 0; jj < eye.eye_width; ++jj) {
                        int index = ii * eye.eye_width + jj;
                        Color ce = new Color((int) eyeIntense[index], (int) eyeIntense[index], (int) eyeIntense[index]);
                        for (int kk = 0; kk < blockSz; ++kk) {
                            for (int ll = 0; ll < blockSz; ++ll) {
                                eyewindow.setRGB(jj * blockSz + kk, ii * blockSz + ll, ce.getRGB());
                            }
                        }
                    }
                }
            }
            theFrame.repaint();

        }
            eye.update(currentImage, dx, dy, dt);
//            if (!headless) {
//                Graphics2D g2 = im.createGraphics();
//                //g2.clearRect(20 * x, 20 * y, 15, 15);
//                g2.drawImage(im, 0, 0, theFrame);
//                g2.setColor(new Color(0.005f * getBeen(x,y), 0f, 0f));
//                g2.fill(new Ellipse2D.Float(20 * eye.getX(), 20 * eye.getY(), 15, 15));
//                g2.dispose();
//                theFrame.repaint();
//            }

        for(int ii=0; ii<eye.eye_height; ii++) {
            for(int jj=0; jj<eye.eye_width; ++jj) {
                int ind = ii*eye.eye_width + jj;
                double intensity = eye.getData()[ind]/255.0;
                eyeIntense[ind] = (float)-dt*eyeIntense[ind]/10 + (neurons.spks.get(ind) ? 50:0);
                if(eyeIntense[ind] > 255) {
                    eyeIntense[ind]=255;
                }
                if(eyeIntense[ind] < 0) {
                    eyeIntense[ind]=0;
                }
                neurons.i_bg.setAt(15+intensity*gamma, ind);
                //neurons.i_bg.setAt(15+(1-intensity)*gamma, ind+(eye.eye_height*eye.eye_width));
            }
        }
        neurons.update(dt, time, spkBuffer);
//        for(int ii=0; ii<spkBuffer.length; ++ii) {
//            if(spkBuffer.get(ii)) {
//                System.out.println("Spike " + ii);
//            }
//        }
    }

//    public synchronized void incBeen(final int x, final int y) {
//        been[y*28+x]++;
//    }
//    public synchronized void setBeen(final int x, final int y, final int val) {
//        been[y*28+x]=val;
//    }
//    public synchronized int getBeen(final int x, final int y) {
//        return been[y*28+x];
//    }

    public void setdXdY(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public void setExposureTime(int exposureTime) {
        this.exposureTime = exposureTime;
    }

    public MNISTImage getCurrentImage() {
        return currentImage;
    }

    public int getCurrentNumber() {
        return currentImage.label;
    }

    public BoolArray getSpikes() {
        return neurons.getSpikes();
    }

    public int getSize() {
        return neurons.getSize();
    }

    @Override
    public int[] getOutDegree() {
        return outDegree;
    }


    @Override
    public boolean isExcitatory(){
        return true;
    }

    @Override
    public double[][] getCoordinates(boolean trans) {
        if (trans) {
            double[][] xyzCpy = new double[3][getSize()];
            for (int ii = 0; ii < getSize(); ++ii) {
                xyzCpy[0][ii] = xyzCoors[ii][0];
                xyzCpy[1][ii] = xyzCoors[ii][1];
                xyzCpy[2][ii] = xyzCoors[ii][2];
            }
            return xyzCpy;
        } else {
            return  xyzCoors;
        }
    }

    public void setCoor(int index, double x, double y, double z) {
        xyzCoors[index][0] = x;
        xyzCoors[index][1] = y;
        xyzCoors[index][2] = z;
    }

    public void setCoor(int index, double[] xyz) {
        setCoor(index, xyz[0], xyz[1], xyz[2]);
    }

    @Override
    public int getID() {
        return id;
    }

}
