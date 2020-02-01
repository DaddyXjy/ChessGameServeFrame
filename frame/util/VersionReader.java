//Date: 2019/03/15
//Author: dylan
//Desc: CSV配置文件 读取

package frame.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.resource.Resource;
import frame.log;

public class VersionReader {

    public static Integer read(String path) {
        try {
            Resource resource = new ClassPathResource(path);
            log.info("version Path:{}", resource.getUrl());
            InputStream inputStream = resource.getStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = br.readLine();
            if (line.equals("")) {
                return 0;
            }
            return Integer.parseInt(line);
        } catch (Exception e) {
            log.error("error read version:", e);
            return 0;
        }
    }

}