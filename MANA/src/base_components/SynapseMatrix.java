package base_components;

import base_components.enums.OrderType;
import utils.SMOperation;
import utils.SrcTarDataPack;

import java.util.*;

/**
 * A special sort of sparse matrix designed speifically for containing values pertinent to synapses and the neurons
 * which send/receive signals over them. Values can be interleaved in target-major ordering, but also allows for the
 * storage of different values in source major fashion, since optimal data continuity for different synapse operations is
 * different.
 * @author Zoe Tosi
 */
public class SynapseMatrix {

    private final OrderType ordering;

    /** Data values, may be interleaved--interleaving facor is nILFac
     * Arranged in the order of target-major, i.e. the fan-in of each neuron is contiguous. */
    private double [] values;
    /** The position in values where each target neuron's fan in begins capped at the end with the length of values*/
    private int [] ptrs;
    /** The source neuron indices of each value in values .*/
    private int [] ordIndices;
    /** Number of target neurons. */
    private int noMajor;
    /** Number of source neurons. */
    private int noMinor;
    /** Number of interleaved values in values.*/
    private final int nILFac;
    /** Number of non-zero values.*/
    private int nnz;

    public final int offsetMajor;

    public final int offsetMinor;
//
    private int[] reverseDegrees;

    public SynapseMatrix(List<SrcTarDataPack> tuples, int noMinor, int noMajor, int offsetMajor, int offsetMinor,
                         final OrderType ordering) {
        this.offsetMajor = offsetMajor;
        this.offsetMinor = offsetMinor;
        this.ordering = ordering;
        nnz = tuples.size();
        nILFac = tuples.get(0).values.length;
        this.noMinor = noMinor;
        this.noMajor = noMajor;
        int [] majorDegrees = new int[noMajor];
        reverseDegrees = new int[noMinor];

        Collections.sort(tuples, orderTypeTupleComp());
        // Now that tuples is sorted right, we can fill in values & ordIndices while calculating the in and out degrees to get the
        // target and source index arrays
        values = new double[nnz*nILFac];
        ordIndices = new int[nnz];
        int index = 0;
        for(SrcTarDataPack tup : tuples) {
            if (ordering == OrderType.TARGET) {
                reverseDegrees[tup.coo.src]++;
                majorDegrees[tup.coo.tar]++;
            } else {
                reverseDegrees[tup.coo.tar]++;
                majorDegrees[tup.coo.src]++;
            }
            ordIndices[index] = ordering == OrderType.TARGET ? tup.coo.src : tup.coo.tar;
            System.arraycopy(tup.values, 0, values, nILFac*index, tup.values.length);
            index++;
        }
        ptrs = new int[noMajor+2];
        for(int ii=0; ii<noMajor; ++ii) {
            ptrs[ii+1] = majorDegrees[ii] + ptrs[ii];
        }

    }

    /**
     * Provides the appropriate comparator for sorting a COO as tuples in a list
     * appropriate to the ordering (source or target major). Tuples must be:
     * {src#, tar#, vals...}
     * @return
     */
    private Comparator<SrcTarDataPack> orderTypeTupleComp() {
        if (ordering == OrderType.TARGET) {
            return (SrcTarDataPack a, SrcTarDataPack b) ->
            {
                if (a.coo.tar < b.coo.tar) {
                    return -1;
                } else if (a.coo.tar > b.coo.tar) {
                    return 1;
                } else {
                    if (a.coo.src < b.coo.src)
                        return -1;
                    else if (a.coo.src > b.coo.src)
                        return 1;
                    else
                        return 0;
                }
            };
        } else {
            return (SrcTarDataPack a,SrcTarDataPack b) ->
            {
                if (a.coo.src < b.coo.src) {
                    return -1;
                } else if (a.coo.src > b.coo.src) {
                    return 1;
                } else {
                    if (a.coo.tar < b.coo.tar)
                        return -1;
                    else if (a.coo.tar > b.coo.tar)
                        return 1;
                    else
                        return 0;
                }
            };
        }
    }

//    /**
//     *
//     * @param tarSrcMap
//     * @param noMajor
//     * @param noMinor
//     * @param nValsPerCoordinate
//     */
//    public SynapseMatrix(int [][] tarSrcMap, int noMajor, int noMinor, int nValsPerCoordinate, OrderType ordering) {
//        this.ordering = ordering;
//        this.tarSrcMap = tarSrcMap;
//        nILFac = nValsPerCoordinate;
//        this.noMinor = noMinor;
//        this.noMajor = noMajor;
//        int numVals = 0;
//        ptrs = new int[tarSrcMap.length+2];
//        int accum = 0;
//        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
//            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
//                Arrays.sort(tarSrcMap[ii]); // Sort so that src indices are sorted
////                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
////                    numVals++;
////                }
//                ptrs[ii+1] = tarSrcMap[ii].length;
//                numVals++;
//            }
//        }
//        nnz = numVals;
//        ptrs[ptrs.length-1] = numVals;
//        for(int ii = 1; ii<(ptrs.length-1); ++ii) {
//            ptrs[ii] += ptrs[ii-1];
//        }
//
//        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
//            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
//                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
//                    numVals++;
//                }
//            }
//        }
//
//        values = new double[numVals * nILFac];
//        ordIndices = new int[numVals];
//        int [][] srcTarInd = new int[numVals][3];
//
//        numVals = 0;
//        for(int ii = 0; ii < tarSrcMap.length; ++ii) {
//            if (tarSrcMap[ii] != null && tarSrcMap[ii].length !=0) {
//                for (int jj = 0; jj < tarSrcMap[ii].length; ++jj) {
//                    ordIndices[numVals] = tarSrcMap[ii][jj];
//                    srcTarInd[numVals] = new int[]{tarSrcMap[ii][jj], ii, jj};
//                    ++numVals;
//                }
//            }
//        }
//        Arrays.fill(values, 0, values.length, DEF_INIT_VAL);
//
//        // For the src targ map, sort the 3-tuples {srcIndex, tarIndex, indexInTarg'sFanin}
//        // first by srcIndex, then by tarIndex.
//        Arrays.sort(srcTarInd, (int[] a, int[] b) ->
//        {
//            if(a[0] < b[0]) {
//                if (a[1] < b[1]) {
//                    return -1;
//                } else if (a[1] > b[1]) {
//                    return 1;
//                } else {
//                    return 0;
//                }
//            } else if (a[0] > b[0]) {
//                return 1;
//            } else {
//                return 0;
//            }
//        }
//        );
//        // The pointer to where each src's target indexes ends
//        srcPtr = new int[noMinor+2];
//        srcPtr[srcPtr.length-1] = numVals;
//        // We multiply by 2 because we want to store the target as
//        // well as this source's location in its target's fanIn,so
//        // we don't have to search for it each time
//        tarIndices = new int[srcTarInd.length * 2];
//        for(int ii=0, n = srcTarInd.length; ii<n; ++ii) {
//            // Follow the 3-tuples now sorted by src then targ and
//            // assign to the srcPtr how many outgoing c's this has.
//            srcPtr[srcTarInd[ii][0]+1]++;
//            tarIndices[2*ii] = srcTarInd[ii][1]; // target location
//            tarIndices[2*ii + 1] = srcTarInd[ii][2]; // location in target's fanIn
//        }
//
//        for(int ii=1; ii<(srcPtr.length-1); ++ii) {
//            srcPtr[ii] += srcPtr[ii-1];
//        }
//
//
//    }

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




    public void set(int tarInd, int srcInd, double val) {
//        if (ptrs[tarInd] < 0) {
//            ptrs[tarInd] = ptrs[tarInd-1] + 1;
//            int [] newSrcInds = new int[ordIndices.length+1];
//            for(int ii=tarInd+1; ii < noMajor; ++ii) {
//                ptrs[ii]++;
//
//            }
//        }

        // Most Common case first
        for(int ii = ptrs[tarInd], n = ptrs[tarInd+1]; ii<n; ++ii) {
            if (ordIndices[ii] == srcInd) {
                values[ii] = val;
            }
        }
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

    public void sumIncoming(double[] localSums, int start, int inc) {
        checkInc(inc);
        if(localSums.length != noMajor) {
            throw new IllegalArgumentException("Dimension mismatch between location of stored sums and number of targets");
        }
        for(int ii = 0; ii < noMajor; ++ii) {
            localSums[ii] = 0;
            for(int jj = ptrs[ii], n = ptrs[ii+1]; jj<n; ++jj) {
                localSums[ii] += values[jj*inc + start];
            }
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


    public void scalarMult(double a, int start, int inc) {
        checkInc(inc);
        for(int ii = 0; ii< noMajor +1; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] *= a;
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

    private void checkInc(int inc) {
        if (inc > nILFac) {
            throw new IllegalArgumentException("Increment is larger than number of interleaved values");
        }
    }

    public void scalarAdd(double a, int start, int inc) {
        checkInc(inc);
        for(int ii = 0; ii< noMajor; ++ii) {
            for(int jj = ptrs[ii]+start, n = nILFac* ptrs[ii+1]; jj<n; jj+=inc) {
                values[jj] += a;
            }
        }
    }

    private void insertTar() {

    }

    private double[][] tarSrcMapFromTuples(List<double[]> tuples) {
        return null;
        //TODO: write this function!
    }

    /** Converts the nonzero element coordinates in tar/source major used here into COO coordinates where each
     * synapse consists of a tuple: {source #, target #, interleaved vals...}
     * @return a linked list of tuples containing the above information in that order.
     */
    private LinkedList<double[]> conv2Tuples() {
        return null;
        //TODO: Do I need this anymore?

//        int noV = 2+nILFac + 1;
//
//        LinkedList<double[]> tups = new LinkedList<>();
//        for(int ii = 1; ii<(noMajor +1); ++ii) {
//            for(int jj = ptrs[ii-1]; jj< ptrs[ii]; ++jj) {
//                double [] els = new double[noV];
//                els[0] = ii;
//                els[1] = ordIndices[jj];
//                for(int kk = 0; kk<nILFac; ++kk) {
//                    els[2+kk] = values[jj*nILFac + kk];
//                }
//                tups.add(els);
//            }
//        }
//        tups.sort( (double[] a, double[] b) ->
//                {
//                    if(a[1] < b[1]) {
//                        if (a[0] < b[0]) {
//                            return -1;
//                        } else if (a[0] > b[0]) {
//                            return 1;
//                        } else {
//                            return 0;
//                        }
//                    } else if (a[1] > b[1]) {
//                        return 1;
//                    } else {
//                        return 0;
//                    }
//                }
//        );
//
//        int index = 0;
//        for (double [] tuple : tups) {
//            tuple[noV-1] = srcOrdVals[index++];
//        }
//
//        tups.sort( (double[] a, double[] b) ->
//                {
//                    if(a[0] < b[0]) {
//                        if (a[1] < b[1]) {
//                            return -1;
//                        } else if (a[1] > b[1]) {
//                            return 1;
//                        } else {
//                            return 0;
//                        }
//                    } else if (a[0] > b[0]) {
//                        return 1;
//                    } else {
//                        return 0;
//                    }
//                }
//        );
//
//        return tups;
    }

}
