package frame;

import frame.game.*;
import frame.http.GlobeResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Call {
    private @Setter Callback call;
    private Trigger success;
    private Trigger failure;
    private @Setter long timeout = Config.CALL_TIME_OUT;
    private @Getter long runTime;
    private boolean isDone;

    public void setSuccess(Callback callback) {
        success = UtilsMgr.getTaskMgr().createTrigger(callback);
    }

    public void setFailure(Callback callback) {
        failure = UtilsMgr.getTaskMgr().createTrigger(callback);
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() - this.runTime >= timeout;
    }

    public void run() {
        this.runTime = System.currentTimeMillis();
        call.func();
        if(call.getData()!=null){
            GlobeResponse data = (GlobeResponse) call.getData();

            Trigger ok;
            Trigger no;
            if ("200".equals(data.getCode())) {
                ok = success;
                no = failure;
            } else {
                ok = failure;
                no = success;
            }
            if (ok != null) {
                ok.getCallback().setData(data.getData());
                ok.fire();
            }
            if (no != null) {
                no.stop();
            }
        }
    }
}