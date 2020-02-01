//Date: 2019/02/16
//Author: dylan
//Desc: 机器人匹配策略

package frame.game;

import frame.*;

import lombok.Getter;
import lombok.Setter;
import frame.Config.RobotPairType;
import frame.util.RandomUtil;
import java.util.HashSet;
import java.util.HashMap;

public final class RobotPairStrategyMgr {
    private @Getter Config.RobotPairType robotPairType;
    private @Getter @Setter Table table;
    private @Getter Boolean robotForbid;
    private Schedule gamingDragRobotSchedule;
    private Schedule gamingKickRobotSchedule;
    private Timer pairTimer;

    public RobotPairStrategyMgr(Config.RobotPairType robotPairType, Table table, Boolean robotForbid) {
        this.robotPairType = robotPairType;
        this.table = table;
        this.robotForbid = robotForbid;
        if (robotPairType.isRobotPairOnGameing) {
            gamingDragRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {
                @Override
                public void func() {
                    Config.Error err = _dragOneRobot();
                    if (err != Config.ERR_SUCCESS && err != Config.ERR_TABLE_FULL) {
                        table.error(err);
                    }
                }
            }, robotPairType.gamingPairGapTime, table);

            gamingKickRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {
                @Override
                public void func() {
                    tryKickLifeTimeOverRobot();
                }
            }, 1, table);
        }
    }

    public void onDestroy() {
        if (gamingDragRobotSchedule != null) {
            gamingDragRobotSchedule.stop();
        }
        if (gamingKickRobotSchedule != null) {
            gamingKickRobotSchedule.stop();
        }
    }

    // 桌子开启尝试匹配机器人
    public Config.Error onTabelStartPair() {
        if (pairTimer != null) {
            pairTimer.stop();
        }
        int pairTime = Math.max(RandomUtil.ramdom(0, robotPairType.gamingPairTime), 2);
        pairTimer = UtilsMgr.getTaskMgr().createTimer(pairTime, new Callback() {
            @Override
            public void func() {
                Config.Error err = _dragRobots();
                if (err == Config.ERR_SUCCESS) {
                    table.setReadyForStart();
                }
            }
        });

        return Config.ERR_SUCCESS;
    }

    public Config.Error onTabelPairEnd() {
        if (pairTimer != null) {
            pairTimer.stop();
        }
        return Config.ERR_SUCCESS;
    }

    // 游戏开启尝试匹配机器人
    public Config.Error onGameBegin() {
        if (robotPairType.isPairOnGameStart) {
            return _dragRobots();
        }
        return Config.ERR_SUCCESS;
    }

    // 游戏结束尝试踢出机器人
    public Config.Error onGameEnd() {
        if (robotPairType.isKickOnGameOver) {
            return _gameEndkickRobots();
        }
        return Config.ERR_SUCCESS;
    }

    // 拉单个机器人进房间
    private Config.Error _dragOneRobot() {
        if (table.isFull()) {
            return Config.ERR_TABLE_FULL;
        }
        Room room = table.getRoom();
        Robot robot = room.genOneRobot(table);
        if (robot == null) {
            return Config.ERR_GEN_ROBOT;
        }
        if (robotPairType.isRobotPairOnGameing) {
            robot.maxTableLifeTime = RandomUtil.ramdom(this.robotPairType.gamingRobotMinLifeTime,
                    this.robotPairType.gamingRobotMaxLifeTime);
        }
        if (robot.table == null) {
            robot.pair(table);
        }
        log.debug("桌子:{}  成功加入一个机器人:{}, 桌子当前人数{}", table.getId(), robot.nickName, table.getRoleSize());
        return Config.ERR_SUCCESS;
    }

    // 拉一堆机器人进房间
    private Config.Error _dragRobots() {
        if (!robotForbid) {
            int min = RandomUtil.ramdom(this.robotPairType.min, this.robotPairType.max);
            int roleSize = table.getRoleSize();
            if (roleSize < min) {
                int num = min - roleSize;
                for (int i = 0; i < num; ++i) {
                    Config.Error err = _dragOneRobot();
                    if (err != Config.ERR_SUCCESS && err != Config.ERR_TABLE_FULL) {
                        table.error(err);
                    }
                    if (err != Config.ERR_SUCCESS && err != Config.ERR_TABLE_FULL) {
                        return err;
                    }
                }
            }
        }
        return Config.ERR_SUCCESS;
    }

    // 踢出单个机器人
    private Config.Error _kickOneRobot(Robot robot) {
        Config.Error err = robot.exitHall();
        log.debug("桌子:{}  成功踢出一个机器人:{}, 桌子当前人数{}", table.getId(), robot.nickName, table.getRoleSize());
        return err;
    }

    // 游戏结束踢出机器人
    private Config.Error _gameEndkickRobots() {
        int roleSize = table.getRoleSize();
        // 保持房间人数在范围之内
        int need2remove = roleSize - RandomUtil.ramdom(this.robotPairType.min, this.robotPairType.max);
        HashMap<Long, Robot> robots = table.getAllRobot();
        for (Robot robot : robots.values()) {
            if (need2remove-- > 0) {
                if (robot.isCanKickOut()) {
                    Config.Error err = _kickOneRobot(robot);
                    if (err != Config.ERR_SUCCESS) {
                        return err;
                    }
                }
            }
        }
        // log.info("该局踢出机器人人数:" + (need2remove > 0 ? need2remove : 0));
        return Config.ERR_SUCCESS;
    }

    private Config.Error tryKickLifeTimeOverRobot() {
        HashMap<Long, Robot> robots = table.getAllRobot();
        for (Robot robot : robots.values()) {
            if (robot.isTableLifeTimeOver()) {
                Config.Error err = _kickOneRobot(robot);
                if (err != Config.ERR_SUCCESS) {
                    return err;
                }
            }
        }
        return Config.ERR_SUCCESS;
    }
}
