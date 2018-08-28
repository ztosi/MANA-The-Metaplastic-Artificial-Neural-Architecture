package mana;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import base_components.InputNeurons;
import nodes.*;
import utils.Syncable;

public class MANA_Executor {
	
	private double time = 0;
	private double dt = MANA_Globals.dt;
    private double pruneInterval;
    public boolean pruneOn = true;

	private List<UpdateTask> updateTasks = new ArrayList<>();
	private List<Callable<Syncable>> syncTasks = new ArrayList<>();
	private List<PruneTask> pruneTasks = new ArrayList<>();

    AtomicInteger ct = new AtomicInteger(0);

    private final ExecutorService pool;

	public MANA_Executor(final double pruneInterval) {
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.pruneInterval = pruneInterval;
	}
	
	public MANA_Executor(final double pruneInterval, final double _dt) {
		this(pruneInterval);
		this.dt = _dt;
	}
	
	public void addUnit(MANA_Unit unit, int maxID, int maxOD, double lambda, double maxDist) {
		syncTasks.add(new InputSyncTask(unit.externalInp));
		for(MANA_Sector s : unit.sectors.values()) {
			syncTasks.add(new SectorSyncTask(s));
		}
		for(MANA_Node n : unit.nodes) {
			updateTasks.add(new UpdateTask(n));
			pruneTasks.add(new PruneTask(n, maxID, maxOD, lambda, maxDist));
		}
	}


	/**
	 * Updates all nodes given to it to update and synchronizes all inputs and
	 * reservoir neurons associated with those nodes. Currently does ONLY this,
	 * so the calling loop needs to things like schedule growth/pruning periods
	 * or when to record things...
	 * @throws InterruptedException
	 */
	public void invoke() throws InterruptedException {
	    if(time > 0 && (int)(time/dt) %  (int)(pruneInterval/dt) == 0 && pruneOn) {
	        pool.invokeAll(pruneTasks);
        }
		pool.invokeAll(updateTasks);
		pool.invokeAll(syncTasks);
		time += dt;
		ct.set(0);
	}

	public class SectorSyncTask implements Callable<Syncable>{

		public final MANA_Sector sector;
		
		public SectorSyncTask(final MANA_Sector _sector) {
			this.sector = _sector;
		}

		@Override
		public MANA_Sector call() throws Exception {
			sector.synchronize();
			System.out.println(time + " " +  ct.incrementAndGet());
			return sector;
		}
		
	}
	
	public class InputSyncTask implements Callable<Syncable>{

		public final InputNeurons inp;
		
		public InputSyncTask(final InputNeurons _inp) {
			this.inp = _inp;
		}
		
		@Override
		public InputNeurons call() throws Exception {
			inp.update(dt, time, inp.spks);
			return inp;
		}
		
	}

	public class PruneTask implements Callable<MANA_Node> {
	    public final MANA_Node node;
	    public final int maxInD, maxOutD;
	    public final double lambda, maxDist;

	    public PruneTask(final MANA_Node node, int maxInD, int maxOutD, double lambda, double maxDist) {
	        this.node = node;
	        this.maxInD = maxInD;
	        this.maxOutD = maxOutD;
	        this.lambda = lambda;
	        this.maxDist = maxDist;
	    }

	    @Override
        public MANA_Node call() throws Exception {
            node.structuralPlasticity(maxInD, maxOutD, lambda, maxDist, time);
            return node;
        }
	}
	
	public class UpdateTask implements Callable<MANA_Node> {

		public final MANA_Node node;
		
		public UpdateTask(final MANA_Node _node) {
			this.node = _node;
		}
		
		@Override
		public MANA_Node call() throws Exception {
			node.update(time, dt);
			return node;
		}
	}
	
	public double getTime() {
		return time;
	}
	
	public double getDt() {
		return dt;
	}
	
}
