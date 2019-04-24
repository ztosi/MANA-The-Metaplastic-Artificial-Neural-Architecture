package Java.org.network.mana.utils;

public class Bounder {


    public static void applyBoundsI(MathFunction boundFunction, double [] vals, double ... parms) {
        boundFunction.applyFunctionInPlace(vals, parms);
    }

}
