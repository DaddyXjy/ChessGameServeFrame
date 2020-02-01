package frame.socket.redis;

import org.apache.commons.lang3.StringUtils;

import frame.Config;
import frame.log;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author KAZER
 * @version 创建时间：2019年5月28日 下午2:51:04 类说明
 */
public class redisPool {
	// Redis的端口号
	private static int PORT = 6379;

	// 可用连接实例的最大数目默认值为8；
	// 如果赋值为-1则表示不限制 如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
	private static int MAX_ACTIVE = 8;

	// 控制一个pool最多有多少个状态为idle(空闲的)jedis实例，默认值8
	private static int MAX_IDLE = 8;

	// 等待可用连接的最大时间
	// 表示永不超时如果超过等待时间,则直接抛出JedisConnectionException
	private static int MAX_WAIT = 3000;

	// 超时时间
	private static int TIMEOUT = 10000;

	// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
	private static boolean TEST_ON_BORROW = false;

	private static JedisPool jedisPool = null;
	/**
	 * redis密码
	 */
	private static String redisPassWord = "VAFCy693nvUXiNRY";

	/**
	 * 初始化Redis连接池
	 */
	private static void initialPool() {
		try {
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(MAX_ACTIVE);
			config.setMaxIdle(MAX_IDLE);
			config.setMaxWaitMillis(MAX_WAIT);
			config.setTestOnBorrow(TEST_ON_BORROW);
			
			if(Config.passWord) {
				jedisPool = new JedisPool(config, Config.RedisHost, PORT, TIMEOUT,redisPassWord);
			}else {
				jedisPool = new JedisPool(config, Config.RedisHost, PORT,TIMEOUT);
			}
		} catch (Exception e) {
			log.error("First create JedisPool error : " + e);
		}
	}

	/**
	 * 同步初始化
	 */
	private static synchronized void poolInit() {
		if (jedisPool == null) {
			initialPool();
		}
	}

	/**
	 * @return Jedis
	 */
	public synchronized static Jedis getJedis() {
		if (jedisPool == null) {
			poolInit();
		}
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
		} catch (Exception e) {
			log.error("Get jedis error : " + e);
		} finally {
			returnResource(jedis);
		}
		return jedis;
	}

	public static void main(String args[]) {
		redisPool.setString("123", "123");
		System.err.println(redisPool.getString("123"));
	}

	/**
	 * 释放jedis资源
	 *
	 * @param jedis
	 */
	public static void returnResource(final Jedis jedis) {
		if (jedis != null && jedisPool != null) {
			jedis.close();
		}
	}

	/**
	 * 设置 String
	 *
	 * @param key
	 * @param value
	 */
	public synchronized static void setString(String key, String value) {
		try {
			value = StringUtils.isEmpty(value) ? "" : value;
			getJedis().set(key, value);
		} catch (Exception e) {
			log.error("Set key error : " + e);
		}
	}

	/**
	 * 获取String值
	 *
	 * @param key
	 * @return value
	 */
	public synchronized static String getString(String key) {
		if (getJedis() == null || !getJedis().exists(key)) {
			return null;
		}
		String val = getJedis().get(key);
		return val.substring(1,val.length()-1).replace("\\", "");
	}

	/**
	 * 截取String值
	 *
	 * @param key
	 * @return value
	 */
	public synchronized static String getRange(String key) {
		if (getJedis() == null || !getJedis().exists(key)) {
			return null;
		}
		return getJedis().getrange(key, 1l, -2l);
	}
}
