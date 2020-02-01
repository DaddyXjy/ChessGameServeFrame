package frame;

/**
 * GameTrigger
 */
public class Trigger extends Task {
    private boolean trigger;

    public Trigger(Callback callback) {
        super(callback);
    }

    public void fire() {
        trigger = true;
    }

    @Override
    public void update() {
        if (expired) {
            return;
        }
        if (target != null && target.getIsDestroy()) {
            expired = true;
            return;
        }
        expired = trigger;
        if (expired) {
            callback.func();
        }
    }
}