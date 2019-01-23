package Java.org.network.mana.mana;

import java.util.concurrent.atomic.AtomicInteger;

public class MANA_Globals {

    private static AtomicInteger ID = new AtomicInteger(1);

    public static final double dt = 0.2;

    public static final double DEF_INIT_WT = 0.001;

    public static final double MIN_PFR = 0.1;

    public static int getID() {
        return ID.getAndIncrement();
    }

}
