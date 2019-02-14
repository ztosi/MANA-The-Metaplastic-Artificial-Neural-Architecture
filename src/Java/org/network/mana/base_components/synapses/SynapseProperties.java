package Java.org.network.mana.base_components.synapses;

import java.util.concurrent.ThreadLocalRandom;

public enum SynapseProperties {

    EE {
        @Override
        public double[] getDefaultUDFMeans() {
            return ShortTermPlasticity.EEUDF;
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
            return E_LR;
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
            return ShortTermPlasticity.EIUDF;
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
            return E_LR;
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
            return ShortTermPlasticity.IEUDF;
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
            return I_LR;
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
            return ShortTermPlasticity.IIUDF;
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
            return I_LR;
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

    public static final double E_LR = 1E-6;
    public static final double I_LR = 1E-6;

    public static final double MAX_WEIGHT= 20;
    public static final double MIN_WEIGHT = 0.01;
    public static final double DEF_NEW_WEIGHT = 0.01;
    public static final double MAX_DELAY= 20; //ms
    public static final double DEF_INIT_WDERIV = 5E-6;


    public abstract double[] getDefaultUDFMeans();
    public abstract double getDefaultQTau();
    public abstract boolean isExcitatory();
    public abstract double getLRate();
    public abstract double getDefaultShapeP1();
    public abstract double getDefaultShapeP2();
    public abstract double getDefaultWPlus();
    public abstract double getDefaultWMinus();

    private static final double PI_4TH_RT = Math.pow(Math.PI, 0.25);



    public static final double DEF_EE_C = 0.3;
    public static final double DEF_EI_C = 0.2;
    public static final double DEF_IE_C = 0.4;
    public static final double DEF_II_C = 0.1;

    public static final double ExcTau = 3.0;
    public static final double InhTau = 6.0;

    public static final double eeTauPlus = 25;
    public static final double eeTauMinus = 100;
    public static final double eiTauPlus = 25;
    public static final double eiTauMinus = 80;
    public static final double ieSigma = 22;
    public static final double iiSigma = 12;

    public static final double eeWPlus = 5;
    public static final double eeWMinus = 1.0;
    public static final double eiWPlus = 5;
    public static final double eiWMinus = 1.0;
    public static final double ieWPlus = 1;
    public static final double ieWMinus = 1.8;
    public static final double iiWPlus = 1.2;
    public static final double iiWMinus = 2.2;

    public static final double ieNrmSq = 2.0/(Math.sqrt(3*ieSigma)*PI_4TH_RT);
    public static final double ieSigSq = ieSigma * ieSigma;
    public static final double iiNrmSq = 2.0/(Math.sqrt(3*iiSigma)*PI_4TH_RT);
    public static final double iiSigSq = iiSigma * iiSigma;

    public static SynapseProperties getSynType(boolean srcExc, boolean tarExc) {
        if(srcExc) {
            if(tarExc){
                return SynapseProperties.EE;
            } else {
                return SynapseProperties.EI;
            }
        } else {
            if(tarExc) {
                return SynapseProperties.IE;
            } else {
                return SynapseProperties.II;
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
    public static void setSourceDefaults(final double [] sData, int start, SynapseProperties type) {
        ThreadLocalRandom localRand = ThreadLocalRandom.current();
        double [] meanVals = type.getDefaultUDFMeans();
        sData[2+start] = Math.abs(localRand.nextGaussian()*meanVals[0]/2 + meanVals[0]);
        sData[3+start] = Math.abs(localRand.nextGaussian()*meanVals[1]/2 + meanVals[1]);
        sData[4+start] = Math.abs(localRand.nextGaussian()*meanVals[2]/2 + meanVals[2]);
        sData[6+start] = 1;
    }


}