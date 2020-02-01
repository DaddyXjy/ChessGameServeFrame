package frame;

/**
 * Schedule
 */
public class Schedule extends Task {
    private long delay;
    private int repeat = -1;

    private long interval = 0;
    private long delta = 0;

    public Schedule(Callback callback) {
        this(callback, 0, -1, 0);
    }

    public Schedule(Callback callback, float interval) {
        this(callback, interval, -1, 0);
    }

    public Schedule(Callback callback, float interval, int repeat) {
        this(callback, interval, repeat, 0);
    }

    public Schedule(Callback callback, float interval, int repeat, float delay) {
        super(callback);
        this.interval = (long) (interval * 1000);
        this.repeat = repeat;
        this.delay = (long) (delay * 1000);
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

        if (delay > 0) {
            delay -= UtilsMgr.getDelta();
        }

        if (delay > 0) {
            return;
        }
        if (repeat != 0) {
            if (interval <= 0) {
                repeat--;
                expired = repeat == 0;
                callback.func();
            } else {
                delta += UtilsMgr.getDelta();
                while (delta > interval) {
                    if (repeat == 0) {
                        break;
                    }
                    delta -= interval;
                    repeat--;
                    expired = repeat == 0;
                    callback.func();
                }
            }
        }
    }
}