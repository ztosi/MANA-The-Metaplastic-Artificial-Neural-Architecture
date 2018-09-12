package Java.org.network.mana.base_components;

import Java.org.network.mana.utils.BoolArray;

public interface Neuron {

	BoolArray getSpikes();
	
	int[] getOutDegree();
	
	void update(double dt, double time, BoolArray spkBuffer);
	
	int getSize();
	
	boolean isExcitatory();
	
	double[][] getCoordinates();

	int getID();

}
