package mana;

import java.util.concurrent.atomic.AtomicInteger;

public class MANA_Globals {

    private static AtomicInteger ID = new AtomicInteger(0);

    public static final double dt = 0.25;

    public static int getID() {
        return ID.getAndIncrement();
    }

}
