package guis.mapBuilder;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.annotation.Nonnull;
import java.util.concurrent.Semaphore;

import static helper.Methods.runInNewThread;


public class AutoExecutableQueue {
    private final @Nonnull
    ObservableList<Runnable> runnables_ = FXCollections.observableArrayList();

    private final @Nonnull
    Semaphore runningSemaphore_ = new Semaphore(1);

    /**
     * A Queue that takes {@link Runnable Runnables} and executes them automatically on a new {@link Thread} at some unspecified time in the future.
     */
    public AutoExecutableQueue() {
        runnables_.addListener((ListChangeListener.Change<? extends Runnable> change) -> {
            if (change.next() && change.wasRemoved()) {
                run();
            }
        });
    }

    /**
     * @see #AutoExecutableQueue()
     * @param runnable The {@link Runnable} to push to this Queue.
     */
    public void push(@Nonnull Runnable runnable) {
        runnables_.add(runnable);
        if (runnables_.size() <= 1) {
            run();
        }
    }

    private void run() {
        if (!runnables_.isEmpty()) {
            Runnable runnable = runnables_.get(0);
            runInNewThread(() -> {
                try {
                    runningSemaphore_.acquire();

                    runnable.run();
                    runnables_.remove(runnable);

                    runningSemaphore_.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
