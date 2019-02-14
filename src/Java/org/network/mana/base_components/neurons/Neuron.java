package Java.org.network.mana.base_components.neurons;

import Java.org.network.mana.utils.BoolArray;

public interface Neuron {

	int [] getOutDegree();

	BoolArray getSpikes();
	
	void update(double dt, double time, BoolArray spkBuffer);
	
	int getSize();
	
	boolean isExcitatory();
	
	double[][] getCoordinates(boolean trans);

	int getID();

}
