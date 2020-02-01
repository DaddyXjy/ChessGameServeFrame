package frame.game;

import frame.*;
import frame.game.RobotActions.PkRobot.RobotPkLogic;
import frame.socket.*;
import lombok.Getter;
import java.util.Map;

public abstract class Role extends Root implements Comparable<Role>, MsgDealProtocol {
	/**
	 * 厅主id
	 */
	public int siteId;

	/**
	 * 用户id
	 */
	public int userId;

	/**
	 * 性别
	 */
	public int gender;

	/**
	 * 昵称
	 */
	public String nickName;

	/**
	 * 用户头像
	 */
	public String portrait;

	/**
	 * 微信头像
	 */
	public String wxPortrait;

	/**
	 * 用户余额
	 */
	public long money;

	/**
	 * 用户token
	 */
	public String token;
	/**
	 * 用户的唯一ID
	 */
	public long uniqueId;
	/**
	 * IP地址 by zy
	 */
	public String IP;
	/**
	 * 在桌子中的时间
	 */
	public long tableBeginLifeTime = 0;

	protected boolean inited;

	private Schedule tableLifeTimeSchedule;

	public boolean getInited() {
		return inited;
	}

	protected @Getter GameHall hall;
	protected @Getter Room room;
	protected @Getter Table table;

	public void init() {
		onInit();
		inited = true;
	}

	protected void onInit() {

	}

	public void enterTable(Table table) {
		this.table = table;
		this.tableBeginLifeTime = System.currentTimeMillis();
		onEnterTable();
	}

	public long getTableLifeTime() {
		if (this.table == null) {
			return 0;
		} else {
			return System.currentTimeMillis() - this.tableBeginLifeTime;
		}
	}

	public void enterHall(GameHall hall) {
		hall.enter(this);
		this.hall = hall;
		onEnterHall();
	}

	public Config.Error enterRoom() {
		if (table == null) {
			return Config.ERR_ROOM_NOT_EXIST;
		}
		return Config.ERR_SUCCESS;
	}

	public Config.Error enterRoom(int id) {
		if (this.hall != null) {
			Room room = hall.getRoomMgr().getRooms().get(id);
			if (room != null) {
				if (room.isForbid()) {
					return Config.ERR_ROOM_FORBID;
				} else {
					return enterRoom(room);
				}
			}
		}
		return Config.ERR_ROOM_NOT_EXIST;
	}

	public Config.Error enterRoom(Room room, Table table) {
		Config.Error err;
		do {
			if (room == null) {
				err = Config.ERR_ROOM_NOT_EXIST;
				break;
			}
			if (GameMain.getInstance().getStatus() != GameMain.Status.RUN) {
				err = Config.ERR_STOP;
				break;
			}
			// 对战和捕鱼进入金钱判断在这里检查
			if (room.isPkGame() || room.isFishGame()) {
				if (!room.isFreeRoom()) {
					if (room.getPkRoomCfg() != null && room.getPkRoomCfg().getMinMoney() > money) {
						err = Config.ERR_MONEY_NOT_ENOUGH;
						break;
					}
					if (room.getFishRoomCfg() != null && room.getFishRoomCfg().getMinMoney() > money) {
						err = Config.ERR_MONEY_NOT_ENOUGH;
						break;
					}
				}
			}

			err = room.enter(this);
			if (err == Config.ERR_SUCCESS) {
				this.room = room;
				convert2RoomMoney();
				onEnterRoom();
				// 下注类游戏进入房间就会开始匹配
				if (GameMain.getInstance().getGameMgr().getRobotConfig().GetIsEnterRoomPair()) {
					err = room.pair(this, table);
					if (err == Config.ERR_SUCCESS) {
						if (hall != null) {
							hall.exit(this);
						}
						return Config.ERR_SUCCESS;
					}
					break;
				}

				return Config.ERR_SUCCESS;
			}
		} while (false);

		error(err);
		return err;
	}

	public Config.Error enterRoom(Room room) {
		return enterRoom(room, null);
	}

	private void error(Config.Error err) {
		ErrResponse res = new ErrResponse(err);
		if (OnError(res)) {
			send(res);
		}
	}

	public abstract void send(BaseResponse response);

	public abstract void onMsg(Request request);

	protected boolean OnError(ErrResponse res) {
		return true;
	}

	protected Config.Error exitTable() {
		if (this.table != null) {
			onExitTable();
			Config.Error err = this.table.exit(this);
			if (err != Config.ERR_SUCCESS) {
				return err;
			}
			this.table = null;
		}
		return Config.ERR_SUCCESS;
	}

	public Config.Error exitRoom() {
		Config.Error err = exitTable();
		if (err != Config.ERR_SUCCESS) {
			return err;
		}
		if (this.room != null) {
			err = this.room.exit(this);
			if (err != Config.ERR_SUCCESS) {
				return err;
			}
			try {
				onExitRoom();
			} catch (Exception e) {
				log.error("下层游戏Role退出房间错误:", e);
			} finally {
				revert2RealMoney();
				this.room = null;
			}
		}
		return Config.ERR_SUCCESS;
	}

	// 配桌
	public Config.Error pair() {
		return pair(null);
	}

	public Config.Error pair(Table table) {
		if (this.table != null) {
			exitTable();
		}
		
		if(this instanceof RobotPkLogic) {
			initRobotExpression();
		}

		if (this.room != null) {
			return this.room.pair(this, table);
		}

		return Config.ERR_ROOM_NOT_EXIST;
	}

	// 准备继续游戏
	public Config.Error ReadyContinueGame() {
		// 如果可以继续游戏
		if (GameMain.getInstance().getGameMgr().getRobotConfig().IsNoChangeTable()) {
			// 如果桌子为空 重新匹配
			if (this.table == null && this.room != null) {
				return this.room.pair(this, table);
			} else {
				return this.table.getTablePair().ReadyContinueGame(this);
			}
		}
		// 准备出错
		return Config.ERR_READY_CONTINUE;
	}

	public abstract boolean isPairOnGaming();

	public Config.Error exitHall() {
		setDestroy(true);
		return Config.ERR_SUCCESS;
	};

	@Override
	public int compareTo(Role p1) {
		if (this.money > p1.money) {
			return 1;
		} else {
			return -1;
		}
	}

	void revert2RealMoney() {

	}

	void convert2RoomMoney() {

	}

	void updateMoney() {

	}

	public void addMoney(long win) {
		money += win;
	}

	public void cutMoney(long lose) {
		money -= lose;
	}

	public void doGameBegin() {
		onGameBegin();
	}

	public void doGameEnd() {
		onGameEnd();
	}

	protected void onGameBegin() {

	}

	protected void onGameEnd() {

	}

	protected void onEnterTable() {

	}

	protected void onExitTable() {

	}

	protected void onEnterRoom() {

	}

	protected void onExitRoom() {

	}

	protected void onEnterHall() {

	}
	
	protected void initRobotExpression() {

	}

	void doStop() {
		onStop();
	}

	void doTerminate() {
		setDestroy(true);
		onTerminate();
	}

	void doDestroy() {
		setDestroy(true);
		onDestroy();
	}

	protected void onStop() {
	}

	protected void onTerminate() {

	}

	protected void onDestroy() {

	}

	public void setSameTablePlayer(Map<Long, Role> roles) {

	}

	public boolean isSameTable(Role role) {
		return false;
	}

	/**
	 * @return boolean return the inited
	 */
	public boolean isInited() {
		return inited;
	}

	/**
	 * @param inited the inited to set
	 */
	public void setInited(boolean inited) {
		this.inited = inited;
		this.onInit();
	}

	/**
	 * @return Schedule return the tableLifeTimeSchedule
	 */
	public Schedule getTableLifeTimeSchedule() {
		return tableLifeTimeSchedule;
	}

	/**
	 * @param tableLifeTimeSchedule the tableLifeTimeSchedule to set
	 */
	public void setTableLifeTimeSchedule(Schedule tableLifeTimeSchedule) {
		this.tableLifeTimeSchedule = tableLifeTimeSchedule;
	}

}