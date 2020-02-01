package frame;

import lombok.Getter;
import lombok.Setter;

/**
 * Task
 */
public abstract class Task {
    protected @Getter boolean expired;
    protected @Getter Callback callback;

    protected @Getter @Setter Root target;

    public Task(Callback callback) {
        this.callback = callback;
    }

    public void stop() {
        expired = true;
    }

    public abstract void update();
}