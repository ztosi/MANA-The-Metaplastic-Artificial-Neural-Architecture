package Java.org.network.mana.base_components.enums;

import Java.org.network.mana.base_components.SynapseData;
import Java.org.network.mana.functions.HebSTDP;
import Java.org.network.mana.functions.MexHatSTDP;
import Java.org.network.mana.functions.STDP;

import java.util.concurrent.ThreadLocalRandom;

public enum SynType {
    EE {
        @Override
        public double[] getDefaultUDFMeans() {
            return EEUDF;
        }
        @Override
        public double getDefaultQTau() {
            return ExcTau;
        }
        @Override
        public boolean isExcitatory() {
            return true;
        }
        @Override
        public double getLRate() {
            return SynapseData.E_LR;
        }
        @Override
        public double getDefaultShapeP1() {
            return eeTauPlus;
        }
        @Override
        public double getDefaultShapeP2() {
            return eeTauMinus;
        }
        @Override
        public double getDefaultWPlus() {
            return eeWPlus;
        }
        @Override
        public double getDefaultWMinus() {
            return eeWMinus;
        }

    }, EI {

        @Override
        public double[] getDefaultUDFMeans() {
            return EIUDF;
        }
        @Override
        public double getDefaultQTau() {
            return ExcTau;
        }
        @Override
        public boolean isExcitatory() {
            return true;
        }
        @Override
        public double getLRate() {
            return SynapseData.E_LR;
        }
        @Override
        public double getDefaultShapeP1() {
            return eiTauPlus;
        }
        @Override
        public double getDefaultShapeP2() {
            return eiTauMinus;
        }
        @Override
        public double getDefaultWPlus() {
            return eiWPlus;
        }
        @Override
        public double getDefaultWMinus() {
            return eiWMinus;
        }
    }, IE {
        @Override
        public double[] getDefaultUDFMeans() {
            return IEUDF;
        }
        @Override
        public double getDefaultQTau() {
            return InhTau;
        }
        @Override
        public boolean isExcitatory() {
            return false;
        }
        @Override
        public double getLRate() {
            return SynapseData.I_LR;
        }
        @Override
        public double getDefaultShapeP1() {
            return ieSigSq;
        }
        @Override
        public double getDefaultShapeP2() {
            return ieNrmSq;
        }
        @Override
        public double getDefaultWPlus() {
            return ieWPlus;
        }
        @Override
        public double getDefaultWMinus() {
            return ieWMinus;
        }
    }, II {
        @Override
        public double[] getDefaultUDFMeans() {
            return IIUDF;
        }
        @Override
        public double getDefaultQTau() {
            return InhTau;
        }
        @Override
        public boolean isExcitatory() {
            return false;
        }
        @Override
        public double getLRate() {
            return SynapseData.I_LR;
        }
        @Override
        public double getDefaultShapeP1() {
            return iiSigSq;
        }
        @Override
        public double getDefaultShapeP2() {
            return iiNrmSq;
        }
        @Override
        public double getDefaultWPlus() {
            return iiWPlus;
        }
        @Override
        public double getDefaultWMinus() {
            return iiWMinus;
        }
    };

    public abstract double[] getDefaultUDFMeans();
    public abstract double getDefaultQTau();
    public abstract boolean isExcitatory();
    public abstract double getLRate();
    public abstract double getDefaultShapeP1();
    public abstract double getDefaultShapeP2();
    public abstract double getDefaultWPlus();
    public abstract double getDefaultWMinus();

    private static final double PI_4TH_RT = Math.pow(Math.PI, 0.25);

    private static final double[] EEUDF = new double[]{0.5, 1100, 50};
    private static final double[] EIUDF = new double[]{0.05, 125, 1200};
    private static final double[] IEUDF = new double[]{0.25, 700, 20};
    private static final double[] IIUDF = new double[]{0.32, 144, 60};

    public static final double DEF_EE_C = 0.3;
    public static final double DEF_EI_C = 0.2;
    public static final double DEF_IE_C = 0.4;
    public static final double DEF_II_C = 0.1;

    public static final double ExcTau = 3.0;
    public static final double InhTau = 6.0;

    public static final double eeTauPlus = 25;
    public static final double eeTauMinus = 100;
    public static final double eiTauPlus = 25;
    public static final double eiTauMinus = 100;
    public static final double ieSigma = 22;
    public static final double iiSigma = 12;

    public static final double eeWPlus = 5;
    public static final double eeWMinus = 1;
    public static final double eiWPlus = 5;
    public static final double eiWMinus = 1;
    public static final double ieWPlus = 1.8;
    public static final double ieWMinus = 1.8;
    public static final double iiWPlus = 1.2;
    public static final double iiWMinus = 2.2;

    public static final double ieNrmSq = 2.0/(Math.sqrt(3*ieSigma)*PI_4TH_RT);
    public static final double ieSigSq = ieSigma * ieSigma;
    public static final double iiNrmSq = 2.0/(Math.sqrt(3*iiSigma)*PI_4TH_RT);
    public static final double iiSigSq = iiSigma * iiSigma;

    public static SynType getSynType(boolean srcExc, boolean tarExc) {
        if(srcExc) {
            if(tarExc){
                return SynType.EE;
            } else {
                return SynType.EI;
            }
        } else {
            if(tarExc) {
                return SynType.IE;
            } else {
                return SynType.II;
            }
        }
    }

    public static double getConProbBase(boolean srcExc, boolean tarExc) {
        if (srcExc) {
            if (tarExc)
                return DEF_EE_C;
            else
                return DEF_EI_C;
        } else {
            if (tarExc)
                return DEF_IE_C;
            else
                return DEF_II_C;
        }
    }


    public static STDP getDefaultSTDP(boolean srcExc, boolean tarExc) {
        if (srcExc) {
            if (tarExc)
                return new HebSTDP(eeTauPlus, eeTauMinus, eeWPlus, eeWMinus, STDP.DEF_LEARNING_RATE);
            else
                return new HebSTDP(eiTauPlus, eiTauMinus, eiWPlus, eiWMinus, STDP.DEF_LEARNING_RATE);
        } else {
            if (tarExc)
                return new MexHatSTDP(ieWPlus, ieWMinus, ieSigma, STDP.DEF_LEARNING_RATE);
            else
                return new MexHatSTDP(iiWPlus, iiWMinus, iiSigma, STDP.DEF_LEARNING_RATE);
        }
    }

    // Outbound values... delay, lastArr, U, D, F, u, R
    public static void setSourceDefaults(final double [] sData, int start, SynType type) {
        ThreadLocalRandom localRand = ThreadLocalRandom.current();
        double [] meanVals = type.getDefaultUDFMeans();
        sData[2+start] = Math.abs(localRand.nextGaussian()*meanVals[0]/2 + meanVals[0]);
        sData[3+start] = Math.abs(localRand.nextGaussian()*meanVals[1]/2 + meanVals[1]);
        sData[4+start] = Math.abs(localRand.nextGaussian()*meanVals[2]/2 + meanVals[2]);
        sData[6+start] = 1;
    }

    public static void getPSR_UDF(int index, double time, double [] data) {
        double isi = -((time + data[index]) - data[index+1]); // time + delay - lastArrival
        data[index+5] = data[index+2] + (data[index+5] * (1-data[index+2]) //U + (u * (1-U))*exp(-isi/F)
                * Math.exp(isi/data[index+4]));
        data[index+6] = 1 + ((data[index+6] - (data[index+5] * data[index+6]) - 1)
                * Math.exp(isi/data[index+3]));

    }
}