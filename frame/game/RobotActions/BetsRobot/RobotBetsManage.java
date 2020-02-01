package frame.game.RobotActions.BetsRobot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import frame.Callback;
import frame.Config;
import frame.Schedule;
import frame.Timer;
import frame.UtilsMgr;
import frame.log;
import frame.game.Banker;
import frame.game.Player;
import frame.game.Robot;
import frame.game.Role;
import frame.game.Table;
import frame.game.RobotActions.RobotFrame.GameData;
import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.game.RobotActions.RobotFrame.RobotGameState;
import frame.game.RobotActions.RobotFrame.RobotManage;
import frame.socket.BaseResponse;
import frame.util.RandomUtil;

// create by zzy 
//下注机器人管理类
public class RobotBetsManage extends RobotManage {

	// 上庄与申请了上庄列表的机器人
	private HashMap<Long, RobotBetsLogic> upBankerRobots;
	// 下注机器人
	private HashMap<Long, RobotBetsLogic> bettingRobots;
	// 被还原 待删除机器人
	private HashMap<Long, RobotBetsLogic> restoreRobots;
	// 机器人配置
	private RobotBetsConfig robotConfig;

	// 上庄数量
	private int _robotUpBankerCount;
	// 空几局坐庄
	private int _waitEmptyBankerCount;
	// 下注比例
	private int _bettingScorePro;

	// 踢出计数
	private int kickRobotCount;

	// 游戏数据
	// 庄家ID
	private Long _bankerID;
	// 庄家携带金钱
	private long _bankerScore;
	// 游戏基本数据
	private BetsGameData _gameData;
	// 性格统计
	private int[] _robotCharacterCount;

	// 下注

	// 下注总金额
	private long _allBettingScore;
	// 盘口限制
	private ArrayList<Long> _doorScore;
	// 下注时间管理
	private ArrayList<Schedule> _bettingScheduleList;
	// 对冲游戏盘口下注金额
	private ArrayList<Long> _pkGameDoorBettingScore;
	// 上庄
	// 空闲上庄局数
	// private int _noBankerCnt;
	// 上庄定时器管理
	private ArrayList<Timer> _upBankerTimerList;
	// 上庄列表
	private ArrayList<Role> _upBankerList;

	public RobotBetsManage(GameType gameType, RobotBaseConfig config, Table table) {
		// 初始化父类
		super(gameType, config, table);
		// 设置状态
		SetGameState(RobotGameState.BETS_GAMESTATE_NULL);
		// 初始化数据
		// robots = new HashMap<>();
		upBankerRobots = new HashMap<>();
		bettingRobots = new HashMap<>();
		restoreRobots = new HashMap<>();
		// _table = table;
		robotConfig = (RobotBetsConfig) config;

		if (!Init()) {
			log.error(this.getClass().getSimpleName() + ":" + new Exception().getStackTrace()[0].getMethodName() + ":初始化数据出错");
		}
		// 间隔时间
		intervalTime = 0.5f;
		kickRobotCount = 0;

		_bankerID = new Long(-1);
		_bankerScore = 0;
		_upBankerTimerList = new ArrayList<Timer>();
		_upBankerList = new ArrayList<Role>();
		// 初始化游戏数据 设置gameID
		_gameData = null;
		_robotCharacterCount = new int[3];
		_bettingScheduleList = new ArrayList<Schedule>();
		_pkGameDoorBettingScore = new ArrayList<Long>();
		_doorScore = new ArrayList<Long>();
		// 尝试匹配机器人
		OnTableCreateToPair();
	}

	// 重置数据
	public void ResetData() {
		// 设置状态
		SetGameState(RobotGameState.BETS_GAMESTATE_NULL);
		robots.clear();
		upBankerRobots.clear();
		bettingRobots.clear();
		restoreRobots.clear();
		intervalTime = 0.5f;
		kickRobotCount = 0;
		if (enterRobotSchedule != null) {
			enterRobotSchedule.stop();
		}
		if (kickRobotSchedule != null) {
			kickRobotSchedule.stop();
		}
		if (pairTimer != null) {
			pairTimer.stop();
		}

		if (!Init()) {
			log.error(this.getClass().getSimpleName() + ":" + new Exception().getStackTrace()[0].getMethodName() + ":初始化数据出错");
		}

		_bankerID = new Long(-1);
		_bankerScore = 0;
		_upBankerTimerList = new ArrayList<Timer>();
		_upBankerList = new ArrayList<Role>();
		if (_gameData != null) {
			_gameData.ResetData();
		}
		_robotCharacterCount = null;
		// 清除定时器
		RemoveBettingSchedule();
		_pkGameDoorBettingScore = new ArrayList<Long>();
		_doorScore = new ArrayList<Long>();
	}

	// 获取机器人数量
	public boolean Init() {

		if (robotConfig == null) {
			return false;
		}
		// 加载机器人数量
		// _loadRobotBettingCount = robotConfig.GetRobotCount();
		_robotUpBankerCount = robotConfig.GetRobotUpBankerCount();
		_waitEmptyBankerCount = robotConfig.GetWaitNoBankerCount();
		_bettingScorePro = robotConfig.GetAllDoorScoreToBettingScoreRro();

		if (_loadRobotBettingCount <= 0 && _robotUpBankerCount <= 0 && _waitEmptyBankerCount <= 0 && _bettingScorePro <= 0) {
			return false;
		} else {
			return true;
		}
	}

	// 开放接口
	// 桌子创建尝试匹配
	public Config.Error OnTableCreateToPair() {
		if (pairTimer != null) {
			pairTimer.stop();
		}
		if (enterRobotSchedule != null) {
			enterRobotSchedule.stop();
		}

		int pairTime = Math.max(RandomUtil.ramdom(0, robotConfig.GetGamePairTime()), 1);
		pairTimer = UtilsMgr.getTaskMgr().createTimer(pairTime, new Callback() {
			@Override
			public void func() {
				// Config.Error err = EnterMoreToTable();
				// if (err == Config.ERR_SUCCESS || err == Config.ROB_ERR_SUCCESS) {
				// _table.setReadyForStart();
				// }
				EnterMoreToTable();
			}
		});
		return Config.ROB_ERR_SUCCESS;
	}

	// 桌子准备完毕
	/**
	 * 
	 * @param state
	 *            游戏状态
	 * @param gameData
	 *            游戏数据
	 * @return
	 */
	public Config.Error OnStart(RobotGameState.GameState state, GameData gameData) {
		if (state != RobotGameState.BETS_GAMESTATE_NULL) {
			return Config.ROB_ERR_GAME_STATE;
		}
		SetGameState(state);
		if (gameData == null) {
			return Config.ROB_ERR_START_ERROR;
		}
		if (!(gameData instanceof BetsGameData)) {
			return Config.ROB_ERR_START_ERROR;
		}
		// 游戏数据
		// _gameData = (BetsGameData) gameData;
		SetGameData((BetsGameData) gameData);
		if (_gameData.bettingTime == 0 || _gameData.chips == null || _gameData.doorBettingPro == null || _gameData.doorCount <= 0
				|| _gameData.doorTimes == null) {
			log.error("OnStart:游戏设置出错");
			return Config.ROB_ERR_START_ERROR;
		}
		if (_gameData.doorBettingPro.length != _gameData.doorCount || _gameData.doorTimes.length != _gameData.doorCount) {
			log.error("OnStart:游戏设置出错");
			return Config.ROB_ERR_START_ERROR;
		}
		// 如果是对冲游戏
		for (int i = 0; i < _gameData.doorCount; i++) {
			_doorScore.add(new Long(0));
			if (_gameData.isPK) {
				_pkGameDoorBettingScore.add(new Long(0));
			}
		}
		ChipsChack();
		return Config.ROB_ERR_NOTIN_TABLE;
	}

	private void SetGameData(BetsGameData gameData) {
		_gameData = new BetsGameData(gameData.GameID);
		_gameData.bettingTime = ((BetsGameData) gameData).bettingTime;
		_gameData.chips = ((BetsGameData) gameData).chips.clone();
		_gameData.doorBettingPro = ((BetsGameData) gameData).doorBettingPro.clone();
		_gameData.doorCount = ((BetsGameData) gameData).doorCount;
		_gameData.doorTimes = ((BetsGameData) gameData).doorTimes.clone();
		_gameData.isPK = ((BetsGameData) gameData).isPK;
		_gameData.isPkGameDynamicBetting = ((BetsGameData) gameData).isPkGameDynamicBetting;
		if (((BetsGameData) gameData).pkDoorSet != null) {
			_gameData.pkDoorSet = ((BetsGameData) gameData).pkDoorSet.clone();
		}
	}

	// 筹码校验
	public void ChipsChack() {
		ArrayList<Long> chipsList = new ArrayList<>();
		if (_gameData.chips.length <= 0) {
			log.error("OnStart:游戏设置出错,筹码数量为空.");
		}
		for (int i = 0; i < _gameData.chips.length; i++) {
			if (robotConfig.GetMinRoomBettingScore() <= _gameData.chips[i]) {
				chipsList.add(new Long(_gameData.chips[i]));
			}
		}
		if (chipsList.size() <= 0) {
			log.error("OnStart:游戏设置出错,筹码与房间最小限红不匹配,最小限红{},最小筹码{}.", robotConfig.GetMinRoomBettingScore(), _gameData.chips[0]);
		}
		long[] chipsTemp = new long[chipsList.size()];
		for (int i = 0; i < chipsList.size(); i++) {
			chipsTemp[i] = chipsList.get(i);
		}
		_gameData.chips = chipsTemp;
	}

	public boolean OnSetGameData(GameData gameData) {
		if (gameData == null) {
			return false;
		}
		if (!(gameData instanceof BetsGameData)) {
			return false;
		}
		// 游戏数据
		BetsGameData gameDataTemp = (BetsGameData) gameData;
		if (gameDataTemp.bettingTime == 0 || gameDataTemp.chips == null || gameDataTemp.doorBettingPro == null || gameDataTemp.doorCount <= 0
				|| gameDataTemp.doorTimes == null) {
			log.error("OnStart:游戏设置出错");
			return false;
		}
		if (gameDataTemp.doorBettingPro.length != gameDataTemp.doorCount || gameDataTemp.doorTimes.length != gameDataTemp.doorCount) {
			log.error("OnStart:游戏设置出错");
			return false;
		}
		// 如果是对冲游戏
		for (int i = 0; i < gameDataTemp.doorCount; i++) {
			_doorScore.add(new Long(0));
			if (gameDataTemp.isPK) {
				_pkGameDoorBettingScore.add(new Long(0));
			}
		}
		// _gameData = gameDataTemp;
		SetGameData((BetsGameData) gameData);
		return true;
	}

	// 游戏开始
	/**
	 * 
	 * @param doorTimes
	 *            盘口倍率
	 * @param doorBettingPro
	 *            每个盘口下注概率
	 * @param isPK
	 *            是否是对赌游戏（如只有两个盘口）
	 * @return
	 */
	public Config.Error OnGameBegin(RobotGameState.GameState state) {
		// 状态校验
		if (state != RobotGameState.BETS_GAMESTATE_BEGIN && GetGameState() == RobotGameState.BETS_GAMESTATE_BETTING) {
			return Config.ROB_ERR_GAME_STATE;
		}
		// 设置游戏状态
		SetGameState(state);
		// 游戏开始 尝试加载机器人
		// if (robotConfig.IsHaveRobot()) {
		EnterMoreToTable();
		// }

		return Config.ROB_ERR_NOTIN_TABLE;
	}

	// 下注阶段
	/**
	 * 游戏内部调用
	 * 
	 * @param state
	 *            游戏状态
	 * @param bankerScore
	 *            庄家金额(无庄家,系统上庄时,庄家金额为0)
	 * @return
	 */
	public Config.Error OnGameBetting(RobotGameState.GameState state, long bankerScore) {
		// 游戏状态校验
		if (state != RobotGameState.BETS_GAMESTATE_BETTING && GetGameState() != RobotGameState.BETS_GAMESTATE_BEGIN) {
			return Config.ROB_ERR_GAME_STATE;
		}
		// 设置游戏状态
		SetGameState(state);
		// 停止退出计时器
		if (kickRobotSchedule != null) {
			kickRobotSchedule.stop();
		}
		// this._bankerID = bankerID;

		Banker banker = _table.getNowBanker();
		if (banker.getIdentity() != Banker.SYSTEM_BANKER) {
			this._bankerScore = _table.getNowBanker().getBanker().money;
		} else {
			this._bankerScore = 0;

			// 达到空庄局数设置创建机器人时间
			int bankerNum = _table.getNowBanker().getBankerNum();
			if (bankerNum >= _waitEmptyBankerCount) {
				bankeRobotTime = 0;
				// log.info("桌子:{} 达到空庄局数:{},重置创建上庄机器人时间.", _table.getId(), bankerNum);
			}
			// log.info("桌子:{} 空庄局数:{}.", _table.getId(), bankerNum);
		}

		// log.info("当前庄家ID:",_bankerID)
		if (this._bankerScore <= 0) {
			// 系统坐庄 下注金额无限
			_allBettingScore = 0;
			// _noBankerCnt++;
		} else {
			_allBettingScore = (this._bankerScore / 100) * _bettingScorePro;
			// _noBankerCnt = 0;
		}

		return GetBettingProject();
	}

	/** 机器人下庄 */
	@Override
	public Config.Error onChangeBanker(Role role) {
		if (role == null) {
			return Config.ROB_ERR_SUCCESS;
		}

		// 重新加入下注和上庄列表
		RobotBetsLogic robot = (RobotBetsLogic) role;
		bettingRobots.put(robot.uniqueId, robot);
		upBankerRobots.put(robot.uniqueId, robot);

		return Config.ROB_ERR_SUCCESS;
	}

	// 更新上庄列表
	public Config.Error OnUpDataUpBankerList(Role upBanker, Role downBanker) {
		try {

			// 添加
			if (upBanker != null) {

				_upBankerList.add(upBanker);

				if (robots.containsKey(upBanker.uniqueId)) {

					upBankerRobots.put(upBanker.uniqueId, (RobotBetsLogic) robots.get(upBanker.uniqueId));
				}
			}

			// 清除
			if (downBanker != null) {

				if (_upBankerList.contains(downBanker)) {
					_upBankerList.remove(downBanker);
				}
				if (robots.containsKey(downBanker.uniqueId)) {
					if (upBankerRobots.containsKey(downBanker.uniqueId)) {
						upBankerRobots.remove(downBanker.uniqueId);
					}
				}
			}
		} catch (Exception ex) {
			return Config.ROB_ERR_UPDATA_BANKERLIST;
		}
		if (upBankerRobots.size() >= robotConfig.GetUpBankerListCount() + 1) {
			// 清除上庄定时器
			RemoveUpBankerTimer();
		}
		return Config.ROB_ERR_SUCCESS;
	}

	// 清除游戏计时器
	public Config.Error OnStopBetting() {
		SetGameState(RobotGameState.BETS_GAMESTATE_STOP_BETTING);
		// 清理下注计时器
		RemoveBettingSchedule();

		return Config.ROB_ERR_SUCCESS;
	}

	// 游戏结束
	public Config.Error OnGameEnd(RobotGameState.GameState state) {
		// 游戏状态校验
		if (state != RobotGameState.BETS_GAMESTATE_END && GetGameState() != RobotGameState.BETS_GAMESTATE_BETTING
				&& GetGameState() != RobotGameState.BETS_GAMESTATE_STOP_BETTING) {
			return Config.ROB_ERR_GAME_STATE;
		}
		// 清理下注计时器
		RemoveBettingSchedule();
		// 设置游戏状态
		SetGameState(state);
		_doorScore.clear();
		for (int i = 0; i < _gameData.doorCount; i++) {
			_doorScore.add(new Long(0));
		}
		// 已经到达生命周期的机器人
		ArrayList<Long> liftOver = new ArrayList<Long>();
		int userCount = _table.getRoleSize();
		// 将要踢出的机器人数量
		int wileTickCount = (userCount - robotConfig.GetRobotCountForRoomCount()) > 0 ? userCount - robotConfig.GetRobotCountForRoomCount() : 0;
		for (Long key : bettingRobots.keySet()) {
			RobotBetsLogic robot = bettingRobots.get(key);
			if (robot.getRobotType() == bankerRobotType) {
				// 上庄机器人：携带金钱小于房间上庄最小值，禁用上庄机器人
				if (robot.isTableLifeTimeOver() || robot.money < _table.getBetRoomCfg().getBankerCond() || !robotConfig.IsUpBanker()) {
					liftOver.add(key);
				}
			} else if (robot.isTableLifeTimeOver() || robot.money <= robotConfig.GetRobotTakeScore()
					|| robot.money <= robotConfig.GetMinRoomBettingScore() || !robotConfig.IsHaveRobot()) {
				// 下注机器人：更换昵称，需要取钱，携带金钱小于房间下注最小值，禁用下注机器人
				liftOver.add(key);
			}
		}

		// 需要踢出的机器人
		int tickMoreCount = wileTickCount - liftOver.size();
		// 准备踢人
		if (tickMoreCount > 0) {
			for (Long key : bettingRobots.keySet()) {
				if (!liftOver.contains(key)) {
					liftOver.add(key);
					tickMoreCount--;
					if (tickMoreCount <= 0) {
						break;
					}
				}
			}
		}
		kickRobotCount = 0;
		// 踢出机器人
		if (liftOver.size() > 0) {
			if (kickRobotSchedule != null) {
				kickRobotSchedule.stop();
			}
			// 循环删除机器人
			kickRobotSchedule = UtilsMgr.getTaskMgr().createSchedule(new Callback() {

				@Override
				public void func() {
					RobotBetsLogic robotTemp = null;
					try {
						robotTemp = bettingRobots.get(liftOver.get(kickRobotCount));
					} catch (Exception ex) {
						log.debug("桌子:{},删除机器人失败,无法读取机器人信息!!!!!!", _table.getId());
					}
					if (robotTemp != null) {
						kickRobotCount++;
						log.debug("桌子:{},准备踢出机器人{}.", _table.getId(), robotTemp.nickName);
						if (GameEndOutOneToTable(robotTemp) != Config.ROB_ERR_SUCCESS) {
							log.error("桌子:{},删除机器人{},失败!!!!!!", _table.getId(), robotTemp.nickName);
							// 添加还原 下次结束待删除
							restoreRobots.put(robotTemp.uniqueId, robotTemp);
							robots.put(robotTemp.uniqueId, robotTemp);
						}
						log.debug("桌子:{},踢出机器人成功.", _table.getId());
						// 停止计时器
						if (kickRobotCount >= liftOver.size()) {
							if (kickRobotSchedule != null) {
								kickRobotSchedule.stop();
							}
						}
					} else {
						log.debug("桌子:{},删除机器人失败,无法读取机器人信息!!!!!!", _table.getId());
					}
				}
			}, intervalTime, liftOver.size());
		}

		// 添加机器人进入房间
		EnterMoreToTable();

		return Config.ROB_ERR_SUCCESS;
	}
	// 内部方法

	// 添加机器人
	protected Config.Error PutRobot(Robot robot) {
		super.PutRobot(robot);
		if (robot != null) {
			// 设置机器人性格
			SetRobotCharacter((RobotBetsLogic) robot);
			// 所有新机器人开始都是下注机器人
			bettingRobots.put(robot.uniqueId, (RobotBetsLogic) robot);
			return Config.ROB_ERR_SUCCESS;
		} else {
			return Config.ROB_ERR_PUT_ROBOT;
		}
	}

	private final static int bankerRobotType = 1;

	/** 添加上庄机器人 */
	@Override
	protected Config.Error putBankerRobot(Robot robot) {
		if (robot != null) {
			RobotBetsLogic betRobot = (RobotBetsLogic) robot;
			betRobot.setRobotType(bankerRobotType);
			upBankerRobots.put(robot.uniqueId, betRobot);
			return Config.ROB_ERR_SUCCESS;
		} else {
			return Config.ROB_ERR_PUT_ROBOT;
		}
	}

	/** 清理上庄机器人 */
	@Override
	protected void clearBankerRobot() {
		List<Robot> list = new ArrayList<>();
		for (RobotBetsLogic robot : bettingRobots.values()) {
			if (robot.getRobotType() == bankerRobotType) {
				list.add(robot);
			}
		}

		for (Robot robot : list) {
			KickOneRobot(robot);
		}
	}

	// 删除机器人
	protected Config.Error DeleteRobot(Robot robot) {
		super.DeleteRobot(robot);
		if (robot == null) {
			return Config.ROB_ERR_DELE_ROBOT;
		}
		if (upBankerRobots.containsKey(robot.uniqueId)) {
			upBankerRobots.remove(robot.uniqueId);
		}
		if (bettingRobots.containsKey(robot.uniqueId)) {
			bettingRobots.remove(robot.uniqueId);
		}
		if (restoreRobots.containsKey(robot.uniqueId)) {
			restoreRobots.remove(robot.uniqueId);
		}
		return Config.ROB_ERR_SUCCESS;
	}

	// 设置机器人性格
	protected void SetRobotCharacter(RobotBetsLogic robot) {
		robot.cur_RobotCharacter = GetCharacter();
	}

	// 获取性格
	protected RobotBetsConfig.RobotCharacter GetCharacter() {
		if (_robotCharacterCount[0] < (robotConfig.GetTimidRobotPro() / 10)) {
			_robotCharacterCount[0]++;
			return RobotBetsConfig.RobotCharacter.Timid;
		}
		if (_robotCharacterCount[1] < (robotConfig.GetNomalRobotPro() / 10)) {
			_robotCharacterCount[1]++;
			return RobotBetsConfig.RobotCharacter.Nomal;
		}
		if (_robotCharacterCount[2] < (robotConfig.GetBoldBettingPro() / 10)) {
			_robotCharacterCount[2]++;
			return RobotBetsConfig.RobotCharacter.Bold;
		}
		for (int i = 0; i < _robotCharacterCount.length; i++) {
			_robotCharacterCount[i] = 0;
		}
		// 强行设置胆小机器人
		_robotCharacterCount[0]++;
		return RobotBetsConfig.RobotCharacter.Timid;
	}

	// =====逻辑
	// 下注工作
	private Config.Error GetBettingProject() {

		ArrayList<RobotBetsLogic> robotList = new ArrayList<RobotBetsLogic>();
		// 下注机器人数量
		ArrayList<RobotBetsLogic> bettingRobotList = new ArrayList<RobotBetsLogic>();
		for (Long key : bettingRobots.keySet()) {
			robotList.add(bettingRobots.get(key));
		}
		int willBettingCount = robotConfig.GetRobotPlayCount();
		if (robotList.size() <= willBettingCount) {
			willBettingCount = robotList.size();
		}
		for (int i = 0; i < willBettingCount; i++) {
			int index = RandomUtil.ramdom(0, (robotList.size() - 1));
			RobotBetsLogic robot = robotList.get(index);
			if (robot == null) {
				log.debug("获取下注机器人出错,无法获取下注机器人");
				return Config.ROB_ERR_ROBCOUNT_Betting;
			}
			if (_bankerID == robot.uniqueId) {
				continue;
			}
			bettingRobotList.add(robot);
			robotList.remove(index);
		}
		// 下注方案
		if (_allBettingScore == 0) {
			ByRobotCountBetting(bettingRobotList);
		} else {
			ByBankerScoreBetting(bettingRobotList);
		}
		return Config.ROB_ERR_SUCCESS;
	}

	// 获取单个机器人下注金额
	private ArrayList<Long> GetRobotBettingScore(RobotBetsLogic robot) {
		long roomBettingScore = robotConfig.GetRoomBettingScore() - robotConfig.GetMinRoomBettingScore();
		switch (robot.cur_RobotCharacter) {
		// 胆小
		case Timid: {
			roomBettingScore = (roomBettingScore / 100) * robotConfig.GetTimidBettingPro();
			break;
		}
		// 正常
		case Nomal: {
			roomBettingScore = (roomBettingScore / 100) * robotConfig.GetNomalBettingPro();
			break;
		}
		// 胆大
		case Bold: {
			roomBettingScore = (roomBettingScore / 100) * robotConfig.GetBoldBettingPro();
			break;
		}
		}
		// 下注金额
		roomBettingScore = roomBettingScore + robotConfig.GetMinRoomBettingScore();
		// 下注金额不能大于携带金钱
		if (robot.money < roomBettingScore) {
			roomBettingScore = robot.money;
		}
		// 筹码个数
		int chipsCount = robotConfig.GetChipsCount();
		ArrayList<Long> chipsList = new ArrayList<Long>();
		int index = _gameData.chips.length - 1;
		int whileCnt = 0;
		do {
			if (roomBettingScore >= _gameData.chips[index]) {
				roomBettingScore -= _gameData.chips[index];
				chipsList.add(_gameData.chips[index]);
			} else {
				index--;
			}
			whileCnt++;
		} while (index >= 0 && whileCnt < 100 && chipsList.size() < chipsCount);
		if (chipsList.size() == 0) {
			log.debug("游戏最小下注与最小筹码不匹配.");
			return null;
		}
		if (whileCnt > 99) {
			log.warn("机器人下注次数过多.");
		}
		if (chipsCount < chipsList.size()) {
			// 删除多余筹码
			int removeIndex = chipsList.size() - chipsCount;
			for (int i = 0; i < removeIndex; i++) {
				try {
					chipsList.remove(chipsCount);
				} catch (Exception ex) {
					log.error("拆分筹码出错");
				}
			}
		} else if (chipsCount > chipsList.size()) {
			// 拆分筹码
			if (SplitChips(chipsList, chipsCount) != Config.ROB_ERR_SUCCESS) {
				// 拆分筹码出错
				log.error("拆分筹码出错");
				return null;
			}
		}
		return chipsList;
	}

	// 有限人数下注 下注总金额无上限 会被限红拦截
	private Config.Error ByRobotCountBetting(ArrayList<RobotBetsLogic> bettingRobotList) {

		if (bettingRobotList.size() <= 0) {
			return Config.ROB_ERR_SUCCESS;
		}
		HashMap<RobotBetsLogic, BettingData> rToBData = new HashMap<>();
		ArrayList<BettingData> betList = new ArrayList<>();
		ArrayList<RobotBetsLogic> robList = new ArrayList<>();
		// 下注
		if (bettingRobotList.size() > 0) {
			for (int i = 0; i < bettingRobotList.size(); i++) {
				// 如果机器人携带金钱为房间下注最小值 不下注
				if (bettingRobotList.get(i).money <= robotConfig.GetMinRoomBettingScore()) {
					continue;
				}
				RobotBetsLogic robotBet = bettingRobotList.get(i);
				// 机器人下注次数
				// int bettingCount = robotConfig.GetBettingCount();
				// 下注间隔
				// int bettingIntervalTime = _gameData.bettingTime / bettingCount;
				BettingData bettingData = SendByRobotCountBetting(bettingRobotList.get(i));

				if (bettingData == null) {
					continue;
				}
				if (bettingData.chipsIndex.size() <= 0) {
					continue;
				}
				rToBData.put(robotBet, bettingData);
				// float bettingIntervalTime = (_gameData.bettingTime - 3) /
				// bettingData.chipsIndex.size();
				// log.info("机器人{},下注次数为{},下注间隔为{}.", robotBet.nickName,
				// bettingData.chipsIndex.size(),
				// bettingIntervalTime);

				// 创建下注循环计时器
				// _bettingScheduleList.add(UtilsMgr.getTaskMgr().createSchedule(new Callback()
				// {

				// @Override
				// public void func() {
				// BettingData sendBettingData = bettingData.SendBetting(bettingCount);
				// if (sendBettingData != null) {
				// if (!robotBet.RobotSendBetting(sendBettingData)) {
				// log.error("桌子:{},机器人{},下注失败.", _table.getId(), robotBet.nickName);
				// }
				// }

				// }
				// }, bettingIntervalTime, bettingCount, _table));
			}
		}
		if (rToBData.size() > 0) {
			boolean isWhile = true;
			int whileCnt = 0;
			do {
				ArrayList<RobotBetsLogic> whileDelte = new ArrayList<>();
				for (RobotBetsLogic key : rToBData.keySet()) {
					BettingData data = rToBData.get(key);
					if (data.chipsIndex.size() > 0) {
						ArrayList<Long> chipsTmp = new ArrayList<>();
						chipsTmp.add(data.chipsIndex.get(0));
						BettingData dataTmp = new BettingData(data.doorIndex, chipsTmp);
						betList.add(dataTmp);
						robList.add(key);
						data.chipsIndex.remove(0);
					} else {
						whileDelte.add(key);
					}
				}
				for (int i = 0; i < whileDelte.size(); i++) {
					rToBData.remove(whileDelte.get(i));

				}
				if (rToBData.size() <= 0) {
					isWhile = false;
				}
				whileCnt++;
			} while (isWhile && whileCnt < 200);
		}
		if (betList.size() == 0) {
			return Config.ROB_ERR_SUCCESS;
		}
		float bettingIntervalTime = ((float) _gameData.bettingTime - 1f) / (float) (betList.size());
		int cnt = betList.size();
		// if (betList.size() > 0) {
		// for (int i = 0; i < betList.size(); i++) {
		// robList.get(i).RobotSendBetting(betList.get(i));
		// }
		// }

		_bettingScheduleList.add(UtilsMgr.getTaskMgr().createSchedule(new Callback() {

			@Override
			public void func() {
				if (betList.size() > 0) {
					long score = _doorScore.get(betList.get(0).doorIndex).longValue();
					// 添加盘口限红
					if ((score + betList.get(0).chipsIndex.get(0)) <= robotConfig.GetAllDoorScoreToBettingScore()) {
						robList.get(0).RobotSendBetting(betList.get(0));
						_doorScore.set(betList.get(0).doorIndex, new Long(score + betList.get(0).chipsIndex.get(0)));
					}
					robList.remove(0);
					betList.remove(0);
				}
			}
		}, bettingIntervalTime, cnt, _table));
		return Config.ROB_ERR_SUCCESS;
	}

	// 发送根据人数下注金额
	private BettingData SendByRobotCountBetting(RobotBetsLogic robot) {
		ArrayList<Long> chipsList = new ArrayList<Long>();
		try {
			ArrayList<Long> chipsTemp = GetRobotBettingScore(robot);
			if (chipsTemp != null) {
				chipsList.addAll(chipsTemp);
			} else {
				return null;
			}
		} catch (Exception ex) {
			log.error("添加筹码出错");
			return null;
		}
		long bettingScore = 0;
		for (int i = 0; i < chipsList.size(); i++) {
			bettingScore += chipsList.get(i);
		}
		int bettingDoorIndex = GetArrayRandomByPro(_gameData.doorBettingPro, bettingScore);
		if (bettingDoorIndex == -1) {
			return null;
		}

		BettingData bettingData = new BettingData(bettingDoorIndex, chipsList);

		bettingScore = 0;
		for (int i = 0; i < bettingData.chipsIndex.size(); i++) {
			bettingScore += bettingData.chipsIndex.get(i);
		}
		long score = _doorScore.get(bettingDoorIndex).longValue();
		_doorScore.set(bettingDoorIndex, new Long(score + bettingScore));
		return bettingData;
	}

	// 有限金额下注
	private Config.Error ByBankerScoreBetting(ArrayList<RobotBetsLogic> bettingRobotList) {
		HashMap<RobotBetsLogic, BettingData> rToBData = new HashMap<>();
		ArrayList<BettingData> betList = new ArrayList<>();
		ArrayList<RobotBetsLogic> robList = new ArrayList<>();
		// 下注
		if (bettingRobotList.size() > 0) {
			for (int i = 0; i < bettingRobotList.size(); i++) {
				// 如果机器人携带金钱为房间下注最小值 不下注
				if (bettingRobotList.get(i).money <= robotConfig.GetMinRoomBettingScore()) {
					continue;
				}
				RobotBetsLogic robotBet = bettingRobotList.get(i);
				// 机器人下注次数
				// int bettingCount = robotConfig.GetBettingCount();
				// 下注间隔
				// float bettingIntervalTime = _gameData.bettingTime / bettingCount;
				BettingData bettingData = SendByBankerScoreBetting(bettingRobotList.get(i));
				if (bettingData == null) {
					continue;
				}
				// 如果下注金额为负 停止下注
				if (_allBettingScore <= 0) {
					break;
				}
				rToBData.put(robotBet, bettingData);
				// // 创建下注循环计时器
				// _bettingScheduleList.add(UtilsMgr.getTaskMgr().createSchedule(new Callback()
				// {
				//
				// @Override
				// public void func() {
				// BettingData sendBettingData = bettingData.SendBetting(bettingCount);
				// if (sendBettingData != null) {
				// if (!robotBet.RobotSendBetting(sendBettingData)) {
				// log.error("桌子:{},机器人{},下注失败.", _table.getId(), robotBet.nickName);
				// }
				// }
				//
				// }
				// }, bettingIntervalTime, bettingCount, _table));
			}
		}

		if (rToBData.size() > 0) {
			boolean isWhile = true;
			int whileCnt = 0;
			do {
				ArrayList<RobotBetsLogic> whileDelte = new ArrayList<>();
				for (RobotBetsLogic key : rToBData.keySet()) {
					BettingData data = rToBData.get(key);
					if (data.chipsIndex.size() > 0) {
						ArrayList<Long> chipsTmp = new ArrayList<>();
						chipsTmp.add(data.chipsIndex.get(0));
						BettingData dataTmp = new BettingData(data.doorIndex, chipsTmp);
						betList.add(dataTmp);
						robList.add(key);
						data.chipsIndex.remove(0);
					} else {
						whileDelte.add(key);
					}
				}
				for (int i = 0; i < whileDelte.size(); i++) {
					rToBData.remove(whileDelte.get(i));

				}
				if (rToBData.size() <= 0) {
					isWhile = false;
				}
				whileCnt++;
			} while (isWhile && whileCnt < 200);
		}
		if (betList.size() == 0) {
			return Config.ROB_ERR_SUCCESS;
		}
		float bettingIntervalTime = (_gameData.bettingTime - 1f) / betList.size();
		int cnt = betList.size();

		_bettingScheduleList.add(UtilsMgr.getTaskMgr().createSchedule(new Callback() {
			@Override
			public void func() {
				if (betList.size() > 0) {
					long score = _doorScore.get(betList.get(0).doorIndex).longValue();
					// 添加盘口限红
					if ((score + betList.get(0).chipsIndex.get(0)) <= robotConfig.GetAllDoorScoreToBettingScore()) {
						robList.get(0).RobotSendBetting(betList.get(0));
						_doorScore.set(betList.get(0).doorIndex, new Long(score + betList.get(0).chipsIndex.get(0)));
					}
					robList.remove(0);
					betList.remove(0);
				}
			}
		}, bettingIntervalTime, cnt, _table));

		return Config.ROB_ERR_SUCCESS;
	}

	// 发送根据庄家金额决定下注金额
	private BettingData SendByBankerScoreBetting(RobotBetsLogic robot) {
		ArrayList<Long> chipsList = new ArrayList<Long>();
		try {
			ArrayList<Long> chipsTemp = GetRobotBettingScore(robot);
			if (chipsTemp != null) {
				chipsList.addAll(chipsTemp);
			}
		} catch (Exception ex) {
			log.error("添加筹码出错");
			return null;
		}
		long bettingScore = 0;
		for (int i = 0; i < chipsList.size(); i++) {
			bettingScore += chipsList.get(i);
		}
		int bettingDoorIndex = GetArrayRandomByPro(_gameData.doorBettingPro, bettingScore);
		if (bettingDoorIndex == -1) {
			return null;
		}
		BettingData bettingData = new BettingData(bettingDoorIndex, chipsList);

		long curBettingScore = 0;
		for (int i = 0; i < bettingData.chipsIndex.size(); i++) {
			curBettingScore += (bettingData.chipsIndex.get(i)) * _gameData.doorTimes[bettingDoorIndex];
		}
		// 如果为对冲游戏
		if (_gameData.isPK) {
			_allBettingScore -= GetPkGameBettingRealityScore(bettingDoorIndex, curBettingScore);
		} else {
			_allBettingScore -= curBettingScore;
		}
		bettingScore = 0;
		for (int i = 0; i < bettingData.chipsIndex.size(); i++) {
			bettingScore += bettingData.chipsIndex.get(i);
		}
		long score = _doorScore.get(bettingDoorIndex).longValue();
		_doorScore.set(bettingDoorIndex, new Long(score + bettingScore));
		return bettingData;
	}

	// 获取实际对冲金额
	private long GetPkGameBettingRealityScore(int bettingDoorIndex, long curBettingScore) {
		int pkType = _gameData.pkDoorSet[bettingDoorIndex];
		int otherPkDoor = -1;
		for (int i = 0; i < _gameData.doorCount; i++) {
			if (_gameData.pkDoorSet[i] == pkType && i != bettingDoorIndex) {
				otherPkDoor = i;
			}
		}

		// 没有找到对冲盘口
		if (otherPkDoor == -1) {
			return curBettingScore;
		}

		long score = _pkGameDoorBettingScore.get(bettingDoorIndex) + curBettingScore;
		// 以前的对冲差值
		long oldDifferScore = 0;
		long newDifferScore = 0;

		oldDifferScore = Math.abs(_pkGameDoorBettingScore.get(bettingDoorIndex) - _pkGameDoorBettingScore.get(otherPkDoor));
		// 设置对冲金额
		_pkGameDoorBettingScore.set(bettingDoorIndex, score);
		newDifferScore = Math.abs(_pkGameDoorBettingScore.get(bettingDoorIndex) - _pkGameDoorBettingScore.get(otherPkDoor));

		return newDifferScore - oldDifferScore;
	}

	// 拆分筹码
	private Config.Error SplitChips(ArrayList<Long> chipsList, int chipsCount) {
		if (chipsCount - chipsList.size() < 0) {
			return Config.ROB_ERR_ROBCOUNT_Betting;
		}
		int splitCount = chipsCount - chipsList.size();
		boolean over = true;
		ArrayList<Long> chips = new ArrayList<Long>();
		int whileCount = 0;
		// 强制循环六次以下
		do {
			// 要被才分的筹码
			long willSplitChips = chipsList.get(0);
			int index = _gameData.chips.length - 1;
			int whileCnt = 0;
			do {
				if (willSplitChips > _gameData.chips[index]) {
					willSplitChips -= _gameData.chips[index];
					chips.add(_gameData.chips[index]);
				} else {
					index--;
				}
				whileCnt++;
			} while (index >= 0 && whileCnt < 100);
			if (whileCnt > 99) {
				log.error("游戏筹码设置出错.");
			}
			chipsList.remove(0);
			if (chipsList.size() <= 0) {
				over = false;
			}
			if (chips.size() >= splitCount) {
				over = false;
			}
			whileCount++;
		} while (over && whileCount < 6);
		for (int i = 0; i < chips.size(); i++) {
			chipsList.add(chips.get(i));
		}
		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 设置机器人上庄
	 */
	@Override
	public Config.Error setRobotUpBanker() {
		// 是否可以上庄
		if (!robotConfig.IsUpBanker() || _table.getBetRoomCfg().getSysBanker() == 2) {
			return Config.ROB_ERR_SUCCESS;
		}

		// 是否有上庄机器人
		if (upBankerRobots.size() <= 0) {
			return Config.ROB_ERR_SUCCESS;
		}

		// 上庄金额
		long upBankerScore = _table.getBetRoomCfg().getBankerCond();
		ArrayList<RobotBetsLogic> willUpBankerRobot = new ArrayList<RobotBetsLogic>();
		for (RobotBetsLogic robot : upBankerRobots.values()) {
			if (robot.money >= upBankerScore) {
				willUpBankerRobot.add(robot);
			}
		}

		// 移除之前的定时器
		// RemoveUpBankerTimer();

		for (RobotBetsLogic robot : willUpBankerRobot) {
			upBankerRobots.remove(robot.uniqueId);
			// 5分钟内上庄
			int upBankerTime = RandomUtil.ramdom(1, 300);
			// _upBankerTimerList.add(
			// log.info("机器人{}在{}秒后申请上庄", robot.nickName, upBankerTime);
			UtilsMgr.getTaskMgr().createTimer(upBankerTime, new Callback() {
				@Override
				public void func() {
					// 是否可以上庄
					if (!robotConfig.IsUpBanker() || _table.getBetRoomCfg().getSysBanker() == 2 || robot.getTable() == null) {
						return;
					}

					// 上庄金额不足
					if (robot.money < upBankerScore) {
						upBankerRobots.put(robot.uniqueId, robot);
						return;
					}

					if (_table.upBanker(robot)) {
						robot.RobotSendUpBanker();
						bettingRobots.remove(robot.uniqueId);
					}
				}
			});
			// );
		}

		return Config.ROB_ERR_SUCCESS;
	}

	/**
	 * 校验上庄列表
	 */
	@Override
	public void checkBankerList() {
		List<Role> list = _table.getBankerList();
		if (list == null || list.size() <= 0) {
			return;
		}

		// (3)当申请列表第一位是真实玩家时，正在坐庄的机器人有60%概率立即下庄
		if (list.get(0) instanceof Player) {
			if (_table.getNowBanker().getIdentity() == Banker.ROBOT_BANKER) {
				if (RandomUtil.ramdom(100) <= 60) {
					_table.setDownBanker(true);
				}
			}
		}

		if (list.size() <= 2) {
			return;
		}

		// 统计真实玩家数量
		int playerCount = 0;
		for (Role role : list) {
			if (role instanceof Player) {
				playerCount++;
			}
		}

		// (1)列表中真实用户数量达到3人，随机一个机器人退出申请列表。优先退出排在玩家前面的机器人。
		// (2)列别中真实用户数量达到5人，随机二个机器人退出申请列表。优先退出玩家前面的机器人。
		int removeRobot = 0;
		if (playerCount >= 5) {
			removeRobot = 2;
		} else if (playerCount >= 3) {
			removeRobot = 1;
		}

		// 机器人退出上庄列表
		if (removeRobot >= 1) {
			for (int i = 0; i < list.size(); i++) {
				Role role = list.get(i);
				if (role instanceof Robot) {
					if (_table.exitBankerList(role)) {
						RobotBetsLogic robot = (RobotBetsLogic) role;
						robot.RobotSendDownBanker();

						// 重新加入下注和上庄列表
						bettingRobots.put(robot.uniqueId, robot);
						upBankerRobots.put(robot.uniqueId, robot);

						removeRobot--;
						if (removeRobot == 0) {
							break;
						}
					}
				}
			}
		}
	}

	// 辅助方法
	// 清除所有定时器
	// private Config.Error KillAllTimes() {
	// RemoveBettingSchedule();
	// RemoveUpBankerTimer();
	// return Config.ROB_ERR_SUCCESS;
	// }

	// 清除下注定时器
	private Config.Error RemoveBettingSchedule() {
		// 清除定时器
		if (_bettingScheduleList.size() > 0) {
			for (int i = 0; i < _bettingScheduleList.size(); i++) {
				if (_bettingScheduleList.get(i) != null) {
					_bettingScheduleList.get(i).stop();
				}
			}
		}
		// 清空
		_bettingScheduleList.clear();
		return Config.ROB_ERR_SUCCESS;
	}

	/** 清除上庄定时器 */
	private Config.Error RemoveUpBankerTimer() {
		if (_upBankerTimerList == null) {
			return Config.ROB_ERR_SUCCESS;
		}
		if (_upBankerTimerList.size() > 0) {
			for (int i = 0; i < _upBankerTimerList.size(); i++) {
				if (_upBankerTimerList.get(i) != null) {
					_upBankerTimerList.get(i).stop();
				}
			}
		}
		// 清空
		_upBankerTimerList.clear();
		return Config.ROB_ERR_SUCCESS;
	}

	// 数组概率获取 根据盘口限制
	private int GetArrayRandomByPro(int[] arrayData, long bettingScore) {
		int doorIndex = 0;
		int count = 0;
		for (int i = 0; i < arrayData.length; i++) {
			count += arrayData[i];
		}
		int rand = RandomUtil.ramdom(0, count - 1);
		int count2 = 0;
		for (int i = 0; i < arrayData.length; i++) {
			count2 += arrayData[i];
			if (rand < count2) {
				doorIndex = i;
				break;
			}
		}
		boolean haveIndex = false;
		int indexTemp = doorIndex;
		int whileCnt = 0;
		do {
			// 如果当前随机到的盘口不可以下注 下注盘口自动上移
			if (_doorScore.get(indexTemp) + bettingScore > robotConfig.GetMaxDoorScore()) {
				indexTemp--;
				if (indexTemp < 0) {
					indexTemp = (_gameData.doorCount - 1);
				}
				if (indexTemp == doorIndex) {
					indexTemp = -1;
					haveIndex = true;
				}
			} else {
				haveIndex = true;
			}
			whileCnt++;
		} while (!haveIndex && whileCnt < 100);

		if (whileCnt > 99) {
			log.error("游戏筹码设置出错.");
		}
		return indexTemp;
	}

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 */
	public void broadcast(BaseResponse msg) {

	}

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 * @param excludeUniqueId
	 *            不包含的玩家 uniqueId
	 */
	public void broadcast(BaseResponse msg, long excludeUniqueId) {

	}

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
	public void broadcast(BaseResponse self, BaseResponse other, long selfUniqueId) {

	}
}