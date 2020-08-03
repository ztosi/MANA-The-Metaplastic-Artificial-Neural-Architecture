package Java.org.network.mana.execution.tasks;

import Java.org.network.mana.execution.MANA_Executor;
import Java.org.network.mana.nodes.MANA_Node;

import java.util.concurrent.Callable;

public class MANANodeUpdateTask implements UpdateTask<Updatable> {

    private final MANA_Executor mana_executor;
    public final MANA_Node node;

    public MANANodeUpdateTask(MANA_Executor mana_executor, final MANA_Node _node) {
        this.mana_executor = mana_executor;
        this.node = _node;
    }

    @Override
    public MANA_Node call() throws Exception {
        node.update(mana_executor.getTime(), mana_executor.getDt());
        return node;
    }

    @Override
    public MANA_Node getUpdatable() {
        return node;
    }

}
