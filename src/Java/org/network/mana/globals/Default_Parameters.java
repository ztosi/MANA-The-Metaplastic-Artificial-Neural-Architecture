package Java.org.network.mana.globals;

import java.util.concurrent.atomic.AtomicInteger;

public class Default_Parameters {


    /** Keeps track of component IDs so that none of them have the same one during the same program run.*/
    private static AtomicInteger ID = new AtomicInteger(1);
    public static int getID() {
        return ID.getAndIncrement();
    }

    /** Simulation integration time-step: keep high during prototyping, low during data collection. */
    public static final double dt = 0.5;

    /*
        ==============NEURON PROPERTIES==================
    */

    public static final double init_v_m = -55;
    public static final double init_thresh = -50;
    public static final double default_v_l = -70;
    public static final double default_r_m = 1.0;
    public static final double default_i_bg = 18;
    public static final double default_exc_tau_m = 30;
    public static final double default_inh_tau_m = 20;
    public static final double default_exc_ref_p = 3;
    public static final double default_inh_ref_p = 2;
    public static final double default_inh_adatpJ = 10;
    public static final double default_exc_adaptJ = 15;


    // -------------------------------------META HOMEOSTATIC PLASTICITY
    public static final double MIN_PFR = 0.1;
    public static final double MAX_PFR = 100;
    public static final double init_tau_MHP = 0.05;
    public static final double final_tau_MHP = 1E-7;
    public static final double mhp_decay = 5E-6;
    public static final double default_alpha = 2;
    public static final double default_lowFR = 1;
    public static final double default_beta = 15;
    public static final double default_noiseVar = 0.2;

    // -------------------------------------HOMEOSTATIC PLASTICITY
    public static final double hp_decay = 5E-6;
    public static final double init_tau_HP = 5E-5;
    public static final double final_tau_HP = 1E-5;

    // -------------------------------------NORMALIZATION PARAMETERS
    public static final double default_sat_a = 300;
    public static final double default_sat_b = 0.1;
    public static final double default_sat_c = -150;

    /*
        ==============SYNAPSE PROPERTIES==================
     */

    public static final double MIN_WEIGHT = 0.01;
    /** The starting weight of newly spawned synapses. */
    public static final double DEF_INIT_WT = 0.01;
    public static final double DEF_NEW_WEIGHT = 0.01;
    public static final double MAX_DELAY= 20; //ms
    public static final double MAX_WT = 20;
    public static final double ExcTau = 3.0;
    public static final double InhTau = 6.0;
    public static final double STDP_TIME_CONST = 1E-6;
    public static final double DEF_INIT_WDERIV = 0;


    // -------------------------------------SHORT TERM PLASTICITY
    // UDF - means for each in that order
    public static final double[] EEUDF = new double[]{0.5, 1100, 50};
    public static final double[] EIUDF = new double[]{0.05, 125, 1200};
    public static final double[] IEUDF = new double[]{0.25, 700, 20};
    public static final double[] IIUDF = new double[]{0.32, 144, 60};

    // -------------------------------------CONNECTION CONSTANTS
    public static final double DEF_EE_C = 0.3;
    public static final double DEF_EI_C = 0.2;
    public static final double DEF_IE_C = 0.4;
    public static final double DEF_II_C = 0.1;

    // -------------------------------------STDP PARAMETERS
    public static final double eeTauPlus = 25;
    public static final double eeTauMinus = 100;
    public static final double eiTauPlus = 25;
    public static final double eiTauMinus = 25;
    public static final double ieSigma = 22;
    public static final double iiSigma = 12;

    public static final double eeWPlus = 10;
    public static final double eeWMinus = 2;
    public static final double eiWPlus = 10;
    public static final double eiWMinus = 8;
    public static final double ieWPlus = 0.5;
    public static final double ieWMinus = 6;
    public static final double iiWPlus = 3;
    public static final double iiWMinus = 6;

    // -------------------------------------STRUCTURAL PLASTICITY PARAMETERS
    public static final double DEF_CON_CONST = 0;
    public static final double NEW_SYN_CONST = 0.02;
    public static double DEF_Thresh = 0.05;

}
