package frame.game;

import frame.*;
import frame.Timer;
import frame.game.RobotActions.BetsRobot.RobotBetsManage;
import frame.game.RobotActions.FishRobot.RobotFishManage;
import frame.game.RobotActions.PkRobot.RobotPkLogic;
import frame.game.RobotActions.PkRobot.RobotPkManage;
import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.game.RobotActions.RobotFrame.RobotGameState;
import frame.game.RobotActions.RobotFrame.RobotManage;
import frame.game.RobotActions.RobotFrame.RobotManage.GameType;
import frame.game.TablePairManage.TablePair;
import frame.game.proto.Game.*;
import frame.game.proto.Game.GameUpdateMoney.Builder;
import frame.game.proto.GameBase;
import frame.game.proto.GameControl.GameLotteryControlReq;
import frame.socket.BaseResponse;
import frame.socket.ErrResponse;
import frame.socket.Request;
import frame.socket.Response;
import frame.socket.common.proto.Error.ErrorRes;
import frame.socket.common.proto.LobbySiteRoom.BetRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.FishRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.PkRoomCfg;
import frame.socket.common.proto.PlayerControl.UpdateControlPlayer;
import frame.socket.common.proto.Storage.StorageInfo;
import frame.socket.db.DBMessageCode;
import frame.socket.gate.proto.Login.KickNotify;
import frame.storageLogic.LotteryModel;
import frame.storageLogic.LotteryProtocol;
import frame.util.RandomUtil;
import frame.util.UUIDUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public abstract class Table extends Root implements BroadCastProtocol, LotteryProtocol {
	public enum Status {
		Open, Pair, Game, Begin, End
	}

	public class Configs {
		public int pairTime;
		public int robotTime;
		public int max;
	}

	protected Configs configs;
	public @Setter @Getter Status status = Status.Open;

	protected @Getter Map<Long, Role> roles = new HashMap<Long, Role>();
	// 准备的用户
	protected Map<Long, Role> readyRoles = new HashMap<Long, Role>();
	protected @Setter @Getter Room room;
	protected @Setter @Getter int id;

	public long endTime;
	public long startTime;
	public String roomName;

	public int maxRoles;
	public boolean robotForbid;
	public int robotMin;
	public int robotMax;
	private @Getter String gameUUID;
	private @Setter boolean needUpdateConfig;
	// 桌子准备好
	private boolean readyForStart;

	protected @Getter RobotManage robotManage;

	protected @Getter BetRoomCfg betRoomCfg;
	protected @Getter FishRoomCfg fishRoomCfg;
	protected @Getter PkRoomCfg pkRoomCfg;

	private @Getter TablePair tablePair;

	protected @Setter String debugString;
	/**
	 * 押注游戏 庄家
	 */
	private Banker banker;
	/**
	 * 是否申请下庄
	 */
	private @Getter @Setter boolean downBanker;
	/**
	 * 庄家列表
	 */
	private List<Role> bankerList;

	private @Getter @Setter boolean terminated = false;

	// 被单控玩家列表
	protected @Getter @Setter ArrayList<Player> controledPlayerList;

	// 游戏开奖控制
	protected @Getter @Setter GameLotteryControlReq gameLotteryControl;
	// 游戏开奖控制的玩家
	protected @Getter Player LotteryPlayer;
	// 下过注的玩家
	protected @Getter HashSet<Role> chipPlayer;
	// 同桌机器人
	public ArrayList<Long> sameTablePkRobot = new ArrayList<>();
	// 机器人循环任务, 每隔一定的时间检测机器人的状态
	private Task robotCheckTask;
	// 机器人的表情任务
	private @Setter @Getter Map<Long, Timer> robotExpressTimer = new HashMap<Long, Timer>();
	void init() {
		betRoomCfg = room.betRoomCfg;
		fishRoomCfg = room.fishRoomCfg;
		pkRoomCfg = room.pkRoomCfg;
		roomName = room.getRoomName();
		maxRoles = room.getRoomMaxPerson();
		chipPlayer = new HashSet<>();
		banker = new Banker();
		bankerList = new ArrayList<Role>();

		// 初始化配座
		tablePair = new TablePair(this);
		if (IsGameType(RobotBaseConfig.Type.One)) {
			robotManage = new RobotBetsManage(GameType.Bet, getRoom().gameConfig, this);
		} else if (IsGameType(RobotBaseConfig.Type.Range) || IsGameType(RobotBaseConfig.Type.Fix)) {
			log.info("RobotManage.GameType.Pair:{}, getRoom().gameConfig:{}", RobotManage.GameType.Pair, getRoom().gameConfig);
			robotManage = new RobotPkManage(RobotManage.GameType.Pair, getRoom().gameConfig, this);
		} else if (IsGameType(RobotBaseConfig.Type.Solo)) {
			robotManage = new RobotFishManage(RobotManage.GameType.fish, getRoom().gameConfig, this);
		}

		onUpdateConfig();
		onInit();
	}

	public void updateConfig() {
		betRoomCfg = room.betRoomCfg;
		fishRoomCfg = room.fishRoomCfg;
		pkRoomCfg = room.pkRoomCfg;
		roomName = room.getRoomName();
		maxRoles = room.getRoomMaxPerson();
		onUpdateConfig();
	}

	protected void onInit() {

	}

	/**
	 * 所有房间配置相关的初始化写到这里
	 */
	protected abstract void onUpdateConfig();

	public int getRoleSize() {
		return roles.size();
	}

	/**
	 * 申请上庄
	 * 
	 * @param role
	 */
	public boolean upBanker(Role role) {
		if(this.betRoomCfg.getSysBanker() == 2) {
			return false;
		}
		if (this.bankerList.contains(role)) {
			return false;
		}
		if (banker.getBanker().userId == role.userId) {
			return false;
		}
		this.bankerList.add(role);

		// 检查上庄列表
		if (role instanceof Player) {
			robotManage.checkBankerList();
		}

		return true;
	}

	/**
	 * 退出上庄队列
	 */
	public boolean exitBankerList(Role role) {
		if (!this.bankerList.contains(role)) {
			return false;
		}
		if (banker.getBanker().userId == role.userId) {
			return false;
		}
		this.bankerList.remove(role);

		// 检查上庄列表
		if (role instanceof Player) {
			robotManage.checkBankerList();
		}

		return true;
	}

	/**
	 * 获取上庄列表
	 * 
	 * @return
	 */
	public List<Role> getBankerList() {
		return this.bankerList;
	}

	/**
	 * 修改庄家
	 * 
	 * @return
	 */
	public void upDataBanker() {
		this.downBanker = false;
		// 机器人下庄
		if (this.banker.getIdentity() == Banker.ROBOT_BANKER) {
			robotManage.onChangeBanker(this.banker.getBanker());
		}

		// 如果没有人申请坐庄, 则更新为系统坐庄
		if (this.bankerList.size() == 0) {
			this.banker.updataBanker(SystemBanker.instance(), betRoomCfg.getBankerTime());
			return;
		}
		
		Role nextBanker = this.bankerList.remove(0);
		this.banker.updataBanker(nextBanker, betRoomCfg.getBankerTime());

		// 检查上庄列表
		robotManage.checkBankerList();
	}

	/**
	 * 获取庄家
	 * @return
	 */
	public Banker getNowBanker() {
		return this.banker;
	}

	/**
	 * 保存庄家输赢
	 */
	public void saveBankerWinLose(long money) {
		this.banker.saveWinLose(money);
	}

	/**
	 * 添加额外当庄局数
	 */
	public void addBankerMaxNum() {
		int num = betRoomCfg.getAddedTime();
		this.banker.addBankerMaxNum(num);
	}

	// 判断庄家是否为玩家
	public boolean checkBankerIsPlayer(){
		return Banker.PLAYER_BANKER == banker.getIdentity();
	}
	
	// 是否桌子满员了
	public Boolean isFull() {
		if (IsGameType(RobotBaseConfig.Type.One)) {
			return getRoleSize() >= getRoom().gameConfig.GetMaxTableRoleCount();
		} else {
			return getRoleSize() >= getRoom().gameConfig.GetMaxPlayer();
		}

	}

	public boolean isGaming() {
		return status == Status.Game || status == Status.Begin || status == Status.End;
	}

	// 标记桌子准备好开局
	public Config.Error setReadyForStart() {
		if (!isGamePlayerEnough()) {
			readyForStart = false;
			return Config.ERR_TABLE_PLAYER_NOT_ENOUGH;
		}
		// 停止配桌监听
		if (tablePair.noRobotStartTimer != null) {
			tablePair.noRobotStartTimer.stop();
		}
		readyForStart = true;
		return Config.ERR_SUCCESS;
	}

	public void error(Config.Error err) {
		log.error("桌子:{} ERROR:{} ", getRoom().getRoomTextInfo(), err.msg);
	}

	public void update() {
		if (readyForStart) {
			start();
			readyForStart = false;
		}
		if ((room.isForbid() || room.isClosing()) && canTerminate()) {
			doTerminate();
		}
		if (!room.isForbid() && terminated) {
			terminated = false;
		}
	}

	public boolean enter(Role role) {
		if (role.table != null) {
			role.exitTable();
		}
		roles.put(role.uniqueId, role);
		role.enterTable(this);
		onEnter(role);
		return true;
	}

	/**
	 * 是否需要游戏过程中销毁桌子
	 */
	protected boolean needPlayingShutdown() {
		return true;
	}

	Config.Error exit(Role role) {
		try {
			onExit(role);
			return Config.ERR_SUCCESS;
		} catch (Exception err) {
			log.error("下层游戏退出桌子错误:", err);
			return Config.ERR_SERVER_BUSY;
		} finally {
			roles.remove(role.uniqueId);
			role.table = null;
			// 当桌子没有真实玩家的时候 是否强行清理桌子
			if (needPlayingShutdown()) {
				autoShutdown();
			}
		}
	}

	void autoShutdown() {
		// 下注游戏桌子不销毁
		if (isBetGame()) {
			return;
		}
		if (roles.size() == 0) {
			shutdown();
			return;
		}
		if (status == Status.End) {
			if (getRealPlayerNum() == 0) {
				shutdown();
				return;
			}
		} else {
			if (needPlayingShutdown()) {
				if (getRealPlayerNum() == 0) {
					shutdown();
					return;
				}
			}
		}
	}

	// 获取真实玩家数
	public int getRealPlayerNum() {
		int realPlayerNum = 0;
		for (Role role : roles.values()) {
			if (role instanceof Player) {
				realPlayerNum++;
			}
		}
		return realPlayerNum;
	}

	public Map<Long, Role> getRealPlayer() {
		Map<Long, Role> realPlayer = new HashMap<Long, Role>();
		for (Role role : roles.values()) {
			if (role instanceof Player) {
				realPlayer.put(role.uniqueId, role);
			}
		}
		return realPlayer;
	}

	boolean canTerminate() {
		if (RobotBaseConfig.Type.Solo == getRoom().gameConfig.GetGameType()) {
			return true;
		} else {
			return status != Status.Begin;
		}
	}

	// 是否达到基本人数
	public boolean isGamePlayerEnough() {
		return getRoom().gameConfig.GetLeastGamingPlayerNum() <= roles.size();
	}

	// 设置准备玩家
	public boolean SetReadyPlayer(Role role) {
		if (roles.containsKey(role.uniqueId)) {
			readyRoles.put(role.uniqueId, role);
		} else {
			return false;
		}
		return true;
	}

	// 准备玩家是否满足开桌需求
	public boolean IsReadyPlayerEnough() {
		return getRoom().gameConfig.GetLeastGamingPlayerNum() <= readyRoles.size();
	}

	/**
	 * 统一结算接口(同步保存历史记录)
	 * 
	 * @param tax(废弃掉的)
	 * @param playerMoneys
	 *            每个玩家的结算信息
	 * @param recordDetail
	 *            游戏记录(字符串,发给后台处理)
	 */
	protected void result(long tax, List<PlayerMoney> playerMoneys, String recordDetail) {
		this.banker.updataMasterNum();
		result(tax, playerMoneys, recordDetail, true);
	}

	/**
	 * 统一结算接口
	 * 
	 * @param tax(废弃掉的)
	 * @param playerMoneys
	 *            每个玩家的结算信息
	 * @param recordDetail
	 *            游戏记录(字符串,发给后台处理)
	 * @param bSaveHistory
	 *            是否保存历史记录
	 */
	protected void result(long tax, List<PlayerMoney> playerMoneys, String recordDetail, boolean bSaveHistory) {
		// 体验房,数据库不保存记录和金币
		if (room.isFreeRoom()) {
			return;
		}
		if (playerMoneys.size() > 0) {
			saveMoney2DB(playerMoneys, recordDetail);
			if (bSaveHistory) {
				saveHistory2Record(playerMoneys, recordDetail);
			}
		}
		for (Role role : roles.values()) {
			role.updateMoney();
		}
	}

	private GameRecord.Builder getRecordBuilder() {
		GameRecord.Builder record = GameRecord.newBuilder();
		record.setStartime(startTime);
		record.setEndtime(GameMain.getInstance().getMillisecond());
		if (roomName == null) {
			roomName = "";
		}
		record.setRoomName(roomName);
		record.setGameID(gameUUID);
		record.setGameIndex(Config.GAME_ID);
		record.setRoomID(room.getId());
		record.setTableNumber(String.valueOf(id));
		return record;
	}

	// 保存历史记录
	private void saveMoney2DB(List<PlayerMoney> playerMoneys, String recordDetail) {
		ActivityConfig config = GameMain.getInstance().getHallMgr().getActiConfig();
		if (config != null) {
			int status = config.getStatus();
			log.info("站点:{},活动启用状态1：启用  0：禁用:{}", Config.SITE_ID, status);
			if (status == 1) {
				float percentage = config.getBetRate() / 100f;
				long startTime = config.getActivityBegin();
				long endTime = config.getActivityEnd();
				long nowTime = System.currentTimeMillis();
				log.info("活动开始时间：{},结束时间:{},现在时间:{}", startTime, endTime, nowTime);
				if (nowTime >= startTime && nowTime <= endTime) {
					for (int i = 0; i < playerMoneys.size(); i++) {
						long vailbet = playerMoneys.get(0).getValidBet();
						log.info("玩家:{},有效投注金额:{},积分转换比率:{}", playerMoneys.get(0).getUserid(), vailbet, percentage);
						long integral = (long) (vailbet * percentage);
						playerMoneys.add(playerMoneys.get(0).toBuilder().setIntegral(integral).build());
						playerMoneys.remove(0);
					}
				}
			}
		}

		GameRecord.Builder record = getRecordBuilder();
		Builder gameUpdateMoney = GameUpdateMoney.newBuilder();
		gameUpdateMoney.setGameRecord(record);
		for (PlayerMoney playerMoney : playerMoneys) {
			if (playerMoney.getUserid() != 0) {
				gameUpdateMoney.addPlayerMoney(playerMoney);
			}
		}
		// gameUpdateMoney.addAllPlayerMoney(playerMoneys);
		StorageInfo storageInfo = getStorageInfo();
		gameUpdateMoney.setStorageInfo(storageInfo);
		log.info("{} 同步结算数据: 库存1:{},库存2:{} ", room.getRoomTextInfo(), storageInfo.getStorage1(), storageInfo.getStorage2());
		for (Role role : roles.values()) {
			if (role instanceof Player) {
				Player player = (Player) role;
				UpdateControlPlayer.Builder controlPlayerBuilder = UpdateControlPlayer.newBuilder();
				controlPlayerBuilder.setLeftWinLose((int) player.getLeftControlMoney());
				controlPlayerBuilder.setUserId(player.userId);
				gameUpdateMoney.addControlPlayer(controlPlayerBuilder.build());
			}
		}
		for (PlayerMoney mm : playerMoneys) {
			log.info("结算玩家:{} 变动金币:{}", mm.getUserid(), mm.getDeltaMoney());
		}
		GameMain.getInstance().send2DB(new Response(DBMessageCode.GameRecord2DB, gameUpdateMoney.build().toByteArray()), room.getHall().getHallId(),
				0, new Callback() {
					@Override
					public void func() {
						Request dbReq = (Request) this.getData();
						if (dbReq.isError()) {
							for (PlayerMoney mm : playerMoneys) {
								log.info("结算数据丢失   " + mm.getUserid() + "  数量 " + mm.getDeltaMoney());
							}
							if (dbReq.isTimeout()) {
								log.error("同步DB数据超时");
							} else {
								try {
									ErrorRes errorRes = ErrorRes.parseFrom(dbReq.protoMsg);
									log.error("同步DB数据失败:{}", errorRes.getMsg());
								} catch (Exception e) {
									log.error("error :", e);
								}
							}
							return;
						}
						try {
							UpdateMoneyNotify updateMoneyNotify = UpdateMoneyNotify.parseFrom(dbReq.protoMsg);
							syncPlayerDataFromDB(updateMoneyNotify);
						} catch (Exception err) {
							log.error("同步DB数据错误:", err);
						}
					}
				});
	}

	// 保存历史记录
	protected void saveHistory2Record(List<PlayerMoney> playerMoneys, String recordDetail) {
		// 体验房,不保存游戏记录
		if (room.isFreeRoom()) {
			return;
		}
		GameRecord.Builder record = getRecordBuilder();
		CMDRecord.Builder cmd = CMDRecord.newBuilder();
		cmd.setInfo(record);
		cmd.setDetail(recordDetail);
		cmd.addAllPMoney(playerMoneys);
		GameMain.getInstance().send2Record(new Response(DBMessageCode.GameRecord2CMD, cmd.build().toByteArray()), room.getHall().getHallId(), 0);
		log.info("发送投注纪录到后台  局号 :{}",this.gameUUID);
	}

	/**
	 * 从DB同步玩家数据
	 */
	private void syncPlayerDataFromDB(UpdateMoneyNotify updateMoneyNotify) {
		int siteId = this.room.getHall().getHallId();
		for (PlayerMoneyRet playerMoneyBet : updateMoneyNotify.getPlayerList()) {
			Player player = GameMain.getInstance().getRoleMgr().getPlayer(siteId, playerMoneyBet.getUserid());
			if (player != null) {
				if (playerMoneyBet.getOnLine() == 0) {
					log.info("syncPlayerDataFromDB 玩家:{} 在redis 没有数据,被强踢", player.getPlayerInfo());
					KickNotify kicknotify = KickNotify.newBuilder().setReason(KickNotify.KickNotifyReason.KICKNOTIFY_REASON_NOT_EXIST).build();
					player.send(new Response(FrameMsg.KICK_OUT_PLAYER, kicknotify.toByteArray()));
				}
				player.syncMoneyFromDB(playerMoneyBet.getMoney());
			}
		}
	}

	// 获取库存信息
	private StorageInfo getStorageInfo() {
		StorageInfo.Builder storageInfoBuilder = StorageInfo.newBuilder().setGameID(Config.GAME_ID).setRoomID(room.getId());
		if (IsGameType(RobotBaseConfig.Type.Solo)) {
			// 小鱼大鱼库存
			storageInfoBuilder.setStorage1(room.getFishStorageMgr().getSmallFishStorage());
			storageInfoBuilder.setStorage2(room.getFishStorageMgr().getBigFishStorage());
			if (room.getFishStorageMgr().getConfig().isOpenStorageCtrl) {
				storageInfoBuilder.setTotalStorageReduce(room.getFishStorageMgr().getTotalStorageReduce());
				room.getFishStorageMgr().setTotalStorageReduce(0);
			}
		} else {
			storageInfoBuilder.setStorage1(room.getStorageMgr().getStorageCurrent());
			storageInfoBuilder.setTotalStorageReduce(room.getStorageMgr().getTotalStorageReduce());
			room.getStorageMgr().setTotalStorageReduce(0);
		}
		return storageInfoBuilder.build();
	}

	// 桌子准备完毕
	protected void start() {
		// 对战类游戏开始停止配座 设置同桌玩家
		if (IsGameType(RobotBaseConfig.Type.Range) || IsGameType(RobotBaseConfig.Type.Fix)) {
			robotManage.OnTabelPairEnd();
			log.info("桌子配对结束!!!");
			// 设置同桌玩家, 就是把真实的玩家放到一个 ArrayList
			for (Long key : roles.keySet()) {
				if (roles.get(key) instanceof Player) {
					roles.get(key).setSameTablePlayer(roles);
				}
			}
		}
		// 设置机器人管理状态
		if (!IsGameType(RobotBaseConfig.Type.One)) {
			robotManage.OnStart(RobotGameState.GAMESTATE_BEGIN, null);
		}
		status = Status.Game;
		onStart();
	}

	private boolean canBegin() {
		return GameMain.getInstance().isRunning();
	}

	// 游戏开始
	protected boolean begin() {
		log.info("Table:{} 游戏开始", this.id);
		if (needUpdateConfig) {
			updateConfig();
			needUpdateConfig = false;
		}

		if (!canBegin()) {
			return false;
		}
		status = Status.Game;
		gameUUID = UUIDUtils.getUUID();
		startTime = GameMain.getInstance().getMillisecond();
		status = Status.Begin;
		if (!IsGameType(RobotBaseConfig.Type.One)) {
			Config.Error err = robotManage.OnGameBegin(RobotGameState.GAMESTATE_BEGIN);
			if (err != Config.ERR_SUCCESS) {
				return false;
			}
		}
		controledPlayerList = null;
		gameLotteryControl = null;
		LotteryPlayer = null;
		for (Role role : roles.values()) {
			role.doGameBegin();
		}
		return true;
	}

	// 游戏结束
	protected void end() {
		// 设置游戏状态为结束
		status = Status.End;

		if (!IsGameType(RobotBaseConfig.Type.One)) {
			robotManage.OnGameEnd(RobotGameState.GAMESTATE_END);
		}
		// 是否可以留桌 改变游戏状态
		if (getRoom().gameConfig.IsNoChangeTable() && isGamePlayerEnough()) {
			// 如果可以留桌桌子状态改为 刚开启状态
			status = Status.Pair;
		}

		endTime = GameMain.getInstance().getMillisecond();
		for (Role role : roles.values()) {
			role.doGameEnd();
		}
		// 自动清理掉线玩家 如果服务器关闭 则清理所有玩家

		autoKick(!canBegin());
	}

	protected void shutdown() {
		doTerminate();
	}

	// 准备销毁 销毁房间时才会销毁
	protected void destroy() {
		setDestroy(true);
		robotManage.destroy();
		onDestroy();
	}

	public HashMap<Long, Robot> getAllRobot() {
		HashMap<Long, Robot> robots = new HashMap<>();
		for (Role role : roles.values()) {
			if (role instanceof Robot) {
				Robot robot = (Robot) role;
				robots.put(robot.uniqueId, robot);
			}
		}
		return robots;
	}

	private void autoKick(boolean clear) {
		HashSet<Role> set = new HashSet<>();
		for (Role role : roles.values()) {
			if (role instanceof Robot) {
				if (clear) {
					set.add(role);
				}
			} else {
				// 离线状态的真实玩家清理
				if (!((Player) role).isOnline()) {
					set.add(role);
				}
			}
		}
		for (Role role : set) {
			Player player = (Player) role;
			if (player.isResultKickOut()) {
				Config.Error err = role.exitHall();
				if (err == Config.ERR_SUCCESS) {
					GameMain.getInstance().notifyPlayerLogOut(player.uniqueId);
					log.info("游戏结束踢出掉线玩家:{} , 通知DB踢出玩家", player.getPlayerInfo());
				} else {
					log.error("游戏结束踢出掉线玩家 {} 失败, 失败原因:{} ", player.getPlayerInfo(), err.msg);
				}
			}
		}
	}

	void doStop() {
		onStop();
	}

	void doTerminate() {
		if (terminated) {
			return;
		}
		terminated = true;
		Set<Role> set = new HashSet<Role>();
		set.addAll(roles.values());
		for (Role role : set) {
			if (room.isForbid() || room.isClosing()) {
				// 桌子被主动禁用或关闭,要给桌子里的玩家发提示消息
				role.send(new ErrResponse(Config.ERR_ROOM_CLOSE));
			}
			onExit(role);
			role.exitRoom();
		}
		roles.clear();
		try {
			if (this.getRoom().gameConfig.GetGameType() == RobotBaseConfig.Type.Fix
					|| this.getRoom().gameConfig.GetGameType() == RobotBaseConfig.Type.Range) {
				if (robotManage.GetPairTimer() != null) {
					robotManage.GetPairTimer().stop();
				}
			}

		} catch (Exception e) {
			log.error("删除匹配计时器：{}", e);
		}
		// 下注游戏不销毁桌子
		if (!IsGameType(RobotBaseConfig.Type.One)) {
			setDestroy(true);
		}
		onTerminate();
	}

	void doDestroy() {
		destroy();
		onDestroy();
	}

	// 判断游戏类型
	public boolean IsGameType(RobotBaseConfig.Type type) {
		return room.IsGameType(type);
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

	public int getBetPlayer() {
		int playerNum = 0;
		for (Role role : chipPlayer) {
			if (role instanceof Player) {
				playerNum++;
			}
		}
		return playerNum;
	}

	public boolean isBetPlayerNotJoining() {
		if (!isBetGame()) {
			return false;
		}
		if (getBetPlayer() == 0 && !checkBankerIsPlayer()) {
			return true;
		} else {
			return false;
		}
	}

	// 是否可以加钱
	public boolean canChargeMoneyAtOnce() {
		if (status == Status.Begin) {
			return false;
		}
		return true;
	}

	// 配桌失败 清除桌子
	public void pairFailShutdown() {
		if (getStatus() == Status.Pair) {
			shutdown();
		}
	}

	/**
	 * 桌子玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 */
	public void broadcast(BaseResponse msg) {
		for (Role r : roles.values()) {
			r.send(msg);
		}
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
		for (Role r : roles.values()) {
			if (r != null) {
				if (excludeUniqueId != r.uniqueId) {
					r.send(msg);
				}
			}
		}
	}

	/**
	 * 桌子玩家广播
	 * 
	 * @param self
	 *            发给自己的消息
	 * @param other
	 *            发给桌子其他玩家的消息
	 * @param selfUniqueId
	 *            自己的uniqueId
	 */
	public void broadcast(BaseResponse self, BaseResponse other, long selfUniqueId) {
		for (Role r : roles.values()) {
			if (r != null) {
				if (selfUniqueId == r.uniqueId) {
					r.send(self);
				} else {
					r.send(other);
				}
			}
		}
	}

	/**
	 * 获取桌子所有单控玩家
	 */
	public ArrayList<Player> getSystemControlPlayerList() {
		ArrayList<Player> controlPlayers = new ArrayList<Player>();
		for (Role role : roles.values()) {
			if (role instanceof Player) {
				Player player = (Player) role;
				if (player.isSystemControl()) {
					controlPlayers.add(player);
				}
			}
		}
		return controlPlayers;
	}

	/**
	 * 获取桌子的单控玩家(找出单控数据绝对值最大的玩家,只找一个)
	 * 
	 */
	public Player getSystemControlPlayer() {
		ArrayList<Player> controlPlayers = getSystemControlPlayerList();
		ArrayList<Player> betControlPlayers = new ArrayList<>();
		ArrayList<Player> sortPlayers = controlPlayers;
		if (controlPlayers.size() == 0) {
			return null;
		} else {
			if (this.isBetGame()) {// 如果是下注类游戏就判断单控玩家是否有下注
				for(Player player : controlPlayers) {
					if(player.userId == this.banker.getBanker().userId) {
						Player bank = (Player) this.banker.getBanker();
						betControlPlayers.add(bank);
						break;
					}
				}
				if (chipPlayer.size() > 0) {
					for (Role chip : chipPlayer) {
						for (Player control : controlPlayers) {
							if (chip == control) {
								betControlPlayers.add(control);
							}
						}
					}
				}
				if (betControlPlayers.size() <= 0) {
					return null;
				}
				sortPlayers = betControlPlayers;
			}
			// 排序(单控绝对值从高到低,绝对值相等，负数优先)
			Collections.sort(sortPlayers, new Comparator<Player>() {
				public int compare(Player lhs, Player rhs) {
					if (Math.abs(lhs.getLeftControlMoney()) != Math.abs(rhs.getLeftControlMoney())) {
						return Math.abs(lhs.getLeftControlMoney()) > Math.abs(rhs.getLeftControlMoney()) ? -1 : 1;
					} else {
						return lhs.getLeftControlMoney() > rhs.getLeftControlMoney() ? 1 : -1;
					}
				}
			});
			return sortPlayers.get(0);
		}
	}

	// 如果是机器人被攻击则触发机器人反击的逻辑
	void robotStrikeBack(GameBase.EmotionReq emoTionReq) {
		if (roles.size() != 0 && roles.containsKey(emoTionReq.getToPlayer())) {
			if (roles.get(emoTionReq.getToPlayer()) instanceof Robot) {
				Robot tempRobot = (Robot) roles.get(emoTionReq.getToPlayer());
				tempRobot.receiveExpression(emoTionReq);
			}
		}
	}

	/**
	 * 开始机器人魔法表情循环任务, 默认游戏开始等待时间是7秒, 随机发送表情最长的等待时间是10秒
	 */
	public void robotAttactRoles(){
		// 参数的意思是: 游戏开始7秒钟后, 机器人魔法表情开始攻击和反击
		// 机器人人随机发送表情的最长等待时间是 10 秒
		this.robotAttactRoles(7, 15);
	}

	// 游戏开始的时候机器人攻击在桌子中的玩家, 包括机器人
	public void robotAttactRoles(int wait, int maxWaitTime) {
		// 确定到底有几个机器人执行以上的攻击判断0-机器人数量中随机。
		log.info("==begain==机器人在游戏开始==begain==");
		// 设置同桌机器人, 就是把机器人放到一个 ArrayList
		log.info("roles的大小是:{}", roles.size());
		for (Long key : roles.keySet()) {
			// 只有对战类的游戏有魔法表情攻击
			if (roles.get(key) instanceof RobotPkLogic) {
				sameTablePkRobot.add(key);
			}
		}
		log.info("同桌机器人数量:sameTablePkRobot:{}", sameTablePkRobot.size());
		int RototNum = sameTablePkRobot.size();
		int randomRototNum = RandomUtil.ramdom(1000) % (RototNum + 1);
		log.info("随机主动发动攻击的机器人数量是: {}", randomRototNum);

		// 随机出 randomRototNum 个机器人放到链表里面, 用于指派攻击的任务数量
		ArrayList<Long> tmpRobots = selectRobots(sameTablePkRobot, randomRototNum);
		log.info(tmpRobots.toString());
		if (randomRototNum != 0) {
			for (Long i : tmpRobots) {
				Role attactRobot = roles.get(i);
				// 给机器人指定 攻击的次
				((RobotPkLogic) attactRobot).getExpression().receiveTask();
			}
		}
		// 开一个定时器用于监控机器人状态, 在冷却时间内不能发送表情
		robotSchedule(wait, maxWaitTime);
	}

	/**
	 * 机器人魔法表情循环任务
     * @param wait 任务开启时的初始等待的时间, 这个时间用于和客户端的游戏开始时播放
     *             动画的时间匹配, 防止服务器的机器人逻辑开始了, 客户端还在播放游戏动画时发送机器人表情
     * @param maxWaitTime 机器人魔法表情触发的时间是随机的, 这个时间用于控制机器人发送表情的最长等待的时间
     *                    机器人魔法表情发送的时间控制在:  表情冷却的时间 < time < 最长等待的时间
     *                    表情冷却时间都是
	 */
	void robotSchedule(int wait, int maxWaitTime) {
		// 这个循环任务不断的检查机器人列表中各个的状态, 在cd 时间之内不发动攻击,
		// 若被攻击优先执行反击逻辑
		float interval = (float) 0.5; // 每隔 0.5 秒检查一个机器人的状态
		long gameStartTime = UtilsMgr.getMillisecond();
		robotCheckTask = UtilsMgr.getTaskMgr().createSchedule(new Callback() {
			@Override
			public void func() {
				// 1. 如果游戏结束则机器人定时任务结束
				// 如果桌子不存在机器人不攻击
				if (Status.End == status) {
					clearTask();
					robotCheckTask.stop();
					return;
				}
				// 配合客户端播放动画的时间
				// waitTime 之内不触发机器人的逻辑
				int waitTime = wait;
				if (UtilsMgr.getMillisecond() - gameStartTime < waitTime * 1000) {
					return;
				}

				// 循环定时器,用于检测机器人的状态
				// 如果机器人有攻击的任务则执行攻击逻辑
				// 如果机器人被攻击则优先执行反击的逻辑
				for (Long i : sameTablePkRobot) {
					Role robot = roles.get(i);
					if (robot instanceof RobotPkLogic) {
						((RobotPkLogic) robot).getExpression().robotTask(maxWaitTime);
					}
				}
			}
		}, interval);
	}

	/** 停止机器人魔法表情逻辑 */
	public void stopRobotSchedule(){
	    clearTask();
	    if(robotCheckTask != null){
	        robotCheckTask.stop();
        }
    }

	// 清理机器表情攻击的任务
	public void clearTask(){
		for(Long k: robotExpressTimer.keySet()){
			Timer tmpTimer = robotExpressTimer.get(k);
			tmpTimer.stop();
		}
	}

	// 从robotList 中 随机 选出 num 个元素 组成新的数组并返回
	ArrayList<Long> selectRobots(ArrayList<Long> robotList, int num) {
		ArrayList<Long> tmp = new ArrayList<Long>(robotList);
		ArrayList<Long> result = new ArrayList<Long>();

		while (tmp.size() >= 1 && num > 0) {
			// 在剩余的
			int index = RandomUtil.ramdom(100) % (tmp.size());
			long id = tmp.get(index);
			result.add(id);
			tmp.remove(index);
			num--;
		}
		tmp.clear();
		return result;
	};

	public abstract ArrayList<LotteryModel> getAllLotteryModel(ArrayList<Player> systemControlPlayerList);

	/**
	 * 控制开奖
	 * 
	 * @param player
	 */
	public void controllLottery(Player player, GameLotteryControlReq gameLotteryControl) {
		this.gameLotteryControl = gameLotteryControl;
		this.LotteryPlayer = player;
		onControllLottery(player, gameLotteryControl);
	}

	public void onControllLottery(Player player, GameLotteryControlReq gameLotteryControl) {

	}

	// 房间配置更新
	public void onRoomConfigUpdate() {

	}

	public void onBanRoom() {

	}

	public void pairFail() {
		onPairFail();
		// 配桌失败玩家都踢出桌子
		ArrayList<Role> allRoles = new ArrayList<Role>();
		for (Role role : roles.values()) {
			if (role != null) {
				allRoles.add(role);
			}
		}
		for (Role role : allRoles) {
			exit(role);
		}
	}

	// 配桌失败
	public abstract void onPairFail();

	protected abstract void onStop();

	protected abstract void onTerminate();

	protected abstract void onEnter(Role role);

	protected abstract void onExit(Role role);

	protected abstract void onStart();

	protected abstract void onDestroy();
}