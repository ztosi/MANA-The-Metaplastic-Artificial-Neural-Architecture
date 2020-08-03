package Java.org.network.mana.execution.tasks;

import Java.org.network.mana.nodes.MANA_Sector;

import java.util.concurrent.Callable;

public class SectorSyncTask implements Callable<Syncable> {

    public final MANA_Sector sector;

    public SectorSyncTask(final MANA_Sector _sector) {
        this.sector = _sector;
    }

    @Override
    public MANA_Sector call() throws Exception {
//			sector.update(time, dt);
        sector.synchronize();
//			System.out.println(time + " " +  ct.incrementAndGet());
        return sector;
    }

}
