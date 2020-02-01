package frame.util;

import java.io.UnsupportedEncodingException;

public class StringReplaceUtils {

	/**
	 * 名字格式化
	 * 
	 * @author Henry
	 * @throws UnsupportedEncodingException
	 * @date 2019年1月14日
	 */
	public static String formatUserName(String userName) {
		StringBuilder afterName = new StringBuilder();
		try {
			char[] charArray = userName.toCharArray();
			int len = 0;
			for (int i = charArray.length - 1; i >= 0; i--) {
				String a = charArray[i] + "";
				len += a.getBytes("GBK").length;
				if (len > 4) {
					afterName.append("*");
				} else {
					afterName.append(a);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return afterName.reverse().toString();
	}

	public static void main(String[] args) {
		/*
		 * System.err.println(formatUserName("张三的大红牛"));
		 * System.err.println(formatUserName("张三的LASSSS"));
		 * System.err.println(formatUserName("张三的928323"));
		 * System.err.println(formatUserName("张三的大345"));
		 */

	}

}
