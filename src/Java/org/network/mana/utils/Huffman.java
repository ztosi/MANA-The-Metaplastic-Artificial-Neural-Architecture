package Java.org.network.mana.utils;

import com.jmatio.io.MatFileReader;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt64;

import java.util.Collections;
import java.util.PriorityQueue;

public class Huffman {

    public static void readMat4Huff(String filename) {
        try {
            MatFileReader mfr = new MatFileReader(filename);
            MLDouble freqs = (MLDouble) mfr.getMLArray("freq");
            double[][] fd = freqs.getArray();
            double [] dummy = new double[fd.length];
            for(int ii=0, n = dummy.length; ii<n; ++ii) {
                dummy[ii] = fd[ii][0];
            }
            long[] symbols = huffman(dummy);
            new MatFileWriter("symbols.mat", Collections.singleton(new MLInt64("symbols", symbols, 1)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long[] huffman(double[] frequencies) {

        PriorityQueue<Node> queue = new PriorityQueue<>((Node a, Node b) -> {
            if (a.weight < b.weight) {
                return -1;
            } else if (a.weight > b.weight) {
                return 1;
            } else {
                return 0;
            }
        });

        for (int ii = 0, n = frequencies.length; ii < n; ++ii) {
            queue.add(new Node(ii, frequencies[ii]));
        }

        while (queue.size() >= 2) {
            Node newNode = Node.buildInternal(queue.poll(), queue.poll());
            queue.add(newNode);
        }

        long[] codes = new long[frequencies.length];
        assignCodes(queue.poll(), 0, true, codes);
        return codes;

    }


    private static void assignCodes(Node root, long code, boolean bit, long[] codes) {
        code <<= 1;
        if (bit) {
            code |= 1;
        }
        if (root.isLeaf()) {
            codes[root.index] = code;
            return;
        } else {
            assignCodes(root.leaf1, code, true, codes);
            assignCodes(root.leaf2, code, false, codes);
        }

    }

    public static void main(String[] args) {

        if (args.length == 0) {
            return;
        } else {
            readMat4Huff(args[0]);
        }

        // double [] freqs = new double[20];
        // double sum = 0;
        // for(int ii=0; ii<20; ++ii) {
        // 	freqs[ii] = Math.exp(10*ThreadLocalRandom.current().nextGaussian());
        // 	sum += freqs[ii];
        // }
        // for(int ii=0; ii<20; ++ii) {
        // 	freqs[ii] /= sum;
        // }

        // long[] bob = Huffman.huffman(freqs);

        // for(int ii=0; ii<20; ++ii) {
        // 	System.out.println(ii + "   " + freqs[ii] + "  " + Long.toBinaryString(bob[ii]));
        // }

    }

    private static class Node {

        public final double weight;
        public final Node leaf1;
        public final Node leaf2;
        public Node parent;
        public boolean visited = false;
        private int index;

        private Node(Node leaf1, Node leaf2) {
            this.leaf1 = leaf1;
            this.leaf2 = leaf2;
            weight = leaf1.weight + leaf2.weight;
            index = -1;
        }

        public Node(int index, double weight) {
            this.index = index;
            this.weight = weight;
            leaf1 = null;
            leaf2 = null;
        }

        public static Node buildInternal(Node leaf1, Node leaf2) {
            Node newNode = new Node(leaf1, leaf2);
            leaf1.parent = newNode;
            leaf2.parent = newNode;
            return newNode;
        }

        public boolean isLeaf() {
            return leaf2 == null && leaf1 == null;
        }

        public int getIndex() {
            return index;
        }

    }


}
