package frame.game.RobotActions.PkRobot;

import frame.game.RobotActions.BetsRobot.RobotBetsConfig;
import frame.game.RobotActions.RobotFrame.*;
import frame.util.RandomUtil;
import frame.util.RandomNameUtil;

public class RobotPkLogic extends RobotLogic {

    // 留在本卓概率
    public int OutTablePro;

    // 初始化配置 创建机器人的时候应该已经配置完成
    public void InitData(RobotBaseConfig config) {
        super.InitData(config);
        OutTablePro = ((RobotPkConfig) config).GetOutTablePro();
        if (OutTablePro > 100) {
            OutTablePro = 100;
        }
    }

    // 更新配置
    public void UpData(RobotBaseConfig config) {
        InitData(config);
        OutTablePro = ((RobotPkConfig) config).GetOutTablePro();
        if (OutTablePro > 100) {
            OutTablePro = 100;
        }
    }

    @Override
    // 是否在游戏过程中匹配
    public boolean isPairOnGaming() {
        return false;
    }

    // 是否离场
    public boolean IsOutTable() {
        return RandomUtil.ramdom(0, 99) < OutTablePro ? true : false;
    }

    // 进入房间
    @Override
    protected void onEnterRoom() {
        gender = RandomUtil.ramdom(1);
        nickName = RandomNameUtil.randomName(gender);
        portrait = String.valueOf(RandomUtil.ramdom(0, 5));
    }

}