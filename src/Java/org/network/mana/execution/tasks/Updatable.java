package Java.org.network.mana.execution.tasks;

public interface Updatable {

    void update(final double time, final double dt);
    void setUpdated(boolean updated);
    boolean isUpdated();

}
