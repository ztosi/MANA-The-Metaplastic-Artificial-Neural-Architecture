package base_components.Matrices;

import base_components.enums.Ordering;
import utils.SMOperation;
import utils.SrcTarDataPack;

import java.io.PrintStream;
import java.util.*;

/**
 * A special sort of sparse matrix designed speifically for containing values pertinent to synapses and the neurons
 * which send/receive signals over them. Values can be interleaved in target-major ordering, but also allows for the
 * storage of different values in source major fashion, since optimal data continuity for different synapse operations is
 * different.
 * @author Zoe Tosi
 */
public class SynapseMatrix {

    private final Ordering ordering;

    /** Data values, may be interleaved--interleaving facor is nILFac
     * Arranged in the order of target-major, i.e. the fan-in of each neuron is contiguous. */
    private double [] values;
    /** The position in values where each target neuron's fan in begins capped at the end with the length of values
     * size is num major + 1*/
    private int [] ptrs;
    /** The source neuron indices of each value in values; size is nnz/nilFac .*/
    private int [] ordIndices;
    /** Number of target neurons. */
    private int noMajor;
    /** Number of source neurons. */
    private int noMinor;
    /** Number of interleaved values in values.*/
    public final int nILFac;
    /** Number of non-zero values.*/
    private int nnz;

    public final int offsetMajor;

    public final int offsetMinor;
//
    private int[] reverseDegrees;

    public SynapseMatrix(List<SrcTarDataPack> tuples, int[] dataRange, int noMinor, int noMajor, int offsetMajor, int offsetMinor,
                         final Ordering ordering) {
        this.offsetMajor = offsetMajor;
        this.offsetMinor = offsetMinor;
        this.ordering = ordering;
        nnz = tuples.size();
        nILFac = dataRange[1] - dataRange[0];
        this.noMinor = noMinor;
        this.noMajor = noMajor;
        int [] majorDegrees = new int[noMajor];
        reverseDegrees = new int[noMinor];

        Collections.sort(tuples, Ordering.orderTypeTupleComp(ordering));
        // Now that tuples is sorted right, we can fill in values & ordIndices while calculating the in and out degrees to get the
        // target and source index arrays
        values = new double[nnz*nILFac];
        ordIndices = new int[nnz];
        int index = 0;
        for(SrcTarDataPack tup : tuples) {
            if (ordering == Ordering.TARGET) {
                reverseDegrees[tup.coo.src]++;
                majorDegrees[tup.coo.tar]++;
            } else {
                reverseDegrees[tup.coo.tar]++;
                majorDegrees[tup.coo.src]++;
            }
            ordIndices[index] = ordering == Ordering.TARGET ? tup.coo.src : tup.coo.tar;
            System.arraycopy(tup.values, dataRange[0], values, nILFac*index, nILFac);
            index++;
        }
        ptrs = new int[noMajor+1];
        for(int ii=0; ii<noMajor; ++ii) {
            ptrs[ii+1] = majorDegrees[ii] + ptrs[ii];
        }

    }
    //TODO: Resolve issue of ptrs being not absolute

    public double get(int tarInd, int srcInd) {
        return get(tarInd, tarInd, 0, 1);
    }

    public double get(int tarInd, int srcInd, int start, int inc) {
        checkInc(inc);
        if (ptrs[tarInd] == ptrs[tarInd+1])
            return 0;
        if (tarInd > noMajor)
            throw new IllegalArgumentException("Invalid target index.");
        if (srcInd > noMinor)
            throw new IllegalArgumentException("Invalid source index.");

        for(int ii = ptrs[tarInd]; ii < ptrs[tarInd+1]; ++ii) {
            if (ordIndices[ii] == srcInd) {
                return values[ii*inc + start];
            } else if (ordIndices[ii] > srcInd) {
                return 0;
            }
        }
        return  0;
    }





    // TODO: Implement binary search for larger numbers of sources/targets
    public int sub2Ind(int tar, int src) {
        for(int ii = ptrs[tar], n = ptrs[tar]; ii<n; ++ii) {
            if (ordIndices[ii] == src) {
                return ii;
            } else if (ordIndices[ii] > src) {
                return -1;
            }
        }
        return -1;
    }

    public void sumIncoming(double[] localSums, int offset) {
        if (Math.abs(offset) >= nILFac) {
            throw new IllegalArgumentException("Invalid offset");
        }
        if(localSums.length != noMajor) {
            throw new IllegalArgumentException("Dimension mismatch between location of stored sums and number of targets");
        }
        for(int ii = 0; ii < noMajor; ++ii) {
            localSums[ii] = 0;
            for(int jj = ptrs[ii], n = ptrs[ii+1]; jj<n; ++jj) {
                localSums[ii] += values[jj*nILFac + offset];
            }
        }
    }

    public double getMajorSum(int noMajor, int offset) {
        checkInc(offset);
        double su = 0;
        for(int ii=ptrs[noMajor]; ii < ptrs[noMajor+1]; ++ii) {
            su += values[ii*nILFac+offset];
        }
        return su;
    }

    /**
     * A convenience method that probably should be somewhere else. Adds the 2i+1-ith element to the 2i-ith and keeps it
     * there. This is specifically for adding the derivative of the weights to the weights.
     */
    public void addDw2W() {
        for(int ii=0, n=nnz*nILFac; ii<n; ii+=nILFac) {
            values[ii] += values[ii+1];
        }
    }

    public void reduction(SMOperation op, double [] result, double[] srcVars, double [] tarArgs) {

        for(int ii = 0; ii< noMajor; ++ii) {
            result[ii] = 0;
            for(int jj = ptrs[ii], n = ptrs[ii+1]; jj<n; ++jj) {
                result[ii] += op.op(tarArgs[ii], srcVars[ordIndices[jj]]);
            }

        }

    }

   // public void update(SMOperation op, double[] srcBlock )

    public void reductionExtraTarArg(SMOperation op, double [] result, double[] srcVars,
                                     double [] tarArgs1, double [] tarArgs2) {
        for(int ii = 0; ii< noMajor; ++ii) {
            result[ii] = 0;
            for(int jj = ptrs[ii], n = ptrs[ii+1]; jj<n; ++jj) {
                result[ii] += op.op(tarArgs1[ii], tarArgs2[ii], srcVars[ordIndices[jj]]);
            }
        }
    }

    public void scalarMult(double a) {
        scalarMult(a, 0, 1);
    }


    //public void

    public void scalarMult(double a, int start, int inc) {
        checkInc(inc);
        for(int ii = 0; ii< noMajor +1; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] *= a;
            }
        }
    }

    public void scaleMajor(int majorInd, double scale, int offset) {
        checkInc(offset);
        for(int ii=ptrs[majorInd]; ii<ptrs[majorInd+1]; ++ii) {
            values[ii*nILFac+offset] *= scale;

        }
    }

    public final void divFromArray(double[] arr, int offset) {
        if(arr.length != noMajor) {
            throw new IllegalArgumentException("Dimension mismatch");
        }
        for(int ii=0; ii<noMajor; ++ii) {
            for(int jj=ptrs[ii], m = ptrs[ii+1]; jj<m; ++jj) {
                values[jj*nILFac + offset] /= arr[ii];
            }
        }
    }

    public final void mulFromArray(double[] arr, int offset) {
        if(arr.length != noMajor) {
            throw new IllegalArgumentException("Dimension mismatch");
        }
        for(int ii=0; ii<noMajor; ++ii) {
            for(int jj=ptrs[ii], m = ptrs[ii+1]; jj<m; ++jj) {
                values[jj*nILFac + offset] *= arr[ii];
            }
        }
    }


    public void divMultFanIn(double [] divVal, double [] mulVal, int inc) {
        for(int ii = 0; ii< noMajor; ++ii) {
            for(int jj = ptrs[ii]; jj< ptrs[jj+1]; ++jj) {
                values[jj*nILFac + inc] = values[jj*nILFac + inc] * mulVal[ii] / divVal[ii];
            }
        }

    }

    public void sumNeighValsI(int a, int inc) {
        for(int ii=0; ii<nnz; ++ii) {
            values[ii*nILFac] += a * values[ii*nILFac+inc];
        }
    }

    /**
     * Checks that the increment makes sense considering the number of interleaved values in values
     * @param inc
     */
    private void checkInc(int inc) {
        if (inc > nILFac) {
            throw new IllegalArgumentException("Increment is larger than number of interleaved values");
        }
    }

    /**
     * Adds a scalar value to each element starting from a specific location and with a given
     * increment
     * @param a
     * @param start
     * @param inc
     */
    public void scalarAdd(double a, int start, int inc) {
        checkInc(inc);
        for(int ii = 0; ii< noMajor; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] += a;
            }
        }
    }

    /**
     * This returns the array of actual values--edits to the returned array will
     * alter values in the synapse matrix accordingly
     * @return
     */
    public double[] getRawData() {
        return values;
    }

    /**
     * This returns the array of pointers to the major ordered coordinates, size will be
     * same as either how many source neurons or how many target neurons for this synapse
     * matrix depending upon the order type. --edits to the returned array will
     * alter values in the synapse matrix accordingly
     * @return
     */
    public int[] getRawPtrs() {
        return  ptrs;
    }

    /**
     * This returns the array of minor coordinates--edits to the returned array will
     * alter values in the synapse matrix accordingly
     * @return
     */
    public int[] getRawOrdIndices() {
        return ordIndices;
    }

    /**
     * Returns a copy of the values stored in this synapse matrix
     * @return
     */
    public double[] getValues() {
        double [] cpy = new double[values.length];
        System.arraycopy(values, 0,cpy, 0, values.length);
        return cpy;
    }

    /**
     * Returns a copy of the major order pointers
     * @return
     */
    public int[] getPtrs() {
        int [] cpy = new int[ptrs.length];
        System.arraycopy(ptrs, 0, cpy, 0, ptrs.length);
        return cpy;
    }

    /**
     * Returns a copy of the minor order coordinates
     * @return
     */
    public int[] getOrdIndices() {
        int [] cpy = new int[ordIndices.length];
        System.arraycopy(ordIndices, 0, cpy, 0, ordIndices.length);
        return cpy;
    }

    public int getStartIndex(int neuronNo) {
        return ordIndices[neuronNo] * nILFac;
    }

    public int getEndIndex(int neuronNo) {
        return ordIndices[neuronNo+1] * nILFac;
    }

    public int getInc() {
        return nILFac;
    }

    public int getStartIndex(int neuronNo, int nILFac) {
        return ordIndices[neuronNo] * nILFac;
    }

    public int getEndIndex(int neuronNo, int nILFac) {
        return ordIndices[neuronNo+1] * nILFac;
    }

    public int getNnz() {
        return  nnz;
    }

    public void print(PrintStream out) {
        //TODO:...
    }

    //    private void insertTar() {
//
//    }
//
//    private double[][] tarSrcMapFromTuples(List<double[]> tuples) {
//        return null;
//        //TODO: write this function!
//    }
//
//    /** Converts the nonzero element coordinates in tar/source major used here into COO coordinates where each
//     * synapse consists of a tuple: {source #, target #, interleaved vals...}
//     * @return a linked list of tuples containing the above information in that order.
//     */
//    private LinkedList<double[]> conv2Tuples() {
//        return null;
//        //TODO: Do I need this anymore?
//
////        int noV = 2+nILFac + 1;
////
////        LinkedList<double[]> tups = new LinkedList<>();
////        for(int ii = 1; ii<(noMajor +1); ++ii) {
////            for(int jj = ptrs[ii-1]; jj< ptrs[ii]; ++jj) {
////                double [] els = new double[noV];
////                els[0] = ii;
////                els[1] = ordIndices[jj];
////                for(int kk = 0; kk<nILFac; ++kk) {
////                    els[2+kk] = values[jj*nILFac + kk];
////                }
////                tups.add(els);
////            }
////        }
////        tups.sort( (double[] a, double[] b) ->
////                {
////                    if(a[1] < b[1]) {
////                        if (a[0] < b[0]) {
////                            return -1;
////                        } else if (a[0] > b[0]) {
////                            return 1;
////                        } else {
////                            return 0;
////                        }
////                    } else if (a[1] > b[1]) {
////                        return 1;
////                    } else {
////                        return 0;
////                    }
////                }
////        );
////
////        int index = 0;
////        for (double [] tuple : tups) {
////            tuple[noV-1] = srcOrdVals[index++];
////        }
////
////        tups.sort( (double[] a, double[] b) ->
////                {
////                    if(a[0] < b[0]) {
////                        if (a[1] < b[1]) {
////                            return -1;
////                        } else if (a[1] > b[1]) {
////                            return 1;
////                        } else {
////                            return 0;
////                        }
////                    } else if (a[0] > b[0]) {
////                        return 1;
////                    } else {
////                        return 0;
////                    }
////                }
////        );
////
////        return tups;
//    }

}
