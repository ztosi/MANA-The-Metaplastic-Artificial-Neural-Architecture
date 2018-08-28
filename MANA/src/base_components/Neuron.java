package base_components;

import utils.BoolArray;

public interface Neuron {

	BoolArray getSpikes();
	
	int[] getOutDegree();
	
	void update(double dt, double time, BoolArray spkBuffer);
	
	int getSize();
	
	boolean isExcitatory();
	
	double[][] getCoordinates();

	int getID();

}
