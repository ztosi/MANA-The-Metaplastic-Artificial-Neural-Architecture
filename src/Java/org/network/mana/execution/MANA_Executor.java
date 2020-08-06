package Java.org.network.mana.execution;

import Java.org.network.mana.execution.tasks.*;
import Java.org.network.mana.mana.MANA_Globals;
import Java.org.network.mana.nodes.MANA_Node;
import Java.org.network.mana.nodes.MANA_Sector;
import Java.org.network.mana.nodes.MANA_Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MANA_Executor {
	
	private double time = 0;
	private double dt = MANA_Globals.dt;
    private double pruneInterval;
    public boolean pruneOn = true;

	private List<UpdateTask<Updatable>> updateTasks = new ArrayList<>();
	private List<Callable<Syncable>> syncTasks = new ArrayList<>();
	private List<PruneTask> pruneTasks = new ArrayList<>();
	private List<MANA_Unit> units = new ArrayList<>();

    //AtomicInteger ct = new AtomicInteger(0);

    private final ExecutorService pool;

    private AtomicBoolean invocationComplete = new AtomicBoolean(false);

	public MANA_Executor(final double pruneInterval) {
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.pruneInterval = pruneInterval;
	}
	
	public MANA_Executor(final double pruneInterval, final double _dt) {
		this(pruneInterval);
		this.dt = _dt;
	}
	
	public void addUnit(MANA_Unit unit, int maxID, int maxOD, double lambda, double maxDist) {
		syncTasks.add(new InputSyncTask(this, unit.externalInp));
		for(MANA_Sector s : unit.sectors.values()) {
			syncTasks.add(new SectorSyncTask(s));
		}
		for(MANA_Node n : unit.nodes) {
			updateTasks.add(new MANANodeUpdateTask(this, n));
			pruneTasks.add(new PruneTask(this, n, maxID, maxOD, lambda, maxDist));
		}
		units.add(unit);
	}


	/**
	 * Updates all Java.org.network.mana.nodes given to it to update and synchronizes all inputs and
	 * reservoir neurons associated with those Java.org.network.mana.nodes. Currently does ONLY this,
	 * so the calling loop needs to things like schedule growth/pruning periods
	 * or when to record things...
	 * @throws InterruptedException
	 */
	public void invoke() throws InterruptedException {
		invocationComplete.set(false);
	    if((time > 0 && (int)(time/dt) %  (int)(pruneInterval/dt) == 0 && pruneOn)) {
	    	int nnz = 0;
			for(MANA_Unit unit : units) {
				nnz += unit.getTotalNNZ();
			}
	    	System.out.println("========== " + nnz + " ==========");
	        pool.invokeAll(pruneTasks);
	        int nnz2 = 0;
	        for(MANA_Unit unit : units) {
	        	unit.revalidateDegrees();
				nnz2 += unit.getTotalNNZ();
			}
			System.out.println("======== " + nnz2 + " =========");
			System.out.println("NET: ===== " + (nnz2-nnz) + " =========");
			System.gc();
        }
		try {
			pool.invokeAll(updateTasks);

			pool.invokeAll(syncTasks);
		} catch (Exception e) {
	    	e.printStackTrace();
		}
		for(UpdateTask<Updatable> t : updateTasks) {
			t.getUpdatable().setUpdated(false);
		}//t.node.updated.set(false);
		time += dt;
		invocationComplete.set(true);
//		ct.set(0);
	}

	public double getTime() {
		return time;
	}
	
	public double getDt() {
		return dt;
	}
	
}
