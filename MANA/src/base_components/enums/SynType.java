package base_components.enums;

import base_components.SynapseData;

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
}