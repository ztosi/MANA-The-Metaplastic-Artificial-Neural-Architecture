package Java.org.network.mana.base_components.enums;

import Java.org.network.mana.base_components.MANANeurons;

//TODO: Make this an interface
public enum DampFunction {



    HARD_BOUND {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
//            hardMinBound(dwWVec, minVal);
            for(int ii=dwWVec.length-2; ii>=0; ii-=2) {
//                double dm = dwWVec[ii] - maxVal;
//                dwWVec[ii] = maxVal - (dm - Math.abs(dm))/2;
                if(dwWVec[ii] > maxVal) {
                    dwWVec[ii] = maxVal;
                } else if (dwWVec[ii] < minVal) {
                    dwWVec[ii] = minVal;
                }

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
    }, POLY6_BOUND {
        @Override
        public void dampen(double[] dwWVec, double maxVal, double minVal) {
                for(int ii=1; ii<dwWVec.length; ii+=2) {
                    if(dwWVec[ii-1] < minVal) {
                        dwWVec[ii-1] = minVal;
                        if(dwWVec[ii] < 0) {
                            dwWVec[ii] = 0;
                        }
                    }
                }
            for(int ii=1; ii<dwWVec.length; ii+=2) {
                if(dwWVec[ii]>0) {
                    dwWVec[ii] *= -Math.pow(dwWVec[ii-1]/maxVal, 6) + 1;
                }
            }
            for(int ii=1; ii<dwWVec.length; ii+=2) {
                if(Math.abs(dwWVec[ii])>1) {
                    dwWVec[ii] = Math.signum(dwWVec[ii]);
                }
            }
        }
    }, NO_MAX {
        @Override
        public final void dampen(final double[] dwWVec,
                                 double maxVal, double minVal) {
            hardMinBound(dwWVec, minVal);
        }
    };//,
//        NONE{
//            @Override
//            public final void dampen(final double[] dwWVec,
//            double maxVal, double minVal) {
//                return;
//            }
//    };

    public static final DampFunction DEF_DAMPENER = DampFunction.POLY6_BOUND;

    public abstract void dampen(final double[] dwWVec, double maxVal, double minVal);

    public static void hardMinBound(final double[] dwWVec, final double minVal) {
        for(int ii=0, n=dwWVec.length; ii<n; ii+=2) {
            double dm = dwWVec[ii]-minVal;
            dwWVec[ii] = (dm + Math.abs(dm))/2 + minVal;
        }
    }

}
