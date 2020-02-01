package frame.game;

import frame.*;
import frame.game.proto.User;
import frame.game.proto.Game.FindRecord;
import frame.game.proto.Game.GetUserFishWinScoreReq;
import frame.game.proto.Game.GetUserFishWinScoreRes;
import frame.game.proto.Game.GetUserInfoRes;
import frame.game.proto.GameBase.GameChargeNotify;
import frame.game.proto.GameBase;
import frame.game.proto.GameControl.GameLotteryControlReq;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import io.netty.buffer.ByteBuf;
import frame.socket.common.proto.Money;
import frame.socket.common.proto.GameHistory.ReqHistorys;
import frame.socket.common.proto.PlayerControl.UpdateControlReq;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.db.DBMessageCode;
import frame.socket.route.RouteMgr;
import java.util.ArrayList;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;

public abstract class Player extends Role implements PlayerProtocol {

	/**
	 * 账号
	 */
	public String account;

	/**
	 * 账户状态：0-正常 1-冻结
	 */
	public int state;

	/**
	 * int 会员等级id
	 */
	public int levelId;

	/**
	 * 1,正式玩家,2临时玩家,默认1
	 */
	public int identity;

	/**
	 * 0：总控账号，1：厅主账号，2：会员账号，11厅主子账号
	 */
	public int accountType;

	// 配置单控金额
	private @Getter long controlMoney = 0;

	// 单控对战下注类输赢胜率
	private @Getter int controlBetRate = 0;

	// 单控捕鱼类型输赢胜率
	private @Getter int controlFishRate = 0;

	// 剩余单控金额
	private @Getter @Setter long leftControlMoney = 0;

	// 捕鱼玩家总赢分
	private @Getter @Setter long fishTotalWinMoney = 0;

	/**
	 * 是否启用会员下注 1：启用、0：禁用
	 */
	public boolean accountBet;

	public String playId;

	private @Setter long realMoney;

	// 充值金额
	private long chargeMoney = 0;

	private ChannelHandlerContext ctx;

	private @Getter boolean freshMan;
	// 同桌玩家
	public ArrayList<Long> sameTablePlayer = new ArrayList<>();

	// 玩家在线状态
	private @Getter @Setter boolean online;
	// 断线重连状态
	private @Getter @Setter boolean reconnect;

	// 是否同步过金钱
	private @Getter @Setter boolean syncMoney;

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext inCtx) {
		ctx = inCtx;
	}

	public boolean getInited() {
		return inited;
	}

	@Override
	public boolean isPairOnGaming() {
		// test by zy 2019.3.24
		return GameMain.getInstance().getGameMgr().getRobotConfig().GetIsPlayerPairOnGameing();
	};

	/**
	 * 是否被单控
	 * 
	 */
	public boolean isSystemControl() {
		if (controlMoney != 0) {
			if (controlMoney > 0 && leftControlMoney > 0) {
				return true;
			} else if (controlMoney < 0 && leftControlMoney < 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 更新剩余单控
	 * 
	 */
	public void addLefeControlMoney(long playerWinMoney) {
		leftControlMoney -= playerWinMoney;
		if (this.controlMoney != 0 && !isSystemControl()) {
			log.info("玩家:{} 单控结束:userId : {}", nickName, userId);
		}
	}

	@Override
	void revert2RealMoney() {
		if (!isInited()) {
			return;
		}
		updateChargeMoney();
		money = realMoney;
	}

	void convert2RoomMoney() {
		if (!isInited()) {
			return;
		}
		if (room != null) {
			if (room.isFreeRoom()) {
				// 策划需要, 体验金写死100W
				money = 100 * 10000 * 1000;
			} else {
				money = realMoney;
			}
			// 捕鱼游戏不开启库存控制,个人捕鱼赢分每次进入需要清0
			if (room.isFishGame() && !room.getFishStorageMgr().getConfig().isOpenStorageCtrl) {
				fishTotalWinMoney = 0;
			}
		}
	}

	@Override
	void updateMoney() {
		if (room != null && !room.isFreeRoom()) {
			realMoney = money;
		}
	}

	@Override
	public void addMoney(long win) {
		money += win;
		realMoney += win;
	}

	public long addFishTotalWinMoney(long add) {
		fishTotalWinMoney += add;
		return fishTotalWinMoney;
	}

	public void syncMoneyFromDB(long money) {
		setSyncMoney(true);
		this.realMoney = money;
		if (room == null || (room != null && !room.isFreeRoom())) {
			this.money = money;
		}
	}

	void updateChargeMoney() {
		if (chargeMoney != 0) {
			boolean inFreeRoom = false;
			if (room != null) {
				if (room.isFreeRoom()) {
					inFreeRoom = true;
				}
			}
			if (!isSyncMoney()) {
				realMoney += chargeMoney;
				if (!inFreeRoom) {
					money += chargeMoney;
				}
			}
			if (!inFreeRoom) {
				notifyChargeMoney(chargeMoney, realMoney);
			}
			setSyncMoney(false);
			chargeMoney = 0;
		} else {
			setSyncMoney(false);
		}
	}

	void notifyChargeMoney(long chargeMoney, long currentMoney) {
		GameChargeNotify gameChargeNotify = GameChargeNotify.newBuilder().setChargeMoney(chargeMoney)
				.setCurrentMoney(currentMoney).build();
		send(new Response(GameMsg.GAME_CHARGE_NOTIFY_PLAYER, gameChargeNotify.toByteArray()));
	}

	@Override
	public void doGameBegin() {
		updateChargeMoney();
		onGameBegin();
	}

	@Override
	public void doGameEnd() {
		onGameEnd();
	}

	/**
	 * 从数据库初始化玩家数据
	 * 
	 */
	public void initInfoFromDB(GetUserInfoRes res) {
		User.UserInfo user = res.getUser();
		this.account = user.getAccount();// 账号
		this.state = user.getState();// 账户状态：0-正常 1-冻结
		this.accountBet = true;// 是否启用会员下注 1：启用、0：禁用
		this.playId = String.valueOf(user.getPlayId()); // 推广相关ID
		this.gender = user.getGender();// 性别
		this.nickName = user.getNickName();// 昵称
		this.portrait = user.getPortrait();
		this.controlMoney = user.getControlMoney();
		this.controlBetRate = user.getControlBetRate();
		this.controlFishRate = user.getControlFishRate();
		this.leftControlMoney = user.getLeftControlMoney();
		this.IP = String.valueOf(user.getIp());
		realMoney = money = user.getMoney();
		log.info("从数据库初始化玩家数据成功!");
	}

	/**
	 * 发送玩家数据给客户端
	 * 
	 */
	public void sendUserInfo2Client() {
		User.UserInfo.Builder userInfo = User.UserInfo.newBuilder();
		userInfo.setAccount(account);
		userInfo.setPortrait(portrait);
		userInfo.setGender(gender);
		userInfo.setUniqueId(uniqueId);
		userInfo.setNickName(nickName);
		userInfo.setMoney(money);
		send(new Response(GameMsg.GAME_PLAYER_INFO_NOTIFY, userInfo.build().toByteArray()));
	}

	public void send(BaseResponse response) {
		if (!online) {
			log.debug("玩家:{} 不在线,发送消息失败", getPlayerInfo());
			return;
		}
		int msgType = response.msgType;
		byte[] protoMsg = response.protoMsg;
		ByteBuf msgBuf = MessageUtil.packServer2GateMsg(msgType, siteId, userId, protoMsg);
		UtilsMgr.getMsgQueue().send(ctx, msgBuf);
	}

	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType) {
		RouteMgr.send(response, targetServerType, siteId, userId);
	}

	@Override
	public void reply2Route(Request receivedRequest, BaseResponse response) {
		RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
				receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
				receivedRequest.seqId);
	};

	public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
		RouteMgr.send(response, targetServerType, siteId, userId, callback);
	}

	public void getPlayerHistory(Request req) {
		try {
			FindRecord.Builder mongoReq = FindRecord.newBuilder();
			ReqHistorys eq = ReqHistorys.parseFrom(req.protoMsg);
			mongoReq.setPageIndex(eq.getPageIndex());
			mongoReq.setPageSize(eq.getPageSize());
			mongoReq.setGameIndex(Config.GAME_ID);
			GameMain.getInstance().send2DB(new Response(DBMessageCode.FindRecord, mongoReq.build().toByteArray()),
					hall.getHallId(), userId, new Callback() {
						@Override
						public void func() {
							Request data = (Request) this.getData();
							send(new Response(889, data.protoMsg));
						}
					});
		} catch (Exception e) {
			log.error("错误", e);
		}

	}

	public void onBaseMsg(Request request) {
		switch (request.msgType) {
		case FrameMsg.PLAYER_GET_RECORD:
			getPlayerHistory(request);
			break;
		case GameMsg.DB2G_UpdateLeftControlMoney:
			try {
				UpdateControlReq updateControl = UpdateControlReq.parseFrom(request.protoMsg);
				controlMoney = updateControl.getWinLose();
				controlBetRate = updateControl.getWinRateBattleBet();
				controlFishRate = updateControl.getWinRateFish();
				leftControlMoney = controlMoney;
				log.info("更新玩家:userId:{} 单控数据成功: 单控金额 {} ,下注概率:{}%, 捕鱼概率:{}%", userId,
						controlMoney / Config.MONEY_RATIO, controlBetRate, controlFishRate);
				reply2Route(request, new Response(10000));
			} catch (Exception err) {
				reply2Route(request, new ErrResponse(-10000));
				log.error("DB2G_UpdateLeftControlMoney 处理出错:", err);
			}
			break;
		case GameMsg.GAME_LOTTERY_CONTROL_REQ:
			if (!Config.OPEN_CMD) {
				break;
			}
			try {
				GameLotteryControlReq gameLotteryControl = GameLotteryControlReq.parseFrom(request.protoMsg);
				if (table != null) {
					table.controllLottery(this, gameLotteryControl);
					if (gameLotteryControl.hasFish()) {
						if (gameLotteryControl.getFish().getStorage() != 0) {
							table.getRoom().updateCurrrentStorage(gameLotteryControl.getFish().getStorage() * 1000,
									gameLotteryControl.getFish().getStorage() * 1000);
							log.info("启动超级管理,控制捕鱼游戏库存:{}", gameLotteryControl.getFish().getStorage());
						}
						if (gameLotteryControl.getFish().getProfit() != 0) {
							fishTotalWinMoney = gameLotteryControl.getFish().getProfit() * 1000;
							log.info("启动超级管理,控制玩家捕鱼个人赢分:{}", gameLotteryControl.getFish().getProfit());
						}
					}

					log.info("玩家:userId:{} 启动超级管理,控制游戏开奖成功", userId);
					send(new ErrResponse("启动超级管理,控制游戏开奖成功"));
				} else {
					send(new ErrResponse("控制开奖失败"));
				}
			} catch (Exception err) {
				log.error("GAME_LOTTERY_CONTROL_REQ 消息处理失败:", err);
			}
			break;

		case GameMsg.PLAYER_PK_VOICE_BORADCAST_REQ:
			try {
				if (this.table == null) {
					return;
				}
				GameBase.VoiceReq voiceReq = GameBase.VoiceReq.parseFrom(request.protoMsg);
				GameBase.VoiceRes.Builder voiceRes = GameBase.VoiceRes.newBuilder();
				voiceRes.setGender(this.gender);
				voiceRes.setVoice(voiceReq.getVoice());
				voiceRes.setUniqueId(voiceReq.getUniqueId());
				byte[] val = voiceRes.build().toByteArray();
				Response resp = new Response(GameMsg.PLAYER_PK_VOICE_BORADCAST_RES, val);
				this.table.broadcast(resp);
			} catch (InvalidProtocolBufferException e) {
				log.error("语音消息处理失败");
			}
			break;
		case GameMsg.PLAYER_PK_PHIZ_BORADCAST_REQ:
			try {
				if (this.table == null) {
					return;
				}
				GameBase.EmotionReq emoTionReq = GameBase.EmotionReq.parseFrom(request.protoMsg);
				GameBase.EmotionRes.Builder emoTionRes = GameBase.EmotionRes.newBuilder();
				emoTionRes.setFromPlayer(emoTionReq.getFromPlayer());
				emoTionRes.setToPlayer(emoTionReq.getToPlayer());
				emoTionRes.setEmotionIndex(emoTionReq.getEmotionIndex());
				byte[] val = emoTionRes.build().toByteArray();
				Response resp = new Response(GameMsg.PLAYER_PK_PHIZ_BORADCAST_RES, val);
				this.table.broadcast(resp);
//				if(table.roles.size() != 0 && table.roles.containsKey(emoTionReq.getToPlayer())) {
//					if(table.roles.get(emoTionReq.getToPlayer()) instanceof Robot) {
//						Robot tempRobot = (Robot) table.roles.get(emoTionReq.getToPlayer());
//						tempRobot.receiveExpression(emoTionReq.getEmotionIndex(),this.uniqueId,emoTionReq);
//					}
//				}
			} catch (InvalidProtocolBufferException e) {
				log.error("表情消息处理失败");
			}
			break;
		case GameMsg.DB_CHARGE_NOTIFY:
			try {
				Money.ChangeGameMoneyNotify changeGameMoneyNotify = Money.ChangeGameMoneyNotify
						.parseFrom(request.protoMsg);
				long changeMoney = changeGameMoneyNotify.getChangeMoney();
				if (this.table != null) {
					if (!table.canChargeMoneyAtOnce()) {
						// 不能直接加钱,先缓存起来
						chargeMoney += changeMoney;
						reply2Route(request, new Response(10000));
						return;
					}
				}
				// 可以直接加钱,通知客户端
				realMoney += changeMoney;
				if (this.room == null || (this.room != null && !this.room.isFreeRoom())) {
					money += changeMoney;
					log.info("充值后的金额:{}，变动金额:{}", money, changeMoney);
					notifyChargeMoney(changeMoney, money);
				}
				reply2Route(request, new Response(10000));
			} catch (Exception e) {
				log.error("DB充值通知处理失败", e);
			}
			break;
		default:
			onMsg(request);
		}
	}

	public abstract void onMsg(Request request);

	public void getPlayerWinMoneyFromDB(int roomID, Callback callback) {
		GetUserFishWinScoreReq getUserFishWinScoreReqBuilder = GetUserFishWinScoreReq.newBuilder().setRoomID(roomID)
				.build();
		send2Route(new Response(600301, getUserFishWinScoreReqBuilder.toByteArray()), Server_Type.SERVER_TYPE_DATABASE,
				new Callback() {
					@Override
					public void func() {
						Request req = (Request) this.getData();
						if (req.isError()) {
							if (req.isTimeout()) {
								log.error("从DB获取玩家赢分超时");
								send(new ErrResponse("从DB获取玩家数据超时"));
							} else {
								log.error("从DB获取玩家赢分失败");
								send(new ErrResponse(Config.ERR_DB_ERROR));
							}
							callback.setData(false);
							return;
						}
						try {
							GetUserFishWinScoreRes fishWinScore = GetUserFishWinScoreRes.parseFrom(req.protoMsg);
							fishTotalWinMoney = fishWinScore.getFishWinScore();
							log.info("从DB获取到的玩家赢分:{}", fishTotalWinMoney);
							callback.setData(true);
							callback.func();
						} catch (Exception e) {
							send(new ErrResponse(Config.ERR_DB_ERROR));
							log.error("处理玩家赢分失败", e);
						}
					}
				});
	}

	public Config.Error exitHall() {
		super.exitHall();
		Config.Error err = exitRoom();
		if (err != Config.ERR_SUCCESS) {
			return err;
		}

		if (this.hall != null) {
			this.hall.exit(this);
		}
		GameMain.getInstance().getRoleMgr().removeRole(this);
		this.ctx = null;
		return Config.ERR_SUCCESS;
	}

	/*
	 * 是否在掉线时踢出: false 不踢出 , true 踢出
	 */
	public boolean isDisconnectKickOut() {
		return false;
	}
	
	
	/**
	 * 是否结算后踢玩家
	 * @return
	 */
	public boolean isResultKickOut() {
		return true;
	}
	
	

	/**
	 * 设置同桌玩家 by zy roles 玩家列表
	 */
	public void setSameTablePlayer(Map<Long, Role> roles) {
		if (sameTablePlayer == null) {
			sameTablePlayer = new ArrayList<>();
		}
		// 清空数据
		sameTablePlayer.clear();
		for (Long key : roles.keySet()) {
			if (roles.get(key) instanceof Player && roles.get(key).uniqueId != this.uniqueId) {
				sameTablePlayer.add(key);
			}
		}
	}

	/**
	 * 是否同桌 by zy
	 */
	public boolean isSameTable(Role role) {
		if (sameTablePlayer == null) {
			return false;
		}
		if (sameTablePlayer.contains(role.uniqueId)) {
			return true;
		}
		return false;
	}

	public String getPlayerControlInfo() {
		return String.format("%s , userId:%d , 单控配置金币:%f , 剩余单控金额: %f , 下注胜率: %d , 捕鱼概率: %d", nickName, userId,
				controlMoney / Config.MONEY_RATIO_FLOAT, leftControlMoney / Config.MONEY_RATIO_FLOAT, controlBetRate,
				controlFishRate);
	}

	public String getPlayerInfo() {
		return String.format("%s,userId:%d", nickName, userId);
	}

	public abstract void onConnected();

	public abstract void onReconnect();

	public abstract void onDisconnect();

}