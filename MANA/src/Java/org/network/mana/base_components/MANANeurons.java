package Java.org.network.mana.base_components;

import java.util.Arrays;

import Java.org.network.mana.base_components.enums.SynType;
import Java.org.network.mana.functions.MHPFunctions;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.utils.*;

public class MANANeurons implements Neuron {

	public final int id;
	
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
	public static final double default_exc_SF_tau = 5;
    public static final double default_inh_SF_tau = 5;


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
	public BufferedDoubleArray lastSpkTime;
	public BoolArray spks;
	
	// Can be the same or different for all neurons
	public DataWrapper r_m;
	public DataWrapper tau_m;
	public DataWrapper v_l;
	public DataWrapper i_bg;
	public DataWrapper alpha;
	public DataWrapper beta;
	public DataWrapper lowFRBound;
	public double ref_p;
	public DataWrapper v_reset;
	
	// Adaptation
	public DataWrapper tau_w;
	public double [] adapt;
	
	// MANA related properties
	// TODO: Move these and related Java.org.network.Java.org.network.mana.mana.functions to a separate class... generic IF neuron and MANA stuff separate
	public BufferedFloatArray estFR;
	public double [] ef;
	public double [] dummy; // here to test if my math is right....
	public double [] prefFR;
	public double [] threshRA;
	
	// Afferent Synapse properties
	public BoolArray excSNon;
	public BoolArray inhSNon;
	//public double [] normVals;
	public double [] normValsExc;
	public double [] normValsInh;
	public double [] exc_sf;
	public double [] inh_sf;
	public double [] sat_c;
	public double sat_a = default_sat_a;
	public double sat_b = default_sat_b;
	public int[] inDegree;
	public int[] excInDegree;
	public int[] inhInDegree;
	public int[] outDegree;
	public long[] fVals;
    public boolean allExcSNon = false;
    public boolean allInhSNon = false;
	public double [][] xyzCoors;
	/* Misc. Important */
	public final int N;
	public final boolean exc;

	public static MANANeurons buildFromLimits(int _N, boolean _exc, double[] xlims, double[] ylims, double[] zlims) {
	        double[] xCoors = Utils.getRandomArray(Utils.ProbDistType.UNIFORM, xlims[0], xlims[1], _N);
	        double[] yCoors = Utils.getRandomArray(Utils.ProbDistType.UNIFORM, ylims[0], ylims[1], _N);
	        double[] zCoors = Utils.getRandomArray(Utils.ProbDistType.UNIFORM, zlims[0], zlims[1], _N);
	        return new MANANeurons(_N, _exc, xCoors, yCoors, zCoors);
	}

	/**
	 * Creates MANA neurons of the specified polarity with default parameters.
	 * @param _N size of group
	 * @param _exc polarity (excitatory: true, inhibitory: false)
	 */
	public MANANeurons(int _N, boolean _exc, double[] xCoor, double[] yCoor, double[] zCoor) {
		id = MANA_Globals.getID();
		this.N = _N;
		this.exc = _exc;
		dummy = new double[N];
		v_m = new double[N];
		dv_m = new double[N];
		thresh = new double[N];
		estFR = new BufferedFloatArray(N);
		prefFR = new double[N];
		for(int ii=0; ii < N; ++ii) {
		    estFR.setData(ii, 1.0f);
        }
		Arrays.fill(prefFR, 1.0);
		threshRA = new double[N];
		exc_sf = new double[N];
		inh_sf = new double[N];
		lastSpkTime = new BufferedDoubleArray(N);
		spks = new BoolArray(N);
		normValsExc = new double[N];
		normValsInh = new double[N];
		sat_c = new double[N];
		inDegree = new int[N];
		excInDegree = new int[N];
		inhInDegree = new int[N];
		outDegree = new int[N];
		inhSNon = new BoolArray(N);
		excSNon = new BoolArray(N);
		i_e = new double[N];
		i_i = new double[N];
		ef = new double[N];
		adapt = new double[N];
		fVals = new long[N];

		r_m = new DataWrapper(N, true, default_r_m);
		v_l = new DataWrapper(N, true, default_v_l);
		i_bg = new DataWrapper(N, true, default_i_bg);
		alpha = new DataWrapper(N, true, default_alpha);
		beta = new DataWrapper(N, true, default_beta);
		lowFRBound = new DataWrapper(N, true, default_lowFR);
		v_reset = new DataWrapper(N, true, init_v_m);
		tau_w = new DataWrapper(N, true, 144);
		
		if(exc) {
			ref_p = default_exc_ref_p;
			tau_m = new DataWrapper(N, true, default_exc_tau_m);
		} else {
			ref_p = default_inh_ref_p;
			tau_m = new DataWrapper(N, true, default_inh_tau_m);
		}
		xyzCoors=new double[_N][3];
		for(int ii=0; ii< _N; ++ii) {
			xyzCoors[ii][0] = xCoor[ii];
			xyzCoors[ii][1] = yCoor[ii];
			xyzCoors[ii][2] = zCoor[ii];
		}
	}


	/**
	 * Updates everything placing the results in buffers. DOES NOT modify local variables.
	 * -The voltage & puts who spiked into a buffer as well as the new most recent spike times
	 * -The estimated firing rate
	 * -The threshold
	 * -Calculates the excitatory and inhibitory scale factors
     * -Updates preferred firing rates
     * -Calculates new f+ and f- values for new preferred firing rates
	 * @param spkBuffer
	 * @param time
	 * @param dt
	 */
	public void performFullUpdate(BoolArray spkBuffer, double[] pfrDts, final double time, final double dt) {
	    update(dt, time, spkBuffer);
	    updateEstFR(dt);
	    updateThreshold(dt);
        descaleNormVals();
	    calcScaleFacs();
		if (mhpOn && !(allExcSNon && allInhSNon)) {
		    for(int ii=0; ii<N; ++ii) {
		        prefFR[ii] += dt*eta/inDegree[ii] * pfrDts[ii];
            }
			MHPFunctions.calcfTerm(prefFR, fVals, default_alpha, default_beta, default_lowFR);
		}
		calcNewNorms();
		scaleNormVals();
        lambda += dt * (lambda-final_tau_HP) * hp_decay;
        eta += dt  * (eta - final_tau_MHP) * mhp_decay;
    }

    /**
     * For triggered normVals, de scales them so that the new scaling can be applied.
     */
    private void descaleNormVals() {
	    if(!allExcSNon) {
            for (int ii = 0; ii < N; ++ii) {
                if (excSNon.get(ii)) {
                    normValsExc[ii] /= exc_sf[ii];
                }
            }
        } else  {
            for (int ii = 0; ii < N; ++ii) {
                normValsExc[ii] /= exc_sf[ii];
            }
        }
        if(!allInhSNon) {
            for (int ii = 0; ii < N; ++ii) {
                if (inhSNon.get(ii)) {
                    normValsInh[ii] /= inh_sf[ii];
                }
            }
        } else {
            for (int ii = 0; ii < N; ++ii) {
                normValsInh[ii] /= inh_sf[ii];
            }
        }

    }

    private void scaleNormVals() {
        for(int ii=0; ii<N; ++ii){
            normValsInh[ii] *= inh_sf[ii];
        }
        for(int ii=0; ii<N; ++ii){
            normValsExc[ii] *= exc_sf[ii];
        }

    }

    /**
     * Checks to see if synaptic sums have exceeded their scaled norm vals. If they all have
     * sets allExcSNon and/or allInhSNon to true.
     * @param excSums
     * @param inhSums
     */
    public void updateTriggers(double[] excSums, double[] inhSums) {
        if(!allExcSNon) {
            boolean allOn = true;
            for(int ii=0; ii<N; ++ii) {
                excSNon.set(ii, excSums[ii] >= normValsExc[ii] && !excSNon.get(ii));
                allOn &= excSNon.get(ii);
            }
            allExcSNon = allOn;
        }
        if(!allInhSNon) {
            boolean allOn = true;
            for(int ii=0; ii<N; ++ii) {
                inhSNon.set(ii, inhSums[ii] >= normValsInh[ii] && !inhSNon.get(ii));
                allOn &= inhSNon.get(ii);
            }
            allInhSNon = allOn;
        }
    }

    /**
     * Calculates new normalization values based on pref. firing rate for all non-triggered neurons for
     * both types...
     */
    private void calcNewNorms() {
	    if(!allInhSNon && !allExcSNon) {
	        if(allExcSNon && !allInhSNon) {
	            for(int ii=0; ii<N; ++ii) {
	                if(!inhSNon.get(ii)) {
	                    normValsInh[ii] = newNormVal(ii);
                    }
                }
            } else if(allInhSNon && !allExcSNon) {
                for(int ii=0; ii<N; ++ii) {
                    if(!excSNon.get(ii)) {
                        normValsExc[ii] = newNormVal(ii);
                    }
                }
            } else {
                for(int ii=0; ii<N; ++ii) {
                    if(!excSNon.get(ii) || !inhSNon.get(ii)) {
                        double nnV = newNormVal(ii);
                        if(!excSNon.get(ii)) {
                            normValsExc[ii] = nnV;
                        }
                        if(!inhSNon.get(ii)) {
                            normValsInh[ii] = nnV;
                        }
                    }
                }
            }
        } else {
	        return;
        }
    }

	/**
	 * Updates the equations governing the neurons' memberane potentials
	 * and adaptations and determines which neurons calcSpikeResponses on the next time-step
	 * as a result, storing those in buffers.
	 * @param dt
	 * @param time
	 * @param spkBuffer
	 */
	@Override
	public void update(double dt, double time, BoolArray spkBuffer) {
		for(int ii=0; ii<N; ++ii) {
			int sgn = Utils.checkSign((lastSpkTime.getData(ii)+ref_p)-time);
			dv_m[ii] += i_e[ii] * sgn;
			dv_m[ii] -= i_i[ii] * sgn;
		}
		for(int ii=0; ii<N; ++ii) {
			dv_m[ii] -= adapt[ii];
		}
		for(int ii=0; ii<N; ++ii) {
			i_e[ii] -= dt*i_e[ii]/SynType.ExcTau;

		}
		for(int ii=0; ii<N; ++ii) {
			i_i[ii] -= dt*i_i[ii]/SynType.InhTau;
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

		for (int ii = 0; ii < N; ++ii) {
			v_m[ii] += dv_m[ii];
			if (Double.isNaN(v_m[ii])) {
				System.out.println(" NaN v");
				break;
			}
		}
		for(int ii=0; ii<N; ++ii) {
			adapt[ii] -= dt*adapt[ii]/tau_w.get(ii);
		}

		for(int ii=0; ii<N; ++ii) {
			spkBuffer.set(ii, v_m[ii] >= thresh[ii] && (time > lastSpkTime.getData(ii)+ref_p));
		}

		for(int ii=0; ii<N; ++ii) {
			if(spkBuffer.get(ii)) {
				lastSpkTime.setBuffer(ii, time);
				v_m[ii] = v_reset.get(ii);
				adapt[ii] += 1;
				ef[ii] += 1;
			}
		}
	}
	
	/**
	 * Updates the estimated firing rate of the neurons placing the new
	 * values in a buffer
	 * @param dt
	 */
	public void updateEstFR(double dt) {
		for(int ii=0; ii<N; ++ii) {
			double tauA = 10000 / Math.sqrt(prefFR[ii]);
			ef[ii] -= dt * ef[ii]/tauA;
			if (Double.isNaN(ef[ii])) {
				System.out.println("nan");
			}
			dummy[ii] += dt * (ef[ii]/tauA - dummy[ii]);
			estFR.setBuffer(ii,(float) (dummy[ii] * 1000));//estFR[ii] + (dt *(1000*ef[ii] - estFR[ii]));
		}
	}
	
	/**
	 * Updates the threshold based on the homeostatic plasticity rules using
	 * preferred and estimated firing rate. Updates threshold directly, not to a buffer
	 * @param dt
	 */
	public void updateThreshold(double dt) {
		for(int ii=0; ii<N; ++ii) {
			thresh[ii] += dt * lambda * Math.log((estFR.getData(ii)+0.0001)/(prefFR[ii]+0.0001));
			if (Double.isNaN(thresh[ii])) {
				System.out.println("NaN th");
				break;
			}
			threshRA[ii] += thresh[ii] * lambda + threshRA[ii]*(1-lambda);
		}
	}

	public void calcScaleFacs() {
	    for(int ii=0; ii<N; ++ii) {
	        exc_sf[ii] = Math.exp((threshRA[ii] - thresh[ii])/default_exc_SF_tau);
        }
        for(int ii=0; ii<N; ++ii) {
            inh_sf[ii] = Math.exp((thresh[ii] - threshRA[ii])/default_inh_SF_tau);
        }
    }

    /**
     * For all total excitatory sums that haven't been set by normalization, checks if they
     * have exceeded or are equal to what the normalization value should be. Returns whether or
     * not all neurons have been set.
     * @param sums
     * @return
     */
    public boolean checkExcSums(double[] sums) {
		boolean allUp = true;
		for(int ii=0; ii<N; ++ii) {
		    if(!excSNon.get(ii)) {
		        boolean decision = sums[ii] > normValsExc[ii];
		        excSNon.set(ii, decision);
		        allUp &= decision;
            }
		}
		return allUp;
	}

    /**
     * For all total inhibitory sums that haven't been set by normalization, checks if they
     * have exceeded or are equal to what the normalization value should be. Returns whether or
     * not all neurons have been set.
     * @param sums
     * @return
     */
    public boolean checkInhSums(double[] sums) {
        boolean allUp = true;
        for(int ii=0; ii<N; ++ii) {
            if(!inhSNon.get(ii)) {
                boolean decision = sums[ii] > normValsInh[ii];
                inhSNon.set(ii, decision);
                allUp &= decision;
            }
        }
        return allUp;
    }

	/**
	 * 
	 * @param index
	 * @return the base synaptic normalization value the neuron at the specified
	 * index should have based on its preferred firing rate.
	 */
	public double newNormVal(int index) {
		return sat_a/(1+Utils.expLT0Approx(-sat_b*prefFR[index]))
				+ sat_c[index];
	}

	public int[] getProperInDegrees(Neuron src) {
		if (src.isExcitatory()) {
			return excInDegree;
		} else {
			return inhInDegree;
		}
	}

	public boolean getAllNrmOn(boolean exc) {
	    return exc ? allExcSNon : allInhSNon;
    }
	
	@Override
	public BoolArray getSpikes() {
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

	@Override
	public int getID() {
		return id;
	}
	
}
