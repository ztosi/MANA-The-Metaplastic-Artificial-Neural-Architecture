package base_components;

import utils.SMOperation;

import java.util.Arrays;

public class SparseMatrix {

    public static final double DEF_INIT_VAL = 0.01;

    private double [] values;
    private int [] tarPtr;
    private int [] srcIndices;
    private int noTars;
    private int noSrcs;
    private final int nILFac;

    public SparseMatrix(int [][] tarSrcMap, int noTars, int noSrcs) {
        this(tarSrcMap, noTars, noSrcs, 1);
    }

    public SparseMatrix(int [][] tarSrcMap, int noTars, int noSrcs, int nValsPerCoordinate) {
        nILFac = nValsPerCoordinate;
        this.noSrcs = noSrcs;
        this.noTars = noTars;
        int numVals = 0;
        tarPtr = new int[tarSrcMap.length+1];
        int accum = 0;
        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
                Arrays.sort(tarSrcMap[ii]);
                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
                    numVals++;
                }
                tarPtr[ii] = accum;
                accum += tarSrcMap[ii].length;
            } else {
                tarPtr[ii] = -1;
            }
        }
        tarPtr[tarPtr.length-1] = numVals;

        // Go through and replace all targets with no sources with the index of the start of the next target
        for(int ii=0; ii<tarPtr.length-1; ++ii) {
            if(tarPtr[ii] == -1) {
                tarPtr[ii] = tarPtr[ii+1];
            }
        }

        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
                    numVals++;
                }
            }
        }

        values = new double[numVals * nILFac];
        srcIndices = new int[numVals];
        numVals = 0;
        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
                    srcIndices[numVals] = tarSrcMap[ii][jj];
                    ++numVals;
                }
            }
        }
        Arrays.fill(values, 0, values.length, DEF_INIT_VAL);

    }

    public double get(int tarInd, int srcInd) {
        return get(tarInd, tarInd, 0, 1);
    }

    public double get(int tarInd, int srcInd, int start, int inc) {
        checkInc(inc);
        if (tarPtr[tarInd] == tarPtr[tarInd+1])
            return 0;
        if (tarInd > noTars)
            throw new IllegalArgumentException("Invalid target index.");
        if (srcInd > noSrcs)
            throw new IllegalArgumentException("Invalid source index.");

        for(int ii=tarPtr[tarInd]; ii < tarPtr[tarInd+1]; ++ii) {
            if (srcIndices[ii] == srcInd) {
                return values[ii*inc + start];
            } else if (srcIndices[ii] > srcInd) {
                return 0;
            }
        }
        return  0;
    }




    public void set(int tarInd, int srcInd, double val) {
//        if (tarPtr[tarInd] < 0) {
//            tarPtr[tarInd] = tarPtr[tarInd-1] + 1;
//            int [] newSrcInds = new int[srcIndices.length+1];
//            for(int ii=tarInd+1; ii < noTars; ++ii) {
//                tarPtr[ii]++;
//
//            }
//        }

        // Most Common case first
        for(int ii=tarPtr[tarInd], n=tarPtr[tarInd+1]; ii<n; ++ii) {
            if (srcIndices[ii] == srcInd) {
                values[ii] = val;
            }
        }
    }
    // TODO: Implement binary search for larger numbers of sources/targets
    public int sub2Ind(int tar, int src) {
        for(int ii=tarPtr[tar], n=tarPtr[tar]; ii<n; ++ii) {
            if (srcIndices[ii] == src) {
                return ii;
            } else if (srcIndices[ii] > src) {
                return -1;
            }
        }
    }

    public void sumIncoming(double[] localSums, int start, int inc) {
        checkInc(inc);
        if(localSums.length != noTars) {
            throw new IllegalArgumentException("Dimension mismatch between location of stored sums and number of targets");
        }
        for(int ii=0; ii < noTars; ++ii) {
            localSums[ii] = 0;
            for(int jj=tarPtr[ii], n=tarPtr[ii+1]; jj<n; ++jj) {
                localSums[ii] += values[jj*inc + start];
            }
        }
    }

    public void reduction(SMOperation op, double [] result, double[] srcVars, double [] tarArgs) {

        for(int ii=0; ii<noTars; ++ii) {
            result[ii] = 0;
            for(int jj=tarPtr[ii], n=tarPtr[ii+1]; jj<n; ++jj) {
                result[ii] += op.op(tarArgs[ii], srcVars[srcIndices[jj]]);
            }

        }

    }

   // public void update(SMOperation op, double[] srcBlock )

    public void reductionExtraTarArg(SMOperation op, double [] result, double[] srcVars,
                                     double [] tarArgs1, double [] tarArgs2) {
        for(int ii=0; ii<noTars; ++ii) {
            result[ii] = 0;
            for(int jj=tarPtr[ii], n=tarPtr[ii+1]; jj<n; ++jj) {
                result[ii] += op.op(tarArgs1[ii], tarArgs2[ii], srcVars[srcIndices[jj]]);
            }
        }
    }

    public void scalarMult(double a) {
        scalarMult(a, 0, 1);
    }


    public void scalarMult(double a, int start, int inc) {
        checkInc(inc);
        for(int ii=0; ii<noTars; ++ii) {
            for(int jj=tarPtr[ii]+start, n=nILFac*tarPtr[ii+1]; jj<n; jj+=inc) {
                values[jj] *= a;
            }
        }
    }

    private void checkInc(int inc) {
        if (inc > nILFac) {
            throw new IllegalArgumentException("Increment is larger than number of interleaved values");
        }
    }

    public void scalarAdd(double a, int start, int inc) {
        checkInc(inc);
        for(int ii=0; ii<noTars; ++ii) {
            for(int jj=tarPtr[ii]+start, n=nILFac*tarPtr[ii+1]; jj<n; jj+=inc) {
                values[jj] += a;
            }
        }
    }

    private void insertTar() {

    }

}
