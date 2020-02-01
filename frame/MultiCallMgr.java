package frame;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class MultiCallMgr extends Thread {

    class CallThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private LinkedBlockingQueue<Call> reqs = new LinkedBlockingQueue<>();

    class CallThread implements Runnable {
        Call call;

        CallThread(Call call) {
            this.call = call;
        }

        @Override
        public void run() {
            call.run();
        }
    }

    class CallThreadRejecter implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.info("线程池卡死！！！！！！！！！！！！！！");
            executor.shutdownNow();
            threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.MAX_CALL_THREAD);
        }

    }

    ThreadPoolExecutor threads;

    public MultiCallMgr() {
        threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.MAX_CALL_THREAD);

        threads.setRejectedExecutionHandler(new CallThreadRejecter());
    }

    public boolean isDone() {
        return threads.getActiveCount() == 0;
    }

    public void call(Call c) {
        reqs.add(c);
    }

    @Override
    public void run() {

        while (true) {
            long millisecond = System.currentTimeMillis();
            try {
                Call req = reqs.poll();
                if (req != null) {
                    CallThread t = new CallThread(req);
                    threads.execute(t);
                }
            } catch (Exception err) {
                log.error("多线程任务未知错误！！！ err:" + err.getMessage());
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