//Date: 2019/03/09
//Author: dylan
//Desc: 捕鱼库存配置

package frame.storageLogic;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import frame.Config;
import frame.util.CSVReader;

public final class FishStorageConfig {
    // 小鱼库存档位
    public ArrayList<Long> smallFishStorageMaxList;
    // 小鱼库存概率
    public ArrayList<Float> smallFishStorageMulList;
    // 大鱼库存档位
    public ArrayList<Long> bigFishStorageMaxList;
    // 大鱼库存概率
    public ArrayList<Float> bigFishStorageMulList;
    // 总库存档位
    public ArrayList<Long> totalStorageMaxList;
    // 总库存概率
    public ArrayList<Float> totalStorageMulList;
    // 玩家赢分档位
    public ArrayList<Long> playerWinStorageMaxList;
    // 玩家赢分概率
    public ArrayList<Float> playerWinStorageMulList;
    // 是否开启总库存控制
    public boolean isOpenTotalStorageCtrl;
    // 是否开启玩家赢分档位控制
    public boolean isOpenPlayerWinCtrl = true;
    // 是否开启库存控制
    public boolean isOpenStorageCtrl = true;

    public static final float[] playerWinLevelConfig = { 100, 1000, 10000, 100000 };
    public static final float[] storageRangeLevelConfig = { 100, 1000, 10000, 100000 };

    /**
     * 加载配置
     * 
     * @param: roomField 游戏底分(根据游戏底分加载不同配置)
     */
    public void load(long roomField) {
        isOpenTotalStorageCtrl = false;
        isOpenPlayerWinCtrl = true;
        totalStorageMaxList = new ArrayList<Long>();
        totalStorageMulList = new ArrayList<Float>();
        loadPlayerWinScoreConfig(roomField);
        loadStorageRangeConfig(roomField);
    }

    /**
     * 
     * 
     * @param: roomField 游戏底分(根据游戏底分加载不同配置)
     */

    public int getStorageRangeLevel(float[] config, long roomField) {
        for (int i = 0; i < config.length; i++) {
            if (config[i] < roomField) {
                return i;
            }
        }
        return config.length;
    }

    /**
     * 加载库存档位配置
     */
    public void loadStorageRangeConfig(long baseScore) {
        long judgeScore = (baseScore + 10 * baseScore) / 2;
        ArrayList<String[]> datas = CSVReader.read(getStorageRangeConfigPath(), 2);
        smallFishStorageMaxList = new ArrayList<Long>();
        smallFishStorageMulList = new ArrayList<Float>();
        bigFishStorageMaxList = new ArrayList<Long>();
        bigFishStorageMulList = new ArrayList<Float>();
        if (datas != null) {
            for (String[] lineData : datas) {
                if (lineData != null && lineData.length >= 5) {
                    int fishType = Integer.parseInt(lineData[1]);
                    long scoreMin = (long) (Float.parseFloat(lineData[5]) * Config.MONEY_RATIO);
                    long scoreMax = (long) (Float.parseFloat(lineData[6]) * Config.MONEY_RATIO);
                    // 小鱼
                    Float prob = Float.parseFloat(lineData[4].substring(0, lineData[4].length() - 1)) / 100;
                    if (fishType == 1) {
                        if (scoreMin <= judgeScore && scoreMax > judgeScore) {
                            smallFishStorageMaxList.add((long) (Float.parseFloat(lineData[2]) * Config.MONEY_RATIO));
                            smallFishStorageMulList.add(prob);
                        }
                    } else {
                        if (scoreMin <= judgeScore && scoreMax > judgeScore) {
                            bigFishStorageMaxList.add((long) (Float.parseFloat(lineData[2]) * Config.MONEY_RATIO));
                            bigFishStorageMulList.add(prob);
                        }
                    }
                }
            }
        }
    }

    private static String getStorageRangeConfigPath() {
        return "/csv/storage/fishStorageProb.csv";
    }

    /**
     * 加载玩家赢分档位配置
     */
    public void loadPlayerWinScoreConfig(long baseScore) {
        long judgeScore = (baseScore + 10 * baseScore) / 2;
        ArrayList<String[]> datas = CSVReader.read(getPlayerWinScorePath(), 2);
        playerWinStorageMaxList = new ArrayList<Long>();
        playerWinStorageMulList = new ArrayList<Float>();
        if (datas != null) {
            for (String[] lineData : datas) {
                if (lineData != null && lineData.length >= 4) {
                    String probStr = lineData[3].substring(0, lineData[3].length() - 1);
                    Float prob = Float.parseFloat(probStr) / 100.0f;
                    long scoreMin = (long) (Float.parseFloat(lineData[4]) * Config.MONEY_RATIO);
                    long scoreMax = (long) (Float.parseFloat(lineData[5]) * Config.MONEY_RATIO);
                    if (scoreMin <= judgeScore && scoreMax > judgeScore) {
                        playerWinStorageMaxList.add(Long.parseLong(lineData[1]) * Config.MONEY_RATIO);
                        playerWinStorageMulList.add(prob);
                    }
                }
            }
        }
    }

    private static String getPlayerWinScorePath() {
        return "csv/storage/playerWinProb.csv";
    }
}