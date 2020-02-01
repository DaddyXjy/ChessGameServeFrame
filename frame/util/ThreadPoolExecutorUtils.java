package frame.util;

import cn.hutool.core.thread.ThreadUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通用线程池工具类
 * 
 * @author Henry
 *
 */
public class ThreadPoolExecutorUtils {

	/**
	 * 核心线程数
	 */
	private int corePoolSize = 900;
	/**
	 * 最大线程数
	 */
	private int maxPoolSize = 1000;
	/**
	 * 空闲保留时间
	 */
	private int keepAliveTime = 10;

	private ThreadPoolExecutor executor = null;
	private static ThreadPoolExecutorUtils instance = new ThreadPoolExecutorUtils();
	static {
		instance.executor = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(10, false), new ThreadPoolExecutor.AbortPolicy());
		instance.executor.allowCoreThreadTimeOut(true);
	}

	/**
	 * 创建线程池
	 * 
	 * @param corePoolSize  核心线程数
	 * @param maxPoolSize   最大线程数
	 * @param keepAliveTime 空闲保留时间
	 * @param unit          时间单位
	 * @return
	 */
	// public static ThreadPoolExecutorUtils getInstance(int corePoolSize, int maxPoolSize, int keepAliveTime,
	// 		TimeUnit unit) {
	// 	getInstance().executor = ThreadUtil.newExecutor(corePoolSize, maxPoolSize);
	// 	return getInstance();
	// }

	/**
	 * 获取线程池工具类对象
	 * 
	 * @return
	 */
	public static ThreadPoolExecutorUtils getInstance() {
		return instance;
	}

	/**
	 * 执行任务
	 * 
	 * @param runnable
	 */
	public void execute(Runnable runnable) {
		this.executor.execute(runnable);
	}

}
