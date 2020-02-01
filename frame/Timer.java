package frame;

import lombok.Getter;
public class Timer extends Task {
    private @Getter float time;
    private @Getter long endTimeMillisecond;

    Timer(float time, Callback callback) {
        super(callback);
        this.time = time;
        endTimeMillisecond = UtilsMgr.getMillisecond() + (long) (time * 1000);
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
        expired = (endTimeMillisecond <= UtilsMgr.getMillisecond());
        if (expired) {
            callback.func();
        }
    }
}