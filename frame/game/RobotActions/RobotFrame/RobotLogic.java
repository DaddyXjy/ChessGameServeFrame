package frame.game.RobotActions.RobotFrame;

import frame.game.*;

import frame.game.RobotActions.RobotFrame.RobotBaseConfig;

public class RobotLogic extends Robot {

    // 许机器人进入
    public boolean isHaveRobot;
    // 携带金钱
    public long carryScore;
    // 机器人数量
    public long robotTakeScore;
    // 取钱
    public long takeScore;
    // 昵称更换时间
    public long changeNickNameTime;

    // 初始化逻辑配置 创建机器人的时候应该已经配置完成
    public void InitData(RobotBaseConfig config) {
        try {
            isHaveRobot = config.IsHaveRobot();
            carryScore = config.GetCarryScore();
            robotTakeScore = config.GetRobotTakeScore();
            takeScore = config.GetTakeScore();
            changeNickNameTime = config.GetChangeNickName();

        } catch (Exception ex) {
            assert (false);
        }
    }

    // 更新逻辑配置
    public void UpData(RobotBaseConfig config) {
        try {
            isHaveRobot = config.IsHaveRobot();
            carryScore = config.GetCarryScore();
            robotTakeScore = config.GetRobotTakeScore();
            takeScore = config.GetTakeScore();
            changeNickNameTime = config.GetChangeNickName();

        } catch (Exception ex) {
            assert (false);
        }
    }

    // 根据游戏时间更换昵称
    @Override
    public boolean isTableLifeTimeOver() {
        return getTableLifeTime() > changeNickNameTime;
    }

    // 进入房间
    @Override
    protected void onEnterRoom() {

    }
    // public boolean Init(RobotBaseConfig config) {
    // try {
    // isHaveRobot = config.IsHaveRobot();
    // carryScore = config.GetCarryScore();
    // robotTakeScore = config.GetRobotTakeScore();
    // takeScore = config.GetTakeScore();
    // changeNickName = config.GetChangeNickName();
    // return true;
    // } catch (Exception ex) {
    // return false;
    // }

    // }
}