package Java.org.network.mana.execution.tasks;

import java.util.concurrent.Callable;

public interface UpdateTask<T> extends Callable<T> {

    T getUpdatable();

}
