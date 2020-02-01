package frame.game.RobotActions.BetsRobot;

import frame.game.RobotActions.RobotFrame.GameData;

public class BetsGameData extends GameData {
    // 筹码列表
    public long[] chips;
    // 盘口数量
    public int doorCount;
    // 盘口倍数
    public int[] doorTimes;
    // 盘口下注概率
    public int[] doorBettingPro;
    // 是否是对冲游戏
    public boolean isPK = false;
    // 对冲盘口设置 例：索引为0 与5的盘口为对冲 设置其 值都为0 . 1 与 6的盘口为对冲盘口 设置其 值都为1.

    public int[] pkDoorSet;
    // 是否动态增加下注金额
    public boolean isPkGameDynamicBetting = false;
    // 下注时间
    public float bettingTime;

    public BetsGameData(int GameID) {
        super(GameID);
    }

    @Override
    public void ResetData() {
        chips = null;
        doorCount = 0;
        doorTimes = null;
        doorBettingPro = null;
        isPK = false;
        pkDoorSet = null;
        bettingTime = 0;
    }

    public void Init(long[] chips, int doorCount, int[] doorTimes, int[] doorBettingPro, int bettingTime, boolean isPK,
            int[] pkDoorSet) {
        // 校验数据
        if (chips == null && doorCount == 0 && doorTimes == null && doorBettingPro == null) {
            return;
        }
        this.chips = chips.clone();
        this.doorCount = doorCount;
        this.doorTimes = doorTimes.clone();
        this.doorBettingPro = doorBettingPro.clone();
        this.bettingTime = bettingTime;
        this.isPK = isPK;
        this.pkDoorSet = pkDoorSet.clone();
    }
}