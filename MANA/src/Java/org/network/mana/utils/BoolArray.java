package Java.org.network.mana.utils;

public class BoolArray {

    public final int length;

    private final long[] data;

    public BoolArray(final int length) {
        this.length = length;
        data = new long[(int) Math.ceil(length/64)];
    }

    public BoolArray(BoolArray toCpy) {
        this.length = toCpy.length;
        data = new long[(int) Math.ceil(length/64)];
        System.arraycopy(toCpy.data, 0, data, 0, data.length);
    }

    public void set(int index, boolean value) {
        long mask = 1 << (63 - (index % 64));
        data[index/64] |= mask;
    }

    public boolean get(int index) {
        return (data[index/64] >>> (index % 64)) % 2 == 1;
    }

    public void copyInto(BoolArray toCopyIn) {
        System.arraycopy(toCopyIn, 0, data, 0, data.length);
    }

}
