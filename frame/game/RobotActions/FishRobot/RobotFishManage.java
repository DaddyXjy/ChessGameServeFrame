package frame.game.RobotActions.FishRobot;

import frame.game.RobotActions.RobotFrame.*;
import frame.game.*;
import frame.Config;
import frame.UtilsMgr;
import frame.Config.RobotConfig;

import java.util.ArrayList;

import frame.Callback;
import frame.socket.BaseResponse;
import frame.util.RandomUtil;
import frame.Schedule;
import frame.log;

public class RobotFishManage extends RobotManage {

    // 创建桌子尝试匹配
    private int _gamePairTime;
    private RobotFishConfig robotConfig;

    // 更换用户名检测
    private Schedule _addRobotSchedule;
    private Schedule _kickRobotSchedule;
    // 检测间隔
    private int _interval;

    public RobotFishManage(GameType gameType, RobotBaseConfig config, Table table) {
        super(gameType, config, table);
        robotConfig = (RobotFishConfig) config;
        _interval = 10;
    }

    // 桌子创建尝试匹配
    public Config.Error OnTableCreateToPair() {
        log.debug("桌子尝试创建,开启机器人定时器");
        // 是否必须存在一个机器人
        if (robotConfig.GetIsMustHaveRobot()) {
            Config.Error errOne = EnterOneToTable();
            if (errOne != Config.ROB_ERR_SUCCESS) {
                log.warn("创建机器人出错.");
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

    // 销毁
    public void destroy() {
        super.destroy();
        if (_kickRobotSchedule != null) {
            _kickRobotSchedule.stop();
        }
        if (_addRobotSchedule != null) {
            _addRobotSchedule.stop();
        }
    }

    // 桌子准备完毕
    public Config.Error OnStart(RobotGameState.GameState state, GameData gameData) {
        // 设置游戏状态
        SetGameState(state);
        // 先开启踢出计时器
        _kickRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {

            @Override
            public void func() {
                ArrayList<RobotLogic> overRobot = new ArrayList<>();
                for (Long key : robots.keySet()) {
                    if (robots.get(key).isTableLifeTimeOver()
                            || robots.get(key).money <= _table.getRoom().gameConfig.GetRobotTakeScore()) {
                        overRobot.add(robots.get(key));
                    }
                }
                for (int i = 0; i < overRobot.size(); i++) {
                    KickOneRobot(overRobot.get(i));
                }
            }
        }, _interval, _table);
        int timeChange = RandomUtil.ramdom(1, 4);
        // 在开启创建计时器
        _addRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {

            @Override
            public void func() {
                if (!_table.isFull()) {
                    boolean isAdd = RandomUtil.ramdom(100) < 50;
                    if (isAdd || _table.getRealPlayerNum() == 1) {
                        EnterOneToTable();
                    }
                }
            }
        }, (_interval + timeChange), _table);

        return Config.ROB_ERR_SUCCESS;
    }

    // 游戏开始
    public Config.Error OnGameBegin(RobotGameState.GameState state) {
        // if (robotConfig.GetIsRobotPairOnGameing()) {
        // pairTimer = UtilsMgr.getTaskMgr().createTimer(_gamePairTime, new Callback() {
        // @Override
        // public void func() {
        // Config.Error err = EnterMoreToTable();
        // if (err != Config.ROB_ERR_SUCCESS) {
        // _table.error(err);
        // }
        // }
        // });
        // }

        return Config.ERR_SUCCESS;
    }

    // 游戏结束
    public Config.Error OnGameEnd(RobotGameState.GameState state) {
        if (_kickRobotSchedule != null) {
            _kickRobotSchedule.stop();
        }
        if (_addRobotSchedule != null) {
            _addRobotSchedule.stop();
        }
        // 是否退出
        for (Long key : robots.keySet()) {
            // 捕鱼类游戏 玩家退出机器人全部退出
            Config.Error err = KickOneRobot(robots.get(key));
            if (err != Config.ROB_ERR_SUCCESS) {
                return err;
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