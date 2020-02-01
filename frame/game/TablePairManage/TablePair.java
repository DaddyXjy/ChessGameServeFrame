package frame.game.TablePairManage;

import frame.game.*;
import frame.game.RobotActions.RobotFrame.*;
import frame.game.Table.Status;
import frame.game.RobotActions.PkRobot.*;
import frame.Config;
import java.util.ArrayList;
import frame.log;
import frame.Timer;
import frame.UtilsMgr;
import frame.Callback;

public class TablePair {
    // 桌子数据
    private Table _table;

    public Timer noRobotStartTimer;

    public TablePair(Table table) {

        _table = table;
    }

    // 是否等待玩家进入
    public boolean IsWaitingRole(Role role) {
        // 是否准备销毁
        if (_table.getIsDestroy()) {
            return false;
        }
        // 是否一直等待玩家进入 下注类
        if (_table.getRoom().gameConfig.GetIsAlwaysWaitRole()) {
            return true;
        }
        if (!role.isPairOnGaming() && _table.isGaming()) {
            return false;
        }

        if (_table.isFull()) {
            return false;
        }
        // 捕鱼 与 体验房 只会有一个真实玩家
        if (_table.getRoom().gameConfig.GetIsOneRealPlayer()
                || _table.getRoom().gameConfig.GetRoomType() == RobotBaseConfig.RoomType.Try) {
            if (role instanceof Player && _table.getRealPlayerNum() > 0) {
                return false;
            }
        }
        // 对战类游戏
        if (_table.getRoom().gameConfig.GetGameType() == RobotBaseConfig.Type.Range
                || _table.getRoom().gameConfig.GetGameType() == RobotBaseConfig.Type.Fix) {
            // 这个玩家是真实玩家
            if (role instanceof Player) {
                RobotPkConfig config = ((RobotPkConfig) _table.getRoom().gameConfig);
                // 是否只能有一个真实玩家
                if (!config.GetAllowPlayerPlay()) {
                    if (_table.getRealPlayerNum() > 0) {
                        log.warn("只可以有一个真实玩家");
                        return false;
                    }
                }
                // 是否上次同桌
                if (!config.GetAllowLastSameTablePlayer()) {

                    for (Role var : _table.getRealPlayer().values()) {
                        if (var.isSameTable(role)) {
                            log.warn("不允许上次同桌");
                            return false;
                        }
                    }

                }
                // 是否可以同IP
                if (!config.GetAllowSameIP()) {
                    ArrayList<String> ip = new ArrayList<>();
                    for (Role var : _table.getRealPlayer().values()) {
                        ip.add(var.IP);
                    }
                    if (ip.contains(role.IP)) {
                        log.warn("不允许相同IP");
                        return false;
                    }
                }
            }

        }
        return true;
    }

    // 下注类游戏配座
    public void Pair() {

        if (_table.getIsDestroy()) {
            _table.error(Config.ERR_PAIR_DESTORY);
        }
        // 是否超过最大人数
        if (_table.maxRoles <= _table.getRoleSize()) {
            _table.error(Config.ERR_CREATE_TABLE);
        }
        // 不用进人直接开始
        if (_table.getStatus() == Table.Status.Open) {
            _table.setReadyForStart();
        }
    }

    // 玩家主动配桌
    public Config.Error Pair(Role role) {
        if (_table.getIsDestroy()) {
            return Config.ERR_PAIR_DESTORY;
        }

        // 桌子开启
        if (_table.getStatus() == Table.Status.Open) {
            if (_table.enter(role)) {
                if (_table.getRoleSize() >= -1) {
                    _table.setStatus(Table.Status.Pair);
                    // 是否匹配机器人
                    if (_table.getRoom().gameConfig.IsHaveRobot()) {
                        return _table.getRobotManage().OnTableCreateToPair();
                    } else {
                        // 如果不匹配机器人
                        // 没有机器人准备时间
                        noRobotStartTimer = UtilsMgr.getTaskMgr()
                                .createTimer(_table.getRoom().gameConfig.GetGamePairTime(), new Callback() {

                                    @Override
                                    public void func() {
                                        // 如果足够会开始游戏
                                        if (_table.isGamePlayerEnough()) {
                                            _table.setReadyForStart();
                                        } else {
                                            // 配桌失败
                                            _table.pairFail();
                                        }
                                    }
                                });
                        return Config.ERR_SUCCESS;
                    }
                } else {
                    return Config.ERR_PAIR_FAILURE;
                }
            } else {
                return Config.ERR_PAIR_FAILURE;
            }
        }
        // 配桌状态
        else if (_table.getStatus() == Table.Status.Pair) {
            if (_table.isFull()) {
                return Config.ERR_TABLE_FULL;
            }
            if (_table.enter(role)) {
                if (_table.isFull()) {
                    return _table.setReadyForStart();
                }
                return Config.ERR_SUCCESS;
            } else {
                return Config.ERR_PAIR_FAILURE;
            }
        }
        // 游戏状态
        else if (_table.getStatus() == Table.Status.Game) {
            if (_table.getRoom().gameConfig.GetIsPairOnGameStart()) {
                return _table.enter(role) ? Config.ERR_SUCCESS : Config.ERR_PAIR_FAILURE;
            }
            return Config.ERR_PAIR_TABLE_STATUS_ERROR;
        }
        // 游戏结束
        else if (_table.getStatus() == Table.Status.Begin || _table.getStatus() == Table.Status.End) {
            if (role.isPairOnGaming()) {
                if (_table.isFull()) {
                    return Config.ROB_ERR_TABLE_FULL;
                }
                return _table.enter(role) ? Config.ERR_SUCCESS : Config.ERR_PAIR_FAILURE;
            }
            return Config.ERR_PAIR_TABLE_STATUS_ERROR;
        }
        // 其他
        else {
            return Config.ERR_PAIR_TABLE_STATUS_ERROR;
        }

    }

    // 继续留在桌子
    public Config.Error ReadyContinueGame(Role role) {
        if (_table.getIsDestroy()) {
            return Config.ERR_PAIR_DESTORY;
        }
        // 状态错误
        if (_table.getStatus() != Table.Status.Pair) {
            return Config.ERR_READY_CONTINUE;
        }
        // 设置准备玩家
        if (_table.SetReadyPlayer(role)) {
            // 是否匹配机器人
            if (_table.getRoom().gameConfig.IsHaveRobot()) {
                return _table.getRobotManage().OnTableCreateToPair();
            } else {
                // 如果不匹配机器人
                // 没有机器人准备时间
                noRobotStartTimer = UtilsMgr.getTaskMgr().createTimer(_table.getRoom().gameConfig.GetGamePairTime(),
                        new Callback() {

                            @Override
                            public void func() {
                                // 如果足够会开始游戏
                                if (_table.IsReadyPlayerEnough()) {
                                    _table.setReadyForStart();
                                } else {
                                    // 配桌失败
                                    _table.pairFail();
                                }
                            }
                        });
                return Config.ERR_SUCCESS;
            }

        }
        return Config.ROB_ERR_SUCCESS;
    }
}