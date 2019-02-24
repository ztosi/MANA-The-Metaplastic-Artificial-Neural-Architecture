package Java.org.network.mana.base_components.sparse;

import Java.org.network.mana.enums.Ordering;
import Java.org.network.mana.utils.Utils;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * A special sort of sparse matrix designed speifically for containing values pertinent to synapses and the neurons
 * which send/receive signals over them. Values can be interleaved in target-major ordering, but also allows for the
 * storage of different values in source major fashion, since optimal data continuity for different synapse operations is
 * different.
 * @author Zoë Tosi
 * TODO: Clean up legacy methods...
 */
public class InterleavedSparseMatrix {

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

   // public final int offsetMajor;

    //public final int offsetMinor;
//
    private int[] reverseDegrees;

    public InterleavedSparseMatrix(List<SrcTarDataPack> tuples, int[] dataRange, int noMinor, int noMajor, //int offsetMajor, int offsetMinor,
                                   final Ordering ordering) {
      //  this.offsetMajor = offsetMajor;
      //  this.offsetMinor = offsetMinor;
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
    public double get(int tarInd, int srcInd) {
        return get(tarInd, tarInd, 0, 1);
    }

    public double get(int tarInd, int srcInd, int start, int inc) {
        checkOffset(inc);
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
        checkOffset(offset);
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

    public void randomize(Utils.ProbDistType pdist, double[] params, int offset) {
        for(int ii = 0; ii<nnz; ++ii) {
            values[ii*nILFac + offset] = pdist.getRandom(params[0], params[1]);
        }
    }


    public void getInCOO(int[] src, int[] tar, double[] wt, int offset) {
        // TODO: Add checks... and options for ranges in args...
        int kk = 0;
        for(int ii=0; ii<noMajor; ++ii) {
            for(int jj=ptrs[ii]; jj<ptrs[ii+1]; ++jj) {
                tar[kk] = ii;
                src[kk] = ordIndices[jj];
                wt[kk++] = values[jj*nILFac+offset];
            }

        }
    }

    public void scalarMult(double a) {
        scalarMult(a, 0, 1);
    }


    //public void

    public void scalarMult(double a, int start, int inc) {
        checkOffset(inc);
        for(int ii = 0; ii< noMajor +1; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] *= a;
            }
        }
    }

    public void scaleMajor(int majorInd, double scale, int offset) {
        checkOffset(offset);
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
     * @param offset
     */
    private void checkOffset(int offset) {
        if (offset >= nILFac) {
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
        checkOffset(inc);
        for(int ii = 0; ii< noMajor; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] += a;
            }
        }
    }

    public double getMax(int offset) {
        double max = Double.MIN_VALUE;
        for(int ii=offset; ii<values.length; ii+=nILFac) {
            if(values[ii] > max) {
                max = values[ii];
            }
        }
        return max;
    }

    public void getMaxMajors(int offset, double [] mxs) {
        for(int ii=0; ii<noMajor; ++ii) {
            for(int jj = ptrs[ii]; jj < ptrs[ii+1]; ++jj) {
                double val = values[jj*nILFac + offset];
                if(val > mxs[ii]) {
                    mxs[ii] = val;
                }
            }
        }
    }

    public double getMin(int offset) {
        double min = Double.MAX_VALUE;
        for(int ii=offset; ii<values.length; ii+=nILFac) {
            if(values[ii] < min) {
                min = values[ii];
            }
        }
        return min;
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

    public double [] getValues(int offset) {
        checkOffset(offset);
        double[] cpy = new double[nnz];
        for(int ii=0; ii<nnz; ++ii) {
            cpy[ii] = values[ii*nILFac + offset];
        }
        return cpy;
    }

    /**
     * Returns the values at a given offset (if interleaved)
     * @param vals
     * @param absShift
     * @param offset
     */
    public void getValues(double[] vals, int absShift, int offset) {
        checkOffset(offset);
        for(int ii=0; ii<nnz; ++ii) {
            vals[ii+absShift] = values[ii*nILFac + offset];
        }
    }

    public int[] getMajorDegrees() {
        int[] degs = new int[noMajor];
        for(int ii=0; ii<noMajor; ++ii) {
            degs[ii] = ptrs[ii+1]-ptrs[ii];
        }
        return degs;
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

    public int[] getPtrsAsIndices() {
        int[] inds = new int[nnz];
        getPtrsAsIndices(inds, 0, 0);
        return inds;
    }

    public void getPtrsAsIndices(int [] inds, int absShift, int relativeShift) {
        for(int ii=0; ii<noMajor; ++ii) {
            for(int jj=ptrs[ii]; jj<ptrs[ii+1]; ++jj) {
                inds[jj+absShift] = ii + relativeShift;
            }
        }
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

    public void getIndices(int [] inds, int absShift, int relativeShift) {
        System.arraycopy(ordIndices, 0, inds, absShift, nnz);
        for(int ii=absShift, n=absShift+nnz; ii<n; ++ii) {
            inds[ii] += relativeShift;
        }
    }

    public int getDataForMajorInd(int majorInd, int offset, double [] ret, int start) {
        int jj = 0;
        try {
            for (int ii = ptrs[majorInd], n = ptrs[majorInd + 1]; ii < n; ++ii) {
                ret[start + jj++] = values[nILFac * ii + offset];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return ptrs[majorInd+1] - ptrs[majorInd];
    }

    public int getStartIndex(int neuronNo) {
        return ptrs[neuronNo] * nILFac;
    }

    public int getEndIndex(int neuronNo) {
        return ptrs[neuronNo+1] * nILFac;
    }

    public int getInc() {
        return nILFac;
    }

    public int getStartIndex(int neuronNo, int nILFac) {
        return ptrs[neuronNo] * nILFac;
    }

    public int getEndIndex(int neuronNo, int nILFac) {
        return ptrs[neuronNo+1] * nILFac;
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
