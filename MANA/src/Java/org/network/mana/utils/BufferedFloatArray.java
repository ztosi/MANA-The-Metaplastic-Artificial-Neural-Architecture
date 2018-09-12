package Java.org.network.mana.utils;

public class BufferedFloatArray {

    private final float[] dataAndBuff;

    public final int length;

    private int bufferOffset = 1;

    private int dataOffset = 0;

    public BufferedFloatArray(int length) {
        this.length = length;
        dataAndBuff = new float[2*length];
    }

    public float getBuffered(int index) {
        return dataAndBuff[bufferIndex(index)];
    }

    public float getData(int index) {
        return dataAndBuff[dataIndex(index)];
    }

    public void setBuffer(int index, float value) {
        dataAndBuff[bufferIndex(index)] = value;
    }

    public void setData(int index, float value) {
        dataAndBuff[dataIndex(index)] = value;
    }

    private int bufferIndex(int ind) {
        return 2*ind + bufferOffset;
    }

    private int dataIndex(int ind) {
        return  2*ind + dataOffset;
    }

    public void copyTo(final double[] out, int offset) {
        for(int ii=0; ii<length; ++ii) {
            out[ii+offset] = dataAndBuff[ii*2 + dataOffset];
        }
    }

    public void pushBufferShallow() {
        dataOffset = -(dataOffset - 1);
        bufferOffset = -(bufferOffset -1);
    }

    public void pushBufferDeep() {
        for(int ii=0; ii<length; ++ii) {
            dataAndBuff[dataIndex(ii)] = dataAndBuff[bufferIndex(ii)];
        }
    }
}
