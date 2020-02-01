package frame.game.RobotActions.RobotFrame;

import java.util.HashMap;

import frame.Callback;
import frame.Config;
import frame.Schedule;
import frame.Timer;
import frame.UtilsMgr;
import frame.log;
import frame.game.Robot;
import frame.game.Role;
import frame.game.Room;
import frame.game.Table;
import frame.game.RobotActions.BetsRobot.RobotBetsConfig;
import frame.socket.BaseResponse;
import frame.util.RandomNameUtil;
import frame.util.RandomUtil;

//机器人管理类 统筹规划 以桌子为单位
public abstract class RobotManage {

	public enum GameType {
		// 下注类
		Bet,
		// 匹配类
		Pair,
		// 捕鱼类
		fish
	}

	// 机器人
	protected HashMap<Long, RobotLogic> robots;
	protected Table _table;
	// 游戏类型
	protected GameType _gameType;
	// 加载机器人数量
	protected int _loadRobotBettingCount;
	// 机器人配置
	private RobotBaseConfig robotConfig;
	// 游戏状态
	private RobotGameState.GameState _gameState;

	// 循环创建删除机器人间隔
	protected Schedule enterRobotSchedule;
	protected Schedule kickRobotSchedule;
	// 循环间隔
	protected float intervalTime;
	// 尝试匹配时间间隔
	protected Timer pairTimer;

	/** 循环创建上庄机器人定时器 */
	private Schedule enterBankerSchedule;

	/** 创建上庄机器人时间 */
	protected long bankeRobotTime;

	public RobotManage(GameType gameType, RobotBaseConfig config, Table table) {
		robots = new HashMap<>();
		_gameType = gameType;
		_loadRobotBettingCount = config.GetRobotCount();
		robotConfig = config;
		intervalTime = 0.1f;
		_table = table;
	}

	public Timer GetPairTimer() {
		return pairTimer;
	}

	// 设置游戏状态
	public void SetGameState(RobotGameState.GameState state) {
		_gameState = state;
	}

	// 获取游戏状态
	public RobotGameState.GameState GetGameState() {
		return _gameState;
	}

	// 释放定时器
	public void destroy() {
		if (enterRobotSchedule != null) {
			enterRobotSchedule.stop();
		}
		if (kickRobotSchedule != null) {
			kickRobotSchedule.stop();
		}
		if (pairTimer != null) {
			pairTimer.stop();
		}
	}

	// 开发接口 子类自己实现
	// 桌子创建尝试匹配
	public abstract Config.Error OnTableCreateToPair();

	// 桌子准备完毕 当前桌子为空闲状态 准备开始
	public abstract Config.Error OnStart(RobotGameState.GameState state, GameData gameData);

	// 设置游戏配置
	public boolean OnSetGameData(GameData gameData) {
		return true;
	}

	// 游戏开始
	public abstract Config.Error OnGameBegin(RobotGameState.GameState state);

	// 游戏结束
	public abstract Config.Error OnGameEnd(RobotGameState.GameState state);

	// 停止下注
	public Config.Error OnStopBetting() {
		return Config.ROB_ERR_SUCCESS;
	}

	// 下注类自己重写
	// 下注阶段
	public Config.Error OnGameBetting(RobotGameState.GameState state, long bankerScore) {
		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 设置机器人上庄，上庄类自己重写
	 * 
	 * @return
	 */
	public Config.Error setRobotUpBanker() {
		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 校验上庄列表，上庄类自己重写
	 * 
	 */
	public void checkBankerList() {
	}

	/** 机器人下庄 */
	public Config.Error onChangeBanker(Role role) {
		return Config.ROB_ERR_SUCCESS;
	}

	// 更新上庄列表
	public Config.Error OnUpDataUpBankerList(Role upBanker, Role downBanker) {
		return Config.ROB_ERR_SUCCESS;
	}

	// 生成一个机器人
	protected RobotLogic CreateOneRobot() {
		Room room = _table.getRoom();
		Robot robot = room.genOneRobot(_table);
		if (robot != null) {
			log.debug("桌子:{},成功创建机器人{}", _table.getId(), robot.nickName);
			return (RobotLogic) robot;
		} else {
			return null;
		}
	}

	// 关闭配桌
	public Config.Error OnTabelPairEnd() {
		if (pairTimer != null) {
			pairTimer.stop();
		}
		return Config.ERR_SUCCESS;
	}

	// 添加机器人
	protected Config.Error PutRobot(Robot robot) {
		if (robot != null) {
			robots.put(robot.uniqueId, (RobotLogic) robot);
		}
		return Config.ROB_ERR_SUCCESS;
	}

	/** 添加上庄机器人 */
	protected Config.Error putBankerRobot(Robot robot) {
		return Config.ROB_ERR_SUCCESS;
	}

	/** 清理上庄机器人 */
	protected void clearBankerRobot() {
	}

	// 删除机器人
	protected Config.Error DeleteRobot(Robot robot) {
		if (robot != null) {
			if (robots.containsKey(robot.uniqueId)) {
				robots.remove(robot.uniqueId);
			}
		}
		return Config.ROB_ERR_SUCCESS;
	}

	// 机器人进入房间
	protected Config.Error EnterOneToTable() {
		// 如果桌子已满 不会在创建机器人
		if (_table.isFull()) {
			return Config.ROB_ERR_TABLE_FULL;
		}
		if (_table.getAllRobot().size() < _loadRobotBettingCount && robotConfig.IsHaveRobot()) {
			RobotLogic robot = CreateOneRobot();
			if (robot == null) {
				return Config.ROB_ERR_CREATE_ROBOT;
			}
			robot.InitData(robotConfig);
			// 机器人携带金钱
			// robot.money = robot.carryScore;

			// 进入房间
			Config.Error err = robot.enterRoom(_table.getRoom(), _table);
			if (err != Config.ERR_SUCCESS) {
				_table.getRoom().exit(robot);
				log.info("机器人进入房间失败:{}", err.msg);
			}

			// 设定更换昵称时间
			// robot.changeNickNameTime = robotConfig.GetChangeNickName();
			if (robot.getTable() == null) {
				robot.pair(_table);
			}
			err = PutRobot(robot);
			// 添加机器人
			if (err != Config.ROB_ERR_SUCCESS) {
				return err;
			}
			log.debug("桌子:{}  成功加入一个机器人:{}, 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());

		}
		// else {
		// return Config.ROB_ERR_ENTER_TABLE;
		// }

		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 上庄机器人进入房间
	 * 
	 * @return
	 */
	protected Config.Error enterBankerRobot(RobotBetsConfig config) {
		RobotLogic robot = CreateOneRobot();
		if (robot == null) {
			return Config.ROB_ERR_CREATE_ROBOT;
		}
		robot.InitData(robotConfig);
		// 机器人携带金钱
		robot.money = config.GetUpBankerScore();

		// 进入房间
		Config.Error err = robot.enterRoom(_table.getRoom(), _table);
		if (err != Config.ERR_SUCCESS) {
			_table.getRoom().exit(robot);
			log.info("机器人进入房间失败:{}", err.msg);
		}

		if (robot.getTable() == null) {
			robot.pair(_table);
		}

		err = PutRobot(robot);
		err = putBankerRobot(robot);

		// 添加机器人
		if (err != Config.ROB_ERR_SUCCESS) {
			return err;
		}
		log.debug("桌子:{}  成功加入一个上庄机器人:{}, 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());
		return Config.ROB_ERR_SUCCESS;
	}

	// 加载一些机器人进入房间
	protected Config.Error EnterMoreToTable() {
		// 如果桌子已满 不会在创建机器人
		if (_table.isFull()) {
			return Config.ROB_ERR_TABLE_FULL;
		}

		// 创建上庄机器人
		if (robotConfig instanceof RobotBetsConfig && _table.getBetRoomCfg().getSysBanker() == 1) {
			RobotBetsConfig config = (RobotBetsConfig) robotConfig;
			long time = System.currentTimeMillis();
			if (config.IsUpBanker() && time >= bankeRobotTime) {
				if (enterBankerSchedule != null) {
					enterBankerSchedule.stop();
				}

				// 清理上庄机器人
				clearBankerRobot();
				// 上庄机器人数量判断
				int bankerCount = config.GetRobotUpBankerCount();
				for (Role role : _table.getBankerList()) {
					if (role instanceof Robot) {
						bankerCount--;
					}
				}

				if (bankerCount >= 1) {
					bankeRobotTime = time + 60 * 60 * 1000;
					// 循环创建机器人
					enterBankerSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {
						@Override
						public void func() {
							Config.Error err = enterBankerRobot(config);
							if (err != Config.ROB_ERR_SUCCESS) {
								log.warn("{} ERROR:{} ", _table.getRoom().getRoomTextInfo(), err.msg);
							}
							if (robots.size() >= robotConfig.GetRobotCountForRoomCount() || err != Config.ROB_ERR_SUCCESS) {
								log.debug("桌子:{} 桌子当前人数{},已经达到限定值,停止计时器.", _table.getId(), _table.getRoleSize());
								if (enterBankerSchedule != null) {
									enterBankerSchedule.stop();
								}
							}
						}
					}, intervalTime, bankerCount, _table);
				}
			}
		}

		// 创建下注机器人
		if (_table.getAllRobot().size() < _loadRobotBettingCount && robotConfig.IsHaveRobot()) {
			if (enterRobotSchedule != null) {
				enterRobotSchedule.stop();
			}

			int loadRobotCount = 0;
			if (_gameType == GameType.Bet) {
				loadRobotCount = _loadRobotBettingCount - _table.getAllRobot().size();
			} else {
				loadRobotCount = RandomUtil.ramdom(robotConfig.minPlayer, robotConfig.maxPlayer);
				loadRobotCount = loadRobotCount - _table.getRoleSize();
				if (loadRobotCount <= 0) {
					return Config.ROB_ERR_SUCCESS;
				}
			}
			log.debug("桌子:{}  准备创建机器人数为:{}, 桌子当前人数{}!!!!!!!!!!!!!!!!!!!", _table.getId(), loadRobotCount, _table.getRoleSize());
			if (robotConfig.GetGameType() == RobotBaseConfig.Type.One || robotConfig.GetGameType() == RobotBaseConfig.Type.Solo) {
				// 循环创建机器人
				enterRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {
					@Override
					public void func() {
						Config.Error err = EnterOneToTable();
						if (err != Config.ROB_ERR_SUCCESS) {
							log.warn("{} ERROR:{} ", _table.getRoom().getRoomTextInfo(), err.msg);
						}
						if (robots.size() >= robotConfig.GetRobotCountForRoomCount() || err != Config.ROB_ERR_SUCCESS) {
							log.debug("桌子:{} 桌子当前人数{},已经达到限定值,停止计时器.", _table.getId(), _table.getRoleSize());
							if (enterRobotSchedule != null) {
								enterRobotSchedule.stop();
							}
						}
					}
				}, intervalTime, loadRobotCount, _table);
			} else {
				for (int i = 0; i < loadRobotCount; i++) {
					Config.Error err = EnterOneToTable();
					if (err != Config.ROB_ERR_SUCCESS) {
						_table.error(err);
					}
				}
			}
		}

		return Config.ROB_ERR_SUCCESS;
	}

	// 踢出机器人
	protected Config.Error KickOneRobot(RobotLogic robot) {
		Config.Error err = DeleteRobot(robot);
		if (err != Config.ROB_ERR_SUCCESS) {
			log.debug("桌子:{}  成功踢出一个机器人:{},失败!!!! 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());
			return err;
		}
		// 每次清理机器人就更换机器人昵称
		robot.nickName = RandomNameUtil.randomName(robot.gender);
		err = robot.exitRoom();
		if (err == Config.ERR_SUCCESS) {

			log.debug("桌子:{}  成功踢出一个机器人:{}, 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());
			return Config.ROB_ERR_SUCCESS;
		}
		return err;
	}

	// 踢出单个机器人
	protected Config.Error KickOneRobot(Robot robot) {

		Config.Error err = DeleteRobot(robot);
		if (err != Config.ROB_ERR_SUCCESS) {
			log.debug("桌子:{} 成功踢出一个机器人:{},失败!!!! 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());
			return err;
		}
		err = robot.exitRoom();
		if (err == Config.ERR_SUCCESS) {

			log.debug("桌子:{} 成功踢出一个机器人:{}, 桌子当前人数{}", _table.getId(), robot.nickName, _table.getRoleSize());
			return Config.ROB_ERR_SUCCESS;
		}
		return err;
	}

	// 游戏结束踢出机器人
	protected Config.Error GameEndOutOneToTable(Robot robot) {
		return KickOneRobot(robot);
	}

	// 清空房间所有机器人
	protected Config.Error KickALLRobot() {
		for (Long key : robots.keySet()) {
			RobotLogic robot = robots.get(key);
			Config.Error err = KickOneRobot(robot);
			if (err != Config.ROB_ERR_SUCCESS) {
				log.debug("桌子:{}  踢出机器人失败.", _table.getId());
			}
		}

		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 */
	public abstract void broadcast(BaseResponse msg);

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 * @param excludeUniqueId
	 *            不包含的玩家 uniqueId
	 */
	public abstract void broadcast(BaseResponse msg, long excludeUniqueId);

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 * @param self
	 *            发给自己的消息
	 * @param other
	 *            发给桌子其他玩家的消息
	 * @param selfUniqueId
	 *            自己的uniqueId
	 */
	public abstract void broadcast(BaseResponse self, BaseResponse other, long selfUniqueId);

}