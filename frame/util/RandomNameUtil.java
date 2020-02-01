//Date: 2019/03/15
//Author: dylan
//Desc: 随机名字库 工具

package frame.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import frame.log;

public class RandomNameUtil {
    private static ArrayList<String> firstNames = new ArrayList<String>();
    private static ArrayList<String> secondMaleNames = new ArrayList<String>();
    private static ArrayList<String> secondfeMaleNames = new ArrayList<String>();

    // 加载名字库
    public static void loadConfig() {
        ArrayList<String[]> datas = CSVReader.read(getNameConfigPath() , 3);
        if (datas != null) {
            for (String[] lineData : datas) {
                if (lineData != null && lineData.length >= 3) {
                    firstNames.add(lineData[0]);
                    secondMaleNames.add(lineData[1]);
                    secondfeMaleNames.add(lineData[2]);
                }
            }
        }
    }

    private static String getNameConfigPath() {
        return "/csv/randomName.csv";
    }

    /**
     * 随机名字
     * 
     * @gender: 0 男 1 女
     */
    public static String randomName(int gender) {
        if (firstNames.size() == 0 || secondMaleNames.size() == 0 || secondfeMaleNames.size() == 0) {
            return "";
        }

        int randomIndex1 = RandomUtil.ramdom(firstNames.size() - 1);
        ArrayList<String> secondNames = gender == 0 ? secondMaleNames : secondfeMaleNames;
        int randomIndex2 = RandomUtil.ramdom(secondNames.size() - 1);
        StringBuilder builder = new StringBuilder();
        builder.append(firstNames.get(randomIndex1));
        builder.append(secondNames.get(randomIndex2));
        return builder.toString();
    }

}
