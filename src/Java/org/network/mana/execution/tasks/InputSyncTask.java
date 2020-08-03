package Java.org.network.mana.execution.tasks;

import Java.org.network.mana.base_components.InputNeurons;
import Java.org.network.mana.execution.MANA_Executor;

import java.util.concurrent.Callable;

public class InputSyncTask implements Callable<Syncable> {

    private final MANA_Executor mana_executor;
    public final InputNeurons inp;

    public InputSyncTask(MANA_Executor mana_executor, final InputNeurons _inp) {
        this.mana_executor = mana_executor;
        this.inp = _inp;
    }

    @Override
    public InputNeurons call() throws Exception {
        inp.update(mana_executor.getDt(), mana_executor.getTime(), inp.spks);
        return inp;
    }

}
