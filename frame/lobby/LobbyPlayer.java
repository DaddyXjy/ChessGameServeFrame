package frame.lobby;

import frame.socket.*;
import frame.socket.common.proto.Money;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.route.RouteMgr;
import frame.*;
import frame.game.GameMsg;
import frame.game.proto.Game.GetUserInfoRes;
import frame.game.proto.GameBase.GameChargeNotify;
import lombok.Getter;
import lombok.Setter;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import frame.util.*;

public abstract class LobbyPlayer extends Root implements PlayerProtocol, MsgDealProtocol {

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

	/**
	 * 是否启用会员下注 1：启用、0：禁用
	 */
	public boolean accountBet;

	public String playId;

	private long sqlMoney;

	private long realMoney;

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
	 * 用户余额
	 */
	public long money;

	public long uniqueId;

	private boolean inited;

	private @Getter @Setter ChannelHandlerContext ctx;

	protected @Getter LobbyHall hall;

	public boolean getInited() {
		return inited;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext inCtx) {
		ctx = inCtx;
	}

	public void init() {
		onInit();
		inited = true;
	}

	protected void onInit() {

	}

	private void error(Config.Error err) {
		ErrResponse res = new ErrResponse(err);
		if (OnError(res)) {
			send(res);
		}
	}

	protected boolean OnError(ErrResponse res) {
		return true;
	}

	void enterHall(LobbyHall hall) {
		hall.enter(this);
		this.hall = hall;
		onEnterHall();
	}

	public Config.Error exitHall() {
		setDestroy(true);
		onExitHall();
		if (this.hall != null) {
			this.hall.exit(this);
		}
		save();
		LobbyMain.getInstance().getRoleMgr().removePlayer(this);
		this.ctx = null;
		return Config.ERR_SUCCESS;
	};

	public void save() {

	}

	public void addMoney(long win) {
		money += win;
	}

	/**
	 * 从数据库初始化玩家数据
	 * 
	 */
	public abstract void initInfoFromDB(GetUserInfoRes res);

	protected void onEnterHall() {

	}

	protected void onExitHall() {

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

	public void send(BaseResponse res) {
		ByteBuf msgBuf = MessageUtil.packServer2GateMsg(res.msgType, siteId, userId, res.protoMsg);
		UtilsMgr.getMsgQueue().send(ctx, msgBuf);
	}

	/**
	 * 发送消息给路由(不期待回应)
	 * 
	 * @response: 回复的消息
	 * @targetServerType: 目标服务器类型
	 */
	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType) {
		RouteMgr.send(response, targetServerType, siteId, userId);
	}

	/**
	 * 发送消息给路由(期待回应)
	 * 
	 * @response: 回复的消息
	 * @targetServerType: 目标服务器类型
	 * @callback: 路由回复的消息
	 * @example: send2Route(new Response(1000001 , protoMsg)
	 *           ,Server_Type.SERVER_TYPE_DATABASE, new Callback(){
	 * @Override public void func() { Request req = (Request) this.getData();
	 *           if(req.isError()){ //错误处理走这里 }else{ //正确的处理走这里 } } } )
	 * 
	 */
	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
		RouteMgr.send(response, targetServerType, siteId, userId, callback);
	}

	/**
	 * 回复消息给路由
	 * 
	 * @receivedReq: 收到的请求
	 * @response: 回复的消息
	 */
	@Override
	public void reply2Route(Request receivedRequest, BaseResponse response) {
		RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
				receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
				receivedRequest.seqId);
	};

	/**
	 * 转发客户端消息给指定服务器
	 * 
	 * @clientReq: 客户端请求
	 * @targetServerType: 目标服务器类型
	 */
	public void transferMsg(Request clientReq, Server_Type targetServerType) {
		send2Route(new Response(clientReq), targetServerType, new Callback() {
			@Override
			public void func() {
				Request backReq = (Request) this.getData();
				send(new Response(backReq));
			}
		});
	}

	/**
	 * 转发客户端消息给指定服务器
	 * 
	 * @clientReq: 客户端请求
	 * @targetServerType: 目标服务器类型
	 * @callback: 大厅回调处理
	 */
	public void transferMsg(Request clientReq, Server_Type targetServerType, Callback callback) {
		send2Route(new Response(clientReq), targetServerType, new Callback() {
			@Override
			public void func() {
				Request backReq = (Request) this.getData();
				callback.setData(backReq);
				callback.func();
				send(new Response(backReq));
			}
		});
	}

	void notifyChargeMoney(long chargeMoney, long currentMoney) {
		GameChargeNotify gameChargeNotify = GameChargeNotify.newBuilder().setChargeMoney(chargeMoney)
				.setCurrentMoney(currentMoney).build();
		send(new Response(GameMsg.GAME_CHARGE_NOTIFY_PLAYER, gameChargeNotify.toByteArray()));
	}

	public void onBaseMsg(Request request) {
		switch (request.msgType) {
		case GameMsg.DB_CHARGE_NOTIFY:
			try {
				Money.ChangeGameMoneyNotify changeGameMoneyNotify = Money.ChangeGameMoneyNotify
						.parseFrom(request.protoMsg);
				long changeMoney = changeGameMoneyNotify.getChangeMoney();
				// 直接加钱,通知客户端
				realMoney += changeMoney;
				money += changeMoney;
				notifyChargeMoney(changeMoney, money);
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

	protected void onStop() {
	}

	protected void onTerminate() {

	}

	protected void onDestroy() {

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
	}

	public abstract void onConnected();

	public void onDisconnect() {

	}

	public void onReconnect() {

	}

	public void setDebugMessage(Request req) {

	}
}