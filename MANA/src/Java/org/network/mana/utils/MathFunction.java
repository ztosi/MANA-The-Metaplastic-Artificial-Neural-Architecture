package Java.org.network.mana.utils;

public interface MathFunction {

    void applyFunctionInPlace(double[] vals, double ... args);

    double applyFunction(double val, double ... args);
}
