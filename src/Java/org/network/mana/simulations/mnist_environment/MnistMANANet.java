package Java.org.network.mana.simulations.mnist_environment;

import Java.org.network.mana.base_components.MANANeurons;
import Java.org.network.mana.base_components.SpikingNeuron;
import Java.org.network.mana.execution.MANA_Executor;
import Java.org.network.mana.nodes.MANA_Unit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static Java.org.network.mana.mana.MANA_Globals.dt;

public class MnistMANANet {

    public static final String DEF_ODIR = "."+File.separator+"MnistOutputs" + File.separator;
    public static final String DEF_PREFIX = "MnistMANA";

    public HashMap<Integer, ArrayList<MNISTImage>> mnistTrain;
    public HashMap<Integer, ArrayList<MNISTImage>> mnistTest;

    private MANA_Unit centralRes;
    private MANA_Unit second_res;
    private int resSize = 1000;
    private double[][] resBounds = new double[3][2];
    {
        resBounds[0][0] = 0;
        resBounds[0][1] = 200;
        resBounds[1][0] = 0;
        resBounds[1][1] = 200;
        resBounds[2][0] = 50;
        resBounds[2][1] = 550;
    }

    private EyeInput eye;

    private MANANeurons motionOutputP;
    private MANANeurons motionOutputN;
    private MotionOutput dxdy;

    private MANANeurons repOutP;
    private MANANeurons repOutN;
    private MotionOutput rep_motion;
    //private SigmoidFilter representation;

    private int winWidth = 10;
    private int winHeight = 10;

    private double time_f = 7.2E6;
    private double p_shutOff_f =time_f/2;

    private MANA_Executor exec = new MANA_Executor(5000, dt);

    public MnistMANANet(int resSize, String imageFilesTr, String labelsTR){//}, String labelsTR, String labelsTS) {

        mnistTrain = new MNISTLoader(imageFilesTr,labelsTR).mnistData;
//        mnistTest = new MNISTLoader(imageFilesTs,labelsTS).mnistData;
        eye = EyeInput.buildEyeInput(new EyeWindow(winWidth, winHeight), mnistTrain, 50, 50, -50);

        centralRes = MANA_Unit.MANABuilder(Collections.singletonList(eye), resSize, resBounds);
//        for(MANA_Sector s : centralRes.sectors.values()) {
//            double mean = s.target.isExcitatory() ? 35:25;
//            s.target.tau_m.set(Utils.getGaussRandomArray(s.target.N, mean, 5), 0, s.target.N);
//        }


        motionOutputP = new MANANeurons(2, true);
        //motionOutputP.mhpFrozen = true;
        motionOutputN = new MANANeurons(2, true);
     //   motionOutputN.mhpFrozen = true;
        //motionOutputP.i_bg.setAll(20);
        //motionOutputN.i_bg.setAll(20);
        dxdy = new MotionOutput(motionOutputP, motionOutputN,2);
        motionOutputN.setCoor(1, 100, 100, 350);
        motionOutputN.setCoor(0, 100, 100, 350);
        motionOutputP.setCoor(1, 100, 100, 350);
        motionOutputP.setCoor(0, 100, 100, 350);
        motionOutputP.adaptJump=1;
        motionOutputN.adaptJump=1;


        repOutP = new MANANeurons(10, true);
        repOutP.adaptJump = 0;
        //repOutP.r_m.setAll(0.1);
        repOutN = new MANANeurons(10, true);
        repOutN.adaptJump = 0;
       // repOutN.r_m.setAll(0.1);
        //representation = new SigmoidFilter(repOut);
        rep_motion = new MotionOutput(repOutP, repOutN, 10);

        centralRes.addSector(repOutP, centralRes.defLayout, centralRes.defInpCS);
        centralRes.addSector(repOutN, centralRes.defLayout, centralRes.defInpCS);
        centralRes.addSector(motionOutputN, centralRes.defLayout, centralRes.defInpCS);
        centralRes.addSector(motionOutputP, centralRes.defLayout, centralRes.defInpCS);

        //motionOutputN.i_bg.setAt(19.9, );


        double maxDist = centralRes.getMaxDist();
        double lambda = maxDist/3;
        exec.addUnit(centralRes, centralRes.getFullSize(), centralRes.getSize(), lambda, maxDist);
        centralRes.initialize();
    }
    public double [] target_location = new double[10];
    public void runNetwork(double time, double dt) {
        File mainOut = new File(DEF_ODIR);
        if (!mainOut.exists()) {
            if (!mainOut.mkdir()) {
                System.err.println("FATAL ERROR: FAILED TO CREATE OR FIND MAIN "
                        + "OUTPUT DIRECTORY.");
                System.exit(1);
            }
        }
        long iters = 0;
        int outputId = 0;
        boolean first = true;
        boolean tripped = false;
        boolean change = false;
        int oN = 500000;
        float[][] outputs = new float[oN][14];
        eye.setdXdY(1,1);
        motionOutputP.setMhpOn(false);
        motionOutputN.setMhpOn(false);
        motionOutputN.noMHP = true;
        motionOutputP.noMHP = true;
        long start = System.nanoTime();
        centralRes.setMhpOn(false);
        eye.setHeadless(false);
        repOutP.setMhpOn(false);
        repOutP.noMHP=true;
        repOutN.setMhpOn(false);
        repOutN.noMHP=true;

       // repOut.setPrefFRs(1);
        motionOutputP.setPrefFRs(20);
        motionOutputN.setPrefFRs(20);
        List<String> sectorNames = new ArrayList<String>(centralRes.sectors.keySet());
        try {
            while(time < time_f) {
                if(time >= p_shutOff_f && !tripped) {
                    System.out.println("Turning off plasticity");
                    tripped = true;
                    centralRes.setMhpOn(false);
                    centralRes.setNormalizationOn(false);
                    centralRes.setSynPlasticOn(false);
                    //motionOutputN.mhpOn = false;
                    //motionOutputP.mhpOn = false;
                    exec.pruneOn = false;
                }
                eye.iters = (int)iters;

                if(iters != 0 && iters%oN == 0) {
                    print(outputId, outputs);
                    outputId++;
                }

                if((iters)%(1000/ dt) == 0 && time != 0) {
                    System.out.println("----------------------- " + (int)(iters/(1000/dt)) + " sim secs. / " +
                            (System.nanoTime()-start)/1000000000 + " real secs. ----------------------- " );
                }

                if(iters==40000) {
                    centralRes.setMhpOn(true);
                    motionOutputP.setMhpOn(false);
                    motionOutputN.setMhpOn(false);
                    motionOutputN.noMHP = true;
                    motionOutputP.noMHP = true;
                }
                if(iters % 10000 == 0) {
                    for(String s : sectorNames) {
                        for(SpikingNeuron child : centralRes.sectors.get(s).childNodes.keySet()) {
                            if(centralRes.sectors.get(s).childNodes.get(child).inputIsExternal) {
                                System.out.print(centralRes.sectors.get(s).childNodes.get(child).getNNZ() + "  ");
                            }
                        }
                    }
                    centralRes.printData(mainOut.toString(), DEF_PREFIX, time, dt);
                    System.out.println();
                }
                if(iters % 200000 == 0) {
                    centralRes.printData(mainOut.toString(), DEF_PREFIX, time, dt);
                    print((int)(iters/2000), outputs);
                }

                System.arraycopy(rep_motion.position, 0, outputs[(int)(iters%oN)], 0, 10);
                outputs[(int)(iters%oN)][10] = (float)dxdy.getdX(0);
                outputs[(int)(iters%oN)][11] = (float)dxdy.getdX(1);
                outputs[(int)(iters%oN)][12] = (float)eye.getCurrentNumber();
                outputs[(int)(iters%oN)][13] = (float)time;

                // Teacher forcing

                int curr = eye.getCurrentNumber();
                Arrays.fill(target_location, 0);
                target_location[eye.getCurrentNumber()] = 1;
//                if(time<2e6) {
//                    for(int kk=0; kk<10; ++kk) {
//                        repOut.i_bg.setAt(15, kk);
//                    }
//                } else {
                rep_motion.update(time, dt, target_location);
                //System.out.println(Arrays.toString(rep_motion.position));

                eye.neurons.spks.clear();
                eye.update(dt, time, null);
                // Update Everyone
                exec.invoke();

                // get the new dx and dy for the eye window
                dxdy.update(time, dt);
                eye.setdXdY(dxdy.getdX(0), dxdy.getdX(1));


                //representation.update(time, dt);
                time = exec.getTime(); // get time from the executor
                iters++;
            }
        } catch (Exception ie) {
            ie.printStackTrace();
        } finally {
            centralRes.printData(mainOut.toString(), DEF_PREFIX, time, dt);
        }
    }

    public void print(int id, float[][] dat) throws IOException {
        FileWriter f = new FileWriter("RepAnddxdy"+id+".dat");
        PrintWriter pw = new PrintWriter(f);
        for(int ii=0; ii<dat.length; ++ii) {
            pw.println(Arrays.toString(dat[ii]));
        }
        pw.close();
    }

    public static void main(String[] args) {
        MnistMANANet net = new MnistMANANet(1000, "train-images-idx3-ubyte",
                "train-labels-idx1-ubyte");
        net.runNetwork(0, dt);
    }


}
