package Java.org.network.mana.exec.mana;

import Java.org.network.mana.base_components.neurons.InputNeurons;
import Java.org.network.mana.exec.Syncable;
import Java.org.network.mana.globals.Default_Parameters;
import Java.org.network.mana.mana_components.MANA_Node;
import Java.org.network.mana.mana_components.MANA_Sector;
import Java.org.network.mana.mana_components.MANA_Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Parses out a MANA_Unit into discrete tasks which wrap around MANA_Nodes, MANA_Sectors, and Structural_Plasticity.
 * These tasks are then submitted to a fixed thread pool with synchronization points between each stage.
 *
 */
public class MANA_Executor {

	/** Amount of simulated time that has elapsed. */
	private double time = 0;
	/** Integration time step .*/
	private double dt = Default_Parameters.dt;
    /** Hoe often to invoke strucutural plasticity code. */
	private double spInterval;
	/** Whether or not structural plasticity is active. */
    public boolean spOn = true;

    // The lists containing all the different updtatable components as well as the unit(s) they belong to.
	private List<UpdateTask> updateTasks = new ArrayList<>();
	private List<Callable<Syncable>> syncTasks = new ArrayList<>();
	private List<StructuralPlasticityTask> pruneTasks = new ArrayList<>();
	private List<MANA_Unit> units = new ArrayList<>();

    private final ExecutorService pool;

    private AtomicBoolean invocationComplete = new AtomicBoolean(false);

	/**
	 * Mana_Units must be added in order for this to have something to update.
	 * @param pruneInterval
	 */
	public MANA_Executor(final double pruneInterval) {
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.spInterval = pruneInterval;
	}

	/**
	 * Mana_Units must be added in order for this to have something to update.
	 * @param pruneInterval
	 */
	public MANA_Executor(final double pruneInterval, final double _dt) {
		this(pruneInterval);
		this.dt = _dt;
	}

	/**
	 * Adds a MANA_Unit to this executor, the unit's updatable components are all stored and wrapped in callable
	 * tasks for execution.
	 * @param unit
	 * @param maxID - maximum possible in degree
	 * @param maxOD - maximum possible out degree
	 * @param lambda - the constant for distance based connections (prop to exp(-(D/lambda)^2)
	 * @param maxDist - maximum possible distance between two neurons in the unit.
	 */
	public void addUnit(MANA_Unit unit, int maxID, int maxOD, double lambda, double maxDist) {
		syncTasks.add(new InputSyncTask(unit.externalInp));
		for(MANA_Sector s : unit.sectors.values()) {
			syncTasks.add(new SectorSyncTask(s));
		}
		for(MANA_Node n : unit.nodes) {
			updateTasks.add(new UpdateTask(n));
			pruneTasks.add(new StructuralPlasticityTask(n, maxID, maxOD, lambda, maxDist));
		}
		units.add(unit);
	}

	/**
	 * Updates all mana components given to it to update and synchronizes all inputs and
	 * reservoir neurons associated with those components. Whether or not to execute pruning tasks is determined,
	 * followed by the update of all MANA_Nodes. The thread which updates the last MANA_Node is responsible for updating
	 * the sector. All values up to this used by other neurons are stored in buffers. Only after all these tasks complete
	 * does this execute synchronixation tasks across all sectors. Updates the simulation time.
	 * @throws InterruptedException
	 */
	public void invoke() throws InterruptedException {
		invocationComplete.set(false);
	    if(time > 0 && (int)(time/dt) %  (int)(spInterval /dt) == 0 && spOn) {
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
        }
		try {
			pool.invokeAll(updateTasks);

			pool.invokeAll(syncTasks);
		} catch (Exception e) {
	    	e.printStackTrace();
		}
		for(UpdateTask t : updateTasks) {
			t.node.updated.set(false);
		}
		time += dt;
		invocationComplete.set(true);
//		ct.set(0);
	}

	public class SectorSyncTask implements Callable<Syncable>{

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

	public class StructuralPlasticityTask implements Callable<MANA_Node> {
	    public final MANA_Node node;
	    public final int maxInD, maxOutD;
	    public final double lambda, maxDist;

	    public StructuralPlasticityTask(final MANA_Node node, int maxInD, int maxOutD, double lambda, double maxDist) {
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
