package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.base_components.LIFNeurons;
import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.layouts.LatticeLayout;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.BoolArray;
import Java.org.network.mana.execution.tasks.Syncable;
import Java.org.network.mana.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
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
    private double gamma = 60;
    {
        for(int ii=0; ii<10; ++ii) {
            images.put(ii, new ArrayList<>());
        }
    }

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
        neurons = new LIFNeurons(eye.numPx, true);
        this.images = images;
        this.outDegree = new int[eye.numPx];
        currentImage = images.get(0).get(0);
        neurons.i_bg.makeUncompressible();
        xyzCoors = new double[neurons.getSize()][3];

//        frame = new JFrame("Current Image");
    }


    /**
     * Updates the eye input. Randomly selects a new MNIST image if the time interval lapses.
     * Moves the eye window over the MNIST image with a given dx, dy. Pixels are assumed to
     * range from -128-127. Causes neurons with little input to randomly fire with some small
     * probably proportional to the intensity under them.
     * @param dt
     * @param time
     */
    public void update(double dt, double time, BoolArray wow) {
        BoolArray spkBuffer = neurons.spks;
        int exposureIts = (int) (exposureTime/dt);
        if(theFrame == null && !headless) {
            theFrame = new JPanel();
        }
        if(frame == null && !headless) {
            frame = new JFrame("Current Image");
        }
        if(iters % exposureIts == 0) {
            int label = ThreadLocalRandom.current().nextInt(10);
            int ex = ThreadLocalRandom.current().nextInt(images.get(label).size());
            currentImage = images.get(label).get(ex);
            byte[] px = currentImage.getPixels();
            if(!headless) {
                im = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);

                for (int ii = 0; ii < px.length; ++ii) {
//                System.out.println((int)px[ii] +128);
                    Color c = new Color(px[ii] + 128, px[ii] + 128, px[ii] + 128);
//                System.out.println(c.getBlue() + "b");
                    im.setRGB(ii % 28, ii / 28, c.getRGB());
//                System.out.println((im.getRGB(ii/28, ii%28) &0xFF) + "bim");

                }
                theFrame.removeAll();
                FlowLayout fl = new FlowLayout();
                theFrame.setLayout(fl);

                AffineTransform af = AffineTransform.getScaleInstance(20, 20);

                BufferedImage bob = new BufferedImage(560, 560, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bob.createGraphics();
                g.drawRenderedImage(im, af);
                JLabel bobL = new JLabel(new ImageIcon(bob));
                bobL.setVisible(true);
                theFrame.add(bobL, fl);
                theFrame.setVisible(true);
                theFrame.repaint();
                frame.setLocation(200, 200);
                frame.setSize(560, 560);
                frame.add(theFrame);
                frame.pack();
                frame.setVisible(true);
                im = bob;
            }
        }
        int x = eye.getX();
        int y = eye.getY();
        eye.update(currentImage, dx, dy, dt);
        if(!headless) {
            Graphics2D g2 = im.createGraphics();
            g2.clearRect(20 * x, 20 * y, 15, 15);
            g2.drawImage(im, 0, 0, theFrame);
            g2.setColor(Color.RED);
            g2.fill(new Ellipse2D.Float(20 * eye.getX(), 20 * eye.getY(), 15, 15));

            g2.dispose();
            theFrame.repaint();
        }
        for(int ii=0; ii<eye.eye_height; ii++) {
            for(int jj=0; jj<eye.eye_width; ++jj) {
                int ind = ii*eye.eye_width + jj;
                double intensity = eye.getData()[ind]/255.0 + 0.5;
                intensity = 1-intensity;
//                if(intensity<0.1) {
//                    double spkProb = 0.01;
//                    spkProb*=dt;
//                    if(ThreadLocalRandom.current().nextDouble() < spkProb) {
//                        spkBuffer.set(ind, true);
//                        neurons.lastSpkTime.setBuffer(ind, time);
//                    }
//                    neurons.i_bg.setAt(18, ind);
//                } else {
                    neurons.i_bg.setAt(14+intensity*gamma, ind);
//                }
            }
        }
        neurons.update(dt, time, spkBuffer);
    }

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
        return eye.numPx;
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
