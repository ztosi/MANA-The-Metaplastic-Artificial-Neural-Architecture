package Java.org.network.mana.base_components;

import Java.org.network.mana.utils.BoolArray;

public interface SpikingNeuron {

	BoolArray getSpikes();
	
	int[] getOutDegree();
	
	void update(double dt, double time, BoolArray spkBuffer);
	
	int getSize();
	
	boolean isExcitatory();
	
	double[][] getCoordinates(boolean trans);

	void setCoor(int ind, double x, double y, double z);

	void setCoor(int ind, double[] xyz);

	int getID();

}
