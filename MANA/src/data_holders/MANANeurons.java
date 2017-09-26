package data_holders;

import functions.UtilFunctions;

public class MANANeurons implements Spiker {
	
	
	// TODO: Make separately settable, i.e. not static and not final
	// as well as put some code somewhere to read in values from file.
	public static final double init_v_m = -55;
	public static final double init_thresh = -50;
	public static final double default_v_l = -70;
	public static final double default_r_m = 1.0;
	public static final double default_i_bg = 18;
	public static final double init_tau_HP = 1E-4;
	public static final double final_tau_HP = 5E-5;
	public static final double init_tau_MHP = 0.05;
	public static final double final_tau_MHP = 1E-7;
	public static final double hp_decay = 1E-6;
	public static final double mhp_decay = 1E-6;
	public static final double default_alpha = 2;
	public static final double default_lowFR = 1.0;
	public static final double default_beta = 15;
	public static final double default_noiseVar = 0.7;
	
	public static final double default_exc_tau_m = 30;
	public static final double default_inh_tau_m = 20;
	public static final double default_exc_ref_p = 3;
	public static final double default_inh_ref_p = 2;
	public static final double default_sat_a = 300;
	public static final double default_sat_b = 0.1;
	
	public boolean mhpOn = true;
	
	public double lambda = init_tau_HP;
	public double eta = init_tau_MHP;
	public double noiseVar = default_noiseVar;
	
	/* Neuron Properties */
	public double [] v_m;
	public double [] dv_m;
	public double [] thresh;
	public double [] i_e;
	public double [] i_i;
	public double [] lastSpkTime;
	public boolean [] spks;
	public double positions[][];
	
	// Can be the same or different for all neurons
	public DataWrapper r_m;
	public DataWrapper tau_m;
	public DataWrapper v_l;
	public DataWrapper i_bg;
	public DataWrapper alpha;
	public DataWrapper beta;
	public DataWrapper lowFRBound;
	public DataWrapper ref_p;
	public DataWrapper v_reset;
	
	// Adaptation
	public DataWrapper tau_w;
	public double [] adapt;
	
	// MANA related properties
	// TODO: Move these and related functions to a separate class... generic IF neuron and MANA stuff separate
	public double [] estFR;
	public double [] ef;
	public double [] prefFR;
	public double [] threshRA;
	
	// Afferent Synapse properties
	public boolean [] excSNon;
	public boolean [] inhSNon;
	public double [] normVals;
	public double [] exc_sf;
	public double [] inh_sf;
	public double [] sat_c;
	public double sat_a = default_sat_a;
	public double sat_b = default_sat_b;
	public int[] inDegree;
	public int[] excInDegree;
	public int[] inhInDegree;
	public int[] outDegree;
	
	public double [][] xyzCoors;
	
	/* Misc. Important */
	public final int N;
	public final boolean exc;
	
	/**
	 * Creates MANA neurons of the specified polarity with default parameters.
	 * @param _N size of group
	 * @param _exc polarity (excitatory: true, inhibitory: false)
	 */
	public MANANeurons(int _N, boolean _exc) {
		
		this.N = _N;
		this.exc = _exc;
		
		v_m = new double[N];
		dv_m = new double[N];
		thresh = new double[N];
		estFR = new double[N];
		prefFR = new double[N];
		threshRA = new double[N];
		exc_sf = new double[N];
		inh_sf = new double[N];
		lastSpkTime = new double[N];
		spks = new boolean[N];
		//normPool = new double[N];
		
		r_m = new DataWrapper(N, true, default_r_m);
		v_l = new DataWrapper(N, true, default_v_l);
		i_bg = new DataWrapper(N, true, default_i_bg);
		alpha = new DataWrapper(N, true, default_alpha);
		beta = new DataWrapper(N, true, default_beta);
		lowFRBound = new DataWrapper(N, true, default_lowFR);
		
		if(exc) {
			ref_p = new DataWrapper(N, true, default_exc_ref_p);
			tau_m = new DataWrapper(N, true, default_exc_tau_m);
		} else {
			ref_p = new DataWrapper(N, true, default_inh_ref_p);
			tau_m = new DataWrapper(N, true, default_inh_tau_m);
		}
	}
	
	
	/**
	 * Updates the equations governing the neurons' memberane potentials
	 * and adaptations and determines which neurons spike on the next time-step
	 * as a result, storing those in buffers.
	 * @param dt
	 * @param time
	 * @param spkBuffer
	 * @param lastSpkTimeBuffer
	 */
	@Override
	public void update(double dt, double time, boolean[] spkBuffer, double[] lastSpkTimeBuffer) {
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] = i_e[ii];
			i_e[ii] -= dt*i_e[ii]/SynapseData.ExcTau;
			
		}
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] -= i_i[ii];
			i_i[ii] -= dt*i_i[ii]/SynapseData.InhTau;
		}
		
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] -= adapt[ii];
		}
		
		if(!(r_m.isCompressed() && r_m.get(0)==1)){
			for(int ii=0; ii<N; ++ii) {
				dv_m[ii] *= r_m.get(ii);
			}
		} 
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] += (v_l.get(ii)-v_m[ii]);
		}
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] *= dt/tau_m.get(ii);
		}
		
		for(int ii=0; ii<N; ++ii) {
			if(time > (ref_p.get(ii) + lastSpkTime[ii])) {
				v_m[ii] += dv_m[ii];
			}
		}
		for(int ii=0; ii<N; ++ii) {
			adapt[ii] -= dt*adapt[ii]/tau_w.get(ii);
		}
		for(int ii=0; ii<N; ++ii) {
			if(v_m[ii] >= thresh[ii]) {
				spkBuffer[ii] = true;
				lastSpkTimeBuffer[ii] = time;
				v_m[ii] = v_reset.get(ii);
				adapt[ii] += 1;
				ef[ii] += 1;
			} else {
				spkBuffer[ii] = false;
			}
		}
	}
	
	/**
	 * Updates the estimated firing rate of the neurons placing the new
	 * values in a buffer
	 * @param dt
	 * @param estFRBuffer
	 */
	public void updateEstFR(double dt, double[] estFRBuffer) {
		
		for(int ii=0; ii<N; ++ii) {
			double tauA = 10000 / Math.sqrt(prefFR[ii]);
			ef[ii] -= dt * ef[ii]/tauA;
			if(spks[ii]) {
				ef[ii] +=1;
			}
			estFRBuffer[ii] = estFR[ii] + (dt *(1000*ef[ii] - estFR[ii]));
		}
		
	}
	
	/**
	 * Updates the threshold based on the homeostatic plasticity rules using
	 * preferred and estimated firing rate. Updates threshold directly, not to a buffer
	 * @param dt
	 */
	public void updateThreshold(double dt) {
		for(int ii=0; ii<N; ++ii) {
			thresh[ii] += dt*Math.log((estFR[ii]+0.0001)/(prefFR[ii]+0.0001));
			threshRA[ii] += thresh[ii]*lambda + threshRA[ii]*(1-lambda);
		}
	}
	
	/**
	 * 
	 * @param index
	 * @return the base synaptic normalization value the neuron at the specified
	 * index should have based on its preferred firing rate.
	 */
	public double newNormVal(int index) {
		return sat_a/(1+UtilFunctions.expLT0Approx(-sat_b*prefFR[index]))
				+ sat_c[index];
	}
	
	@Override
	public boolean[] getSpikes() {
		return spks;
	}
	
	@Override
	public int[] getOutDegree() {
		return outDegree;
	}
	
	@Override
	public int getSize() {
		return N;
	}
	
	@Override
	public boolean isExcitatory(){
		return exc;
	}


	@Override
	public double[][] getCoordinates() {
		return xyzCoors;
	}
	
}
