package Java.org.network.mana.base_components.synapses;

import Java.org.network.mana.enums.ConnectRule;
import Java.org.network.mana.globals.Default_Parameters;
import Java.org.network.mana.utils.Utils;

public final class ConnectSpecs {
    public final ConnectRule rule;
    public final double [] parms;
    public final double maxDist;
    public final double maxDly;
    public final Utils.ProbDistType randDist;
    public final double[] randParms;

    public ConnectSpecs(ConnectRule rule, double[] parms, double maxDist, double maxDly) {
        this.rule = rule;
        this.parms = parms;
        this.maxDist = maxDist;
        this.maxDly = maxDly;
        randDist = null;
        randParms = null;
    }

    public ConnectSpecs(ConnectRule rule, double[] parms, Utils.ProbDistType randDist, double[] randParms,
                        double maxDist, double maxDly) {
        this.rule = rule;
        this.parms = parms;
        this.maxDist = maxDist;
        this.maxDly = maxDly;
        this.randDist = randDist;
        this.randParms = randParms;
    }

    public double getNewWt() {
        if(randDist == null) {
            return Default_Parameters.DEF_INIT_WT;
        } else {
            return Math.abs(randDist.getRandom(randParms[0], randParms[1]));
        }
    }


}
