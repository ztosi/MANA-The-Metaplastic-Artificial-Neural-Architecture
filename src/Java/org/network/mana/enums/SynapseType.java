package Java.org.network.mana.enums;

import Java.org.network.mana.globals.Default_Parameters;

public enum SynapseType {

    EE {
        @Override
        public double[] getDefaultUDFMeans() {
            return Default_Parameters.EEUDF;
        }

        @Override
        public boolean isExcitatory() {
            return true;
        }

    }, EI {

        @Override
        public double[] getDefaultUDFMeans() {
            return Default_Parameters.EIUDF;
        }

        @Override
        public boolean isExcitatory() {
            return true;
        }
    }, IE {
        @Override
        public double[] getDefaultUDFMeans() {
            return Default_Parameters.IEUDF;
        }

        @Override
        public boolean isExcitatory() {
            return false;
        }

    }, II {
        @Override
        public double[] getDefaultUDFMeans() {
            return Default_Parameters.IIUDF;
        }
        @Override
        public boolean isExcitatory() {
            return false;
        }
    };






    public abstract double[] getDefaultUDFMeans();
    public abstract boolean isExcitatory();


    public static SynapseType getSynType(boolean srcExc, boolean tarExc) {
        if(srcExc) {
            if(tarExc){
                return SynapseType.EE;
            } else {
                return SynapseType.EI;
            }
        } else {
            if(tarExc) {
                return SynapseType.IE;
            } else {
                return SynapseType.II;
            }
        }
    }


}
