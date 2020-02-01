package frame.game.RobotActions.PkRobot;

import frame.game.*;
import frame.game.RobotActions.RobotFrame.*;
import frame.Config;
import frame.UtilsMgr;
import frame.Callback;
import frame.log;

import java.util.ArrayList;

import frame.socket.BaseResponse;

//对战类游戏管理
public class RobotPkManage extends RobotManage {

    // 机器人配置
    private RobotPkConfig robotConfig;
    // 游戏匹配时间
    private int _gamePairTime;

    public RobotPkManage(GameType gameType, RobotBaseConfig config, Table table) {
        super(gameType, config, table);
        robotConfig = (RobotPkConfig) config;
        _gamePairTime = robotConfig.GetReadyTime();
    }

    // 桌子创建尝试匹配
    public Config.Error OnTableCreateToPair() {
        log.debug("桌子尝试创建,开启机器人定时器");
        if (robotConfig.GetMaxPlayer() != 2) {
            // 是否必须存在一个机器人
            if (robotConfig.GetIsMustHaveRobot()) {
                Config.Error errOne = EnterOneToTable();
                if (errOne != Config.ROB_ERR_SUCCESS) {
                    log.warn("创建机器人出错.");
                }
            }
        }

        pairTimer = UtilsMgr.getTaskMgr().createTimer(_gamePairTime, new Callback() {
            @Override
            public void func() {
                log.debug("触发创建机器人定时器.");
                Config.Error err = EnterMoreToTable();
                if (err == Config.ROB_ERR_SUCCESS) {
                    // 标记准备
                    _table.setReadyForStart();
                } else {
                    log.warn("创建机器人出错.");
                }
            }
        });
        return Config.ROB_ERR_SUCCESS;
    }

    // 桌子准备完毕
    public Config.Error OnStart(RobotGameState.GameState state, GameData gameData) {

        // 设置游戏状态
        SetGameState(state);

        if (enterRobotSchedule != null) {
            enterRobotSchedule.stop();
        }
        return Config.ROB_ERR_SUCCESS;
    }

    // 游戏开始
    public Config.Error OnGameBegin(RobotGameState.GameState state) {
        return Config.ERR_SUCCESS;
    }

    // 游戏结束
    public Config.Error OnGameEnd(RobotGameState.GameState state) {
        ArrayList<Long> robotID = new ArrayList<>();
        for (Long key : robots.keySet()) {
            robotID.add(key);
        }
        for (int i = 0; i < robotID.size(); i++) {
            if (((RobotPkLogic) robots.get(robotID.get(i))).IsOutTable() || true) {
                Config.Error err = KickOneRobot(robots.get(robotID.get(i)));
                if (err != Config.ROB_ERR_SUCCESS) {
                    return err;
                }
            }
        }
        return Config.ROB_ERR_SUCCESS;
    }

    /**
     * 桌子玩家广播
     *
     * @param msg 需要广播的消息
     */
    public void broadcast(BaseResponse msg) {

    }

    /**
     * 桌子玩家广播
     *
     * @param msg             需要广播的消息
     * @param excludeUniqueId 不包含的玩家 uniqueId
     */
    public void broadcast(BaseResponse msg, long excludeUniqueId) {

    }

    /**
     * 桌子玩家广播
     *
     * @param msg          需要广播的消息
     * @param self         发给自己的消息
     * @param other        发给桌子其他玩家的消息
     * @param selfUniqueId 自己的uniqueId
     */
    public void broadcast(BaseResponse self, BaseResponse other, long selfUniqueId) {

    }
}