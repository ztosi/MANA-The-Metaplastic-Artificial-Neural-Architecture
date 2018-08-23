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
	
	public static final double DEF_DT = 0.5; //ms
	
	private double time = 0;
	private double dt = DEF_DT;

	private final ExecutorService pool;
	
	List<UpdateTask> updateTasks = new ArrayList<UpdateTask>();
	List<Callable<Syncable>> syncTasks = new ArrayList<Callable<Syncable>>();
	
	public MANA_Executor() {
		pool = Executors.newFixedThreadPool(8); //Runtime.getRuntime().availableProcessors());
	}
	
	public MANA_Executor(final double _dt) {
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.dt = _dt;
	}
	
	public void addUnit(MANA_Unit unit) {
		syncTasks.add(new InputSyncTask(unit.externalInp));
		for(MANA_Sector2 s : unit.sectors) {
			syncTasks.add(new SectorSyncTask(s));
		}
		for(MANA_Node2 n : unit.nodes) {
			updateTasks.add(new UpdateTask(n));
		}
	}

	AtomicInteger ct = new AtomicInteger(0);
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
		ct.set(0);
	}
	
	public class SectorSyncTask implements Callable<Syncable>{

		public final MANA_Sector2 sector;
		
		public SectorSyncTask(final MANA_Sector2 _sector) {
			this.sector = _sector;
		}

		@Override
		public MANA_Sector2 call() throws Exception {
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
	
	public class UpdateTask implements Callable<MANA_Node2> {

		public final MANA_Node2 node;
		
		public UpdateTask(final MANA_Node2 _node) {
			this.node = _node;
		}
		
		@Override
		public MANA_Node2 call() throws Exception {
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
