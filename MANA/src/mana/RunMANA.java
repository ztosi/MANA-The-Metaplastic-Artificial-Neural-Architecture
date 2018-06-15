package mana;

import java.io.File;

import base_components.SynapseData;
import functions.SPFunctions;
import nodes.MANA_Unit;

public class RunMANA {

	public static final String DEF_ODIR = "."+File.separator+"Outputs" + File.separator;
	public static final String DEF_PREFIX = "MANA";
	public static final double DEF_PRINT_INTERVAL = 6E5;

	public static void main(String[] args) { // TODO: enable more complicated command line args...
		System.out.println(System.getProperty("user.dir"));
		int numNeu = 2000;
		double time_f0 = 7.2E6; // two hours...
		double plastShutOff0 = time_f0/2;
		String filename = null;
		String odir = DEF_ODIR;
		String prefix = DEF_PREFIX;
		double printInterval = 1000;
		for(int ii=0; ii<args.length; ++ii) {
			switch(args[ii]) {
			case "-n" :
				numNeu = Integer.parseInt(args[++ii]); break;
			case "-f" :
				filename = args[++ii]; break;
			case "-plastOff" :
				plastShutOff0 = Integer.parseInt(args[++ii]); break;
			case "-time" :
				time_f0 = Double.parseDouble(args[++ii]); break;
			case "-label" :
				prefix = args[++ii]; break;
			case "-o" :
				odir = args[++ii]; break;
			case "-printInterval" :
				printInterval = Double.parseDouble(args[++ii]); break;
			default :
				throw new IllegalArgumentException("Unknown input.");
			}
		}
		final double time_f = time_f0;
		final double p_shutOff_f = plastShutOff0;
		if(filename == null) {
			throw new IllegalArgumentException("No filename for input "
					+ "spikes was specified--exiting...");
		}
		MANA_Unit unit = MANA_Unit.MANABuilder(filename, numNeu);
		MANA_Executor exec = new MANA_Executor(); // initialize threads
		for(int ii=0, n=unit.nodes.size(); ii<n; ++ii) {
			if (ii % 11 == 0) {
				System.out.println();
			}
			System.out.print(unit.nodes.get(ii).type.isExcitatory() + " ");
		}
		exec.addUnit(unit); // tell them what unit they'll be working on

		double maxDist = unit.getMaxDist();
		double lambda = maxDist/SPFunctions.RT_LOG_PA_MIN;
		double time = 0.0;
		int maxAdd = (int)(unit.getSize() * unit.getSize() 
				* SPFunctions.P_ADD_MIN);
		boolean tripped = false;

		File mainOut = new File(odir);
		if (!mainOut.exists()) {
			if (!mainOut.mkdir()) {
				System.err.println("FATAL ERROR: FAILED TO CREATE OR FIND MAIN "
						+ "OUTPUT DIRECTORY.");
				System.exit(1);
			}
		}
		long iters = 0;
		try {
			while(time < time_f) {
				
				if(time >= p_shutOff_f && !tripped) {
					System.out.println("Turning off plasticity");
					tripped = true;
					unit.mhpOn = false;
					unit.synPlasticOn = false;
				}
				// If the prune interval is reached, prune/grow...
				if(time != 0 && iters% (int)((1/exec.getDt()) * SPFunctions.DEF_PG_INTERVAL) == 0) {
					SPFunctions.pruneGrow(unit, SPFunctions.DEF_EXC_THRESH,
							SPFunctions.DEF_INH_THRESH, SynapseData.MIN_WEIGHT,
							lambda, SPFunctions.DEF_CON_CONST, maxAdd);
					unit.printData(mainOut.toString(), prefix, time);
				}

				if(time !=0 && time % printInterval == 0) {
					unit.printData(mainOut.toString(), prefix, time);
				}
				System.out.println(iters);
				if(iters%1000 == 0) {
					System.out.println("------------- " + (int)iters/1000 + "------------- " );
					unit.printData(mainOut.toString(), prefix, time);
				}
				
				exec.invoke();

				time = exec.getTime(); // get time from the executor
				iters++;
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		} finally {
			unit.printData(mainOut.toString(), prefix, time);
		}

	}

}


