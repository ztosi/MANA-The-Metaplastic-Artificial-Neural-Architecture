package data_holders;

public interface Spiker {

	boolean[] getSpikes();
	
	int[] getOutDegree();
	
	void update(double dt, double time, boolean[] spkBuffer, double[] lastSpkTimeBuffer);
	
	int getSize();
	
	boolean isExcitatory();
	
	double[][] getCoordinates();
	
}
