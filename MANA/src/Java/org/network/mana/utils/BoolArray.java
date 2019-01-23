package Java.org.network.mana.utils;

import java.util.Arrays;

public class BoolArray {

    private final long ONE_MASK = 0x0000000000000001;

    public final int length;

    private final long[] data;

    public BoolArray(final int length) {
        this.length = length;
        data = new long[(int) Math.ceil(length/64)+1];
    }

    public BoolArray(BoolArray toCpy) {
        this.length = toCpy.length;
        data = new long[(int) Math.ceil(length/64) + 1];
        System.arraycopy(toCpy.data, 0, data, 0, data.length);
    }

    public void set(int index, boolean value) {
        long mask = ONE_MASK << (63 - (index % 64));
        if(value) {
            data[index / 64] |= mask;
        } else {
            data[index / 64] &= ~mask;
        }
    }

    public boolean get(int index) {
        return ((data[index/64] >>> (63 - (index % 64))) & ONE_MASK) == 1;
    }

    public void copyInto(BoolArray toCopyIn) {
        System.arraycopy(toCopyIn.data, 0, data, 0, data.length);
    }

    public void clear() {
        Arrays.fill(data, 0);
    }


    public static void main(String [] args) {
        BoolArray bob = new BoolArray(100);
        System.out.println(bob.get(79));
        bob.set(79, true);
        System.out.println(bob.get(79));
        System.out.println(bob.get(80));
        bob.set(80, true);
        System.out.println(bob.get(79));
        System.out.println(bob.get(80));
        bob.set(79, false);
        System.out.println(bob.get(79));
        System.out.println(bob.get(80));

    }

}
