package base_components.enums;

import base_components.MANANeurons;

//TODO: Make this an interface
public enum DampFunction {



    HARD_BOUND {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
            hardMinBound(dwWVec, minVal);
            for(int ii=dwWVec.length-2; ii>=0; ii-=2) {
                double dm = dwWVec[ii] - maxVal;
                dwWVec[ii] = maxVal - (dm - Math.abs(dm))/2;
            }
        }
    }, EXP_BOUND {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
            hardMinBound(dwWVec, minVal);
            for(int ii=dwWVec.length-1; ii<=1; ii-=2) {
                dwWVec[ii] *= Math.exp(-5*dwWVec[ii-1]/maxVal);
            }
        }
    }, HARD_EXPSq {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
            hardMinBound(dwWVec, minVal);
            hardMinBound(dwWVec, minVal);
            for(int ii=dwWVec.length-1; ii<=1; ii-=2) {
                dwWVec[ii] *= 1; // TODO
            }

        }
    }, NO_MAX {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
            hardMinBound(dwWVec, minVal);
        }
    },
        NONE{
            @Override
            public final void dampen(final double[] dwWVec,
            double maxVal, double minVal) {
                return;
            }
    };

    public static final DampFunction DEF_DAMPENER = DampFunction.HARD_BOUND;

    public abstract void dampen(final double[] dwWVec, double maxVal, double minVal);

    public static void hardMinBound(final double[] dwWVec, final double minVal) {
        for(int ii=0, n=dwWVec.length; ii<n; ii+=2) {
            double dm = dwWVec[ii]-minVal;
            dwWVec[ii] = (dm + Math.abs(dm))/2 + minVal;
        }
    }

}
