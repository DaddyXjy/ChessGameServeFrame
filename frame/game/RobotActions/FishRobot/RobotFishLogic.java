package frame.game.RobotActions.FishRobot;

import frame.game.RobotActions.RobotFrame.*;
import frame.util.RandomUtil;
import frame.util.RandomNameUtil;
import frame.Timer;
import frame.Callback;
import frame.UtilsMgr;
import frame.log;

public class RobotFishLogic extends RobotLogic {
    private Timer exitTimer;
    private boolean isExitRoom = false;

    // 初始化配置 创建机器人的时候应该已经配置完成
    public void InitData(RobotBaseConfig config) {
        super.InitData(config);
    }

    // 更新配置
    public void UpData(RobotBaseConfig config) {
        InitData(config);
    }

    @Override
    // 是否在游戏过程中匹配
    public boolean isPairOnGaming() {
        return true;
    }

    // 进入房间
    @Override
    protected void onEnterRoom() {
        gender = RandomUtil.ramdom(1);
        nickName = RandomNameUtil.randomName(gender);
        portrait = String.valueOf(RandomUtil.ramdom(0, 5));
        isExitRoom = false;
    }

    // 延迟退出房间
    public void onDelayExitRoom() {
        if (isExitRoom) {
            return;
        } else {
            isExitRoom = true;
        }
        int exitTime = RandomUtil.ramdom(1, 4);
        exitTimer = UtilsMgr.getTaskMgr().createTimer(exitTime, new Callback() {

            @Override
            public void func() {
                // log.warn("机器人{},退出游戏1", nickName);
                exitRoom();
            }
        });
    }
}