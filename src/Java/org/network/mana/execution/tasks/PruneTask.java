package Java.org.network.mana.execution.tasks;

import Java.org.network.mana.execution.MANA_Executor;
import Java.org.network.mana.nodes.MANA_Node;

import java.util.concurrent.Callable;

public class PruneTask implements Callable<MANA_Node> {
    private final MANA_Executor mana_executor;
    public final MANA_Node node;
    public final int maxInD, maxOutD;
    public final double lambda, maxDist;

    public PruneTask(MANA_Executor mana_executor, final MANA_Node node, int maxInD, int maxOutD, double lambda, double maxDist) {
        this.mana_executor = mana_executor;
        this.node = node;
        this.maxInD = maxInD;
        this.maxOutD = maxOutD;
        this.lambda = lambda;
        this.maxDist = maxDist;
    }

    @Override
    public MANA_Node call() throws Exception {
        node.structuralPlasticity(maxInD, maxOutD, lambda, maxDist, mana_executor.getTime());
        return node;
    }
}
