//Date: 2019/03/15
//Author: dylan
//Desc: CSV配置文件 读取

package frame.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.resource.Resource;
import frame.log;

public class CSVReader {

	public static ArrayList<String[]> read(String path, int readStartLine) {
		String line = "";
		String cvsSplitBy = ",";
		Resource resource = new ClassPathResource(path);
		log.info("CSVReader Path:{}", resource.getUrl());
		InputStream inputStream = resource.getStream();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
			int indexLine = 0;
			ArrayList<String[]> datas = new ArrayList<String[]>();
			while ((line = br.readLine()) != null) {
				String[] lineData = line.split(cvsSplitBy);
				if (indexLine >= readStartLine) {
					datas.add(lineData);
				}
				indexLine++;
			}
			return datas;
		} catch (IOException e) {
			log.error("error read csv:", e);
			return null;
		}

	}

}