package utils;

import base_components.enums.ConnectRule;

public final class ConnectSpecs {
    public final ConnectRule rule;
    public final double [] parms;
    public final double maxDist;
    public final double maxDly;

    public ConnectSpecs(ConnectRule rule, double[] parms, double maxDist, double maxDly) {
        this.rule = rule;
        this.parms = parms;
        this.maxDist = maxDist;
        this.maxDly = maxDly;
    }
}
