package base_components;

public class ConnectionMap {


    private int [] srcIndsByTarg;
    private int [] tarPtrs;

    private int [] tarIndsBySrc;
    private int [] srcPtrs;



    private final int height;

    private final int width;

    public ConnectionMap(int _height, int _width) {
        this.height = _height;
        this.width = _width;

    }

    public ConnectionMap(int _size) {
        this.height = _size;
        this.width = _size;
    }

    public void init(int [][] _map, boolean orderFlag) {

    }


    public void init(int [][] _map) { // assumes _map is square

    }

    public void init(double density) {

    }

    public void initFull() {

    }



    private static class SparseArray {
        public int [] indices;

    }



}
