package mana;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import data_holders.InputData;
import nodes.MANA_Node;
import nodes.MANA_Sector;
import nodes.MANA_Unit;
import nodes.Syncable;

public class MANA_Executor {
	
	private double time;
	
	private double dt;

	private final ExecutorService pool;
	
	
	
	List<UpdateTask> updateTasks = new ArrayList<UpdateTask>();
	List<Callable<Syncable>> syncTasks = new ArrayList<Callable<Syncable>>();
	
	
	public MANA_Executor() {
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	public void addUnit(MANA_Unit unit) {
		syncTasks.add(new InputSyncTask(unit.externalInp));
		for(MANA_Sector s : unit.sectors) {
			syncTasks.add(new SectorSyncTask(s));
		}
		for(MANA_Node n : unit.nodes) {
			updateTasks.add(new UpdateTask(n));
		}
	}
	
	/**
	 * Updates all nodes given to it to update and synchonizes all inputs and
	 * reservoir neurons associated with those nodes. Currently does ONLY this,
	 * so the calling loop needs to things like schedule growth/pruning periods
	 * or when to record things...
	 * @throws InterruptedException
	 */
	public void invoke() throws InterruptedException {
		pool.invokeAll(updateTasks);
		pool.invokeAll(syncTasks);
		time += dt;
	}
	
	public class SectorSyncTask implements Callable<Syncable>{

		public final MANA_Sector sector;
		
		public SectorSyncTask(final MANA_Sector _sector) {
			this.sector = _sector;
		}

		@Override
		public MANA_Sector call() throws Exception {
			sector.synchronize(time);
			return sector;
		}
		
	}
	
	public class InputSyncTask implements Callable<Syncable>{

		public final InputData inp;
		
		public InputSyncTask(final InputData _inp) {
			this.inp = _inp;
		}
		
		@Override
		public InputData call() throws Exception {
			inp.update(dt, time, inp.spks, inp.lastSpkTime);
			return inp;
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
	
}
