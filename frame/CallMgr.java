package frame;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
class CallMgr extends Thread {
    private LinkedBlockingQueue<Call> reqs = new LinkedBlockingQueue<>();

    void call(Call c) {
        reqs.add(c);
    }

    @Override
    public void run() {

        while (true) {
            long millisecond = System.currentTimeMillis();
            try {
                Call req = reqs.poll();
                if (req != null) {
                    req.run();
                }
            } catch (Exception err) {
                log.error("多线程任务未知错误！！！ err:", err);
                err.printStackTrace();
            }
            long sleepTime = Config.RATE - (System.currentTimeMillis() - millisecond);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}