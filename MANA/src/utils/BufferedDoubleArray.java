package utils;

public class BufferedDoubleArray {

    private final double[] dataAndBuff;

    public final int length;

    private int bufferOffset = 1;

    private int dataOffset = 0;

    public BufferedDoubleArray(int length) {
        this.length = length;
        dataAndBuff = new double[2*length];
    }

    public double getBuffered(int index) {
        return dataAndBuff[bufferIndex(index)];
    }

    public double getData(int index) {
        return dataAndBuff[dataIndex(index)];
    }

    public void setBuffer(int index, double value) {
        dataAndBuff[bufferIndex(index)] = value;
    }

    public void setData(int index, double value) {
        dataAndBuff[dataIndex(index)] = value;
    }

    private int bufferIndex(int ind) {
        return 2*ind + bufferOffset;
    }

    private int dataIndex(int ind) {
        return  2*ind + dataOffset;
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
