package frame.game;

import frame.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayDeque;
import java.util.HashMap;
import frame.log;
import frame.game.RobotActions.BetsRobot.RobotBetsConfig;
import frame.game.RobotActions.FishRobot.RobotFishConfig;
import frame.game.RobotActions.PkRobot.RobotPkConfig;
import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.storageLogic.*;
import frame.util.RandomUtil;
import frame.socket.common.proto.LobbySiteRoom.BetRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.FishRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.PkRoomCfg;
import frame.socket.common.proto.Storage.StorageConfig;

public class Room {
	private @Setter @Getter GameHall hall;

	public enum PairStatus {
		Success, Failed
	}

	private @Getter TableMgr tableMgr;
	private @Getter HashMap<Long, Role> roles = new HashMap<>();
	private @Getter ArrayDeque<Robot> waitRobots = new ArrayDeque<>(100);

	protected @Getter BetRoomCfg betRoomCfg;
	protected @Getter FishRoomCfg fishRoomCfg;
	protected @Getter PkRoomCfg pkRoomCfg;

	protected @Getter @Setter int id;

	protected @Getter StorageMgr storageMgr;
	protected @Getter FishStorageMgr fishStorageMgr;

	private @Getter @Setter boolean closing = false;

	public RobotBaseConfig gameConfig;

	private int maxPlayer = 0;

	/** 当前房间中真实玩家的数量 */
	public @Getter int curPlayer = 0;

	Room() {
		tableMgr = new TableMgr();
		// 捕鱼库存和其他游戏不走一起
		if (RobotBaseConfig.Type.Solo == GameMain.getInstance().getGameMgr().robotConfig.GetGameType()) {
			fishStorageMgr = new FishStorageMgr(this);
		} else {
			storageMgr = new StorageMgr(this);
		}
		initRobotConfig();
		tableMgr.setRoom(this);
	}

	public void init() {
		prepareTable();
	}

	void updateCfg(BetRoomCfg cfg) {
		betRoomCfg = cfg;
		tableMgr.updateConfig();
		if (cfg.hasStorageConfig()) {
			updateStorageConfig(cfg.getStorageConfig());
		}
		maxPlayer = betRoomCfg.getRoomPersons();
		gameConfig.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
		log.info("{} 更新房间配置成功", getRoomTextInfo());
	}

	void updateCfg(FishRoomCfg cfg) {
		fishRoomCfg = cfg;
		tableMgr.updateConfig();
		if (cfg.hasStorageConfig()) {
			updateStorageConfig(cfg.getStorageConfig());
		}
		maxPlayer = fishRoomCfg.getRoomPersons();
		gameConfig.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
		fishStorageMgr.loadCfg(fishRoomCfg.getRoomField());
		log.info("{} 更新房间配置成功", getRoomTextInfo());
	}

	void updateCfg(PkRoomCfg cfg) {
		pkRoomCfg = cfg;
		tableMgr.updateConfig();
		if (cfg.hasStorageConfig()) {
			updateStorageConfig(cfg.getStorageConfig());
		}
		maxPlayer = pkRoomCfg.getRoomPersons();
		gameConfig.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
		log.info("{} 更新房间配置成功", getRoomTextInfo());
	}

	void updateStorageConfig(StorageConfig storageConfig) {
		if (storageMgr != null) {
			storageMgr.updateStorageConfig(storageConfig);
		} else if (fishStorageMgr != null) {
			fishStorageMgr.updateStorageConfig(storageConfig);
		}
	}

	void updateStorageReduce(int storageReduce) {
		if (storageMgr != null) {
			storageMgr.updateStorageReduce(storageReduce);
		} else if (fishStorageMgr != null) {
			fishStorageMgr.updateStorageReduce(storageReduce);
		}
	}

	void updateCurrrentStorage(long currentStorage1, long currentStorage2) {
		if (storageMgr != null) {
			storageMgr.updateCurrentStorage(currentStorage1);
		} else if (fishStorageMgr != null) {
			fishStorageMgr.updateCurrentStorage(currentStorage1, currentStorage2);
		}
	}

	// 是否是体验房
	public boolean isFreeRoom() {
		if (betRoomCfg != null) {
			return betRoomCfg.getRoomType() == 1;
		} else if (fishRoomCfg != null) {
			return fishRoomCfg.getRoomType() == 1;
		} else if (pkRoomCfg != null) {
			return pkRoomCfg.getRoomType() == 1;
		}
		return false;
	}

	public String getRoomName() {
		String roomName = "";
		if (betRoomCfg != null) {
			roomName = betRoomCfg.getRoomName();
		}
		if (fishRoomCfg != null) {
			roomName = fishRoomCfg.getRoomName();
		}
		if (pkRoomCfg != null) {
			roomName = pkRoomCfg.getRoomName();
		}
		return roomName;
	}

	public int getRoomMaxPerson() {
		int maxRoles = 0;
		if (betRoomCfg != null) {
			maxRoles = betRoomCfg.getRoomPersons();
		}
		if (fishRoomCfg != null) {
			maxRoles = fishRoomCfg.getRoomPersons();
		}
		if (pkRoomCfg != null) {
			maxRoles = pkRoomCfg.getRoomPersons();
		}
		return maxRoles;
	}

	// 获取机器人携带金钱
	public long getRobotScore() {
		long max = 0;
		long min = 0;
		if (betRoomCfg != null) {
			min = betRoomCfg.getRobotGold1();
			max = betRoomCfg.getRobotGold2();
		} else if (fishRoomCfg != null) {
			min = fishRoomCfg.getRobotGold1();
			max = fishRoomCfg.getRobotGold2();
		} else if (pkRoomCfg != null) {
			min = pkRoomCfg.getRobotGold1();
			max = pkRoomCfg.getRobotGold2();
		}
		if (min == max) {
			return min;
		} else {
			return RandomUtil.ramdom(min, max);
		}
	}

	void forbid(int bForbid) {
		if (betRoomCfg != null) {
			betRoomCfg = betRoomCfg.toBuilder().setForbid(bForbid).build();
		}
		if (fishRoomCfg != null) {
			fishRoomCfg = fishRoomCfg.toBuilder().setForbid(bForbid).build();
		}
		if (pkRoomCfg != null) {
			pkRoomCfg = pkRoomCfg.toBuilder().setForbid(bForbid).build();
		}
		if (bForbid == 1) {
			log.info("{} 开启成功", getRoomTextInfo());
		} else if (bForbid == 2) {
			log.info("{} 禁用成功", getRoomTextInfo());
			banRoom();
		}
	}

	// 是否被禁用 1正常 2关闭
	public boolean isForbid() {
		if (betRoomCfg != null) {
			return betRoomCfg.getForbid() == 2;
		}
		if (fishRoomCfg != null) {
			return fishRoomCfg.getForbid() == 2;
		}
		if (pkRoomCfg != null) {
			return pkRoomCfg.getForbid() == 2;
		}
		return true;
	}

	/**
	 * 禁用房间
	 */
	private void banRoom() {
		this.tableMgr.getAllTables().values().forEach(table -> {
			table.onBanRoom();
		});
	}

	// 是否满人
	public boolean isRoomFull(boolean isRobot) {
		if (isRobot) {
			return false;
		} else {
			return curPlayer >= maxPlayer;
		}
	}

	void prepareTable() {
		// 房间创建后是否直接创建桌子
		if (GameMain.getInstance().getGameMgr().getRobotConfig().GetIsNeedPrepareTable()) {
			UtilsMgr.getTaskMgr().createTrigger(new Callback() {
				@Override
				public void func() {
					tableMgr.createTable();
				}
			}).fire();

			float time = (float) (Math.random() * Config.ROOM_CREATE_TIME);
			log.info("随机时间:{}", time);
			UtilsMgr.getTaskMgr().createTimer((time), new Callback() {
				@Override
				public void func() {
					tableMgr.getWait(null).start();
				}
			});
		}
	}

	boolean canTerminate() {
		return tableMgr.canTerminate();
	}

	Config.Error enter(Role role) {
		if (role.room != null) {
			role.exitRoom();
		}
		if (role instanceof Player) {
			log.info("玩家进入,房间人数:{},真实玩家人数:{}", roles.size(), curPlayer);
		}
		boolean isRobot = role instanceof Robot;
		if (isRoomFull(isRobot)) {
			return Config.ERR_ROOM_FULL;
		}
		if (!isRobot) {
			curPlayer++;
		}
		roles.put(role.uniqueId, role);
		role.updateMoney();
		onEnter(role);

		return Config.ERR_SUCCESS;
	}

	public Config.Error exit(Role role) {
		try {
			onExit(role);
			return Config.ERR_SUCCESS;
		} catch (Exception e) {
			log.error("下层游戏退出房间错误:", e);
			return Config.ERR_SERVER_BUSY;
		} finally {
			roles.remove(role.uniqueId);
			if (role instanceof Robot) {
				waitRobots.addLast((Robot) role);
			} else {
				role.enterHall(role.hall);
				curPlayer--;
			}
			if (role instanceof Player) {
				log.info("玩家退出,房间人数:{},真实玩家人数:{}", roles.size(), curPlayer);
			}
		}
	}

	public Config.Error pair(Role role, Table table) {
		if (table == null) {
			table = tableMgr.getWait(role);
		}
		return table.getTablePair().Pair(role);
	}

	public Robot genOneRobot() {
		return genOneRobot(null);
	}

	// 生成一个机器人
	public Robot genOneRobot(Table table) {
		// test by zy 2019.3.23
		Robot robot = waitRobots.pollLast();
		if (robot == null) {
			robot = GameMain.getInstance().getRoleMgr().createRobot();
		}
		if (isFreeRoom()) {
			robot.money = 1000000000;
		} else {
			robot.money = gameConfig.GetCarryScore();
		}
		return robot;
	}

	// 初始化机器人配置
	public void initRobotConfig() {
		int maxPlayer = GameMain.getInstance().getGameMgr().robotConfig.GetMaxPlayer();
		int minPlayer = GameMain.getInstance().getGameMgr().robotConfig.GetMinPlayer();
		RobotBaseConfig.Type type = GameMain.getInstance().getGameMgr().robotConfig.GetGameType();
		if (RobotBaseConfig.Type.Solo == type) {
			gameConfig = new RobotFishConfig(type, minPlayer, maxPlayer);
		} else if (type == RobotBaseConfig.Type.Range || type == RobotBaseConfig.Type.Fix) {
			gameConfig = new RobotPkConfig(type, minPlayer, maxPlayer);

		} else if (type == RobotBaseConfig.Type.One) {
			gameConfig = new RobotBetsConfig(type, minPlayer, maxPlayer);
		}
	}

	// 判断游戏类型
	public boolean IsGameType(RobotBaseConfig.Type type) {
		return GameMain.getInstance().getGameMgr().isGameType(type);
	}

	// 捕鱼游戏
	public boolean isFishGame() {
		return IsGameType(RobotBaseConfig.Type.Solo);
	}

	// 对战游戏
	public boolean isPkGame() {
		return IsGameType(RobotBaseConfig.Type.Fix) || IsGameType(RobotBaseConfig.Type.Range);
	}

	// 下注游戏
	public boolean isBetGame() {
		return IsGameType(RobotBaseConfig.Type.One);
	}

	/**
	 * 房间信息
	 */
	public String getRoomTextInfo() {
		return String.format("%s【%s】:%s", hall.getHallTextInfo(), getRoomName(), id);
	}

	public void update() {
		tableMgr.update();
	}

	protected void onEnter(Role role) {

	}

	protected void onExit(Role role) {

	}

	void doStop() {
		tableMgr.doStop();
	}

	void doTerminate() {
		tableMgr.doTerminate();
	}

	void doDestroy() {
		tableMgr.doDestroy();
	}
}