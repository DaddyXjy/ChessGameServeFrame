package frame.http;

import lombok.Getter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    @Getter
   static ThreadPoolExecutor executor =
            new ThreadPoolExecutor(5, 10, 200, TimeUnit.MICROSECONDS,
                    new ArrayBlockingQueue<Runnable>(5));

}
