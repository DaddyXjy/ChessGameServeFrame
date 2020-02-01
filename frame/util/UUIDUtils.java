package frame.util;

import java.util.Random;
import java.util.UUID;

/**
 * uuid utils
 * 
 * @author Guooo 2017年8月23日 上午6:17:01
 */
public class UUIDUtils {

	public static String getUUID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	/**
	 * 
	 * @Title: GetRandomSequence2
	 * @Description: TODO(生成八位随机数)
	 * @param total
	 * @return String
	 * @author Faker
	 * @date 2018年9月6日
	 */
	public static String getRandomPlayId() {
		StringBuffer id = new StringBuffer("");
		int[] sequence = new int[8];
		int[] output = new int[8];

		for (int i = 0; i < 8; i++) {
			sequence[i] = i;
		}

		Random random = new Random();

		int end = 7;

		for (int i = 0; i < 8; i++) {
			int num = random.nextInt(8);
			output[i] = sequence[num];
			sequence[num] = sequence[end];
			end--;
		}
		for (int i = 0; i < output.length; i++) {
			id.append(output[i]);
		}
		return id.toString();
	}
}
