package Java.org.network.mana.enums;

import Java.org.network.mana.globals.Default_Parameters;

public enum ConnectRule {

    Distance, Random, AllToAll, Distance2;

    public static double getConProbBase(boolean srcExc, boolean tarExc) {
        if (srcExc) {
            if (tarExc)
                return Default_Parameters.DEF_EE_C;
            else
                return Default_Parameters.DEF_EI_C;
        } else {
            if (tarExc)
                return Default_Parameters.DEF_IE_C;
            else
                return Default_Parameters.DEF_II_C;
        }
    }
}
