package frame.lobby;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.protobuf.InvalidProtocolBufferException;

import frame.BroadCastProtocol;
import frame.Callback;
import frame.FrameMsg;
import frame.UtilsMgr;
import frame.log;
import frame.game.proto.Game.updataTurnConfig;
import frame.socket.BaseResponse;
import frame.socket.ErrResponse;
import frame.socket.MessageUtil;
import frame.socket.MsgDealProtocol;
import frame.socket.Request;
import frame.socket.Response;
import frame.socket.common.proto.LobbySiteRoom.GameData;
import frame.socket.common.proto.LobbySiteRoom.SelectTbSiteIdentification;
import frame.socket.common.proto.LobbySiteRoom.UpdateGameDataList;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.redis.redisPool;
import frame.socket.route.RouteMgr;
import frame.util.JsonUtil;
import frame.vo.SiteVipLevelVo;
import frame.vo.SiteVipVo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

@SuppressWarnings("deprecation")
public class LobbyHall implements BroadCastProtocol, MsgDealProtocol {
	private @Getter HashMap<Long, LobbyPlayer> roles = new HashMap<>();
	private @Getter int hallId;
	private @Getter List<GameData> gameDatas;

	/** VIP特权配置 */
	private @Getter SiteVipVo vipVo;

	/** 初始化VIP特权配置 */
	public void initSiteVip() {
		String json = redisPool.getRange("VIP_SERVICE:MAIN" + hallId);
		if (json == null || json.equals("")) {
			return;
		}

		SiteVipVo vipVo = JsonUtil.parseObject(StringEscapeUtils.unescapeJava(json), SiteVipVo.class);
		if (vipVo != null && vipVo.getStatus() == 1) {
			this.vipVo = vipVo;
		}
	}

	protected LobbyHall(int id) {
		hallId = id;
	}

	public void updateHall(SelectTbSiteIdentification siteData) {
		gameDatas = siteData.getGameDatasList();
		gameDatas = new ArrayList<GameData>(gameDatas);
	}

	public void enter(LobbyPlayer role) {
		roles.put(role.uniqueId, role);
	};

	void exit(LobbyPlayer role) {
		roles.remove(role.uniqueId);
	}

	boolean canTerminate() {
		return true;
	}

	/**
	 * 大厅玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 */
	public void broadcast(BaseResponse msg) {
		for (LobbyPlayer r : roles.values()) {
			r.send(msg);
		}
	}

	/**
	 * 大厅玩家广播
	 *
	 * @param msg
	 *            需要广播的消息
	 * @param excludeUniqueId
	 *            不包含的玩家 uniqueId
	 */
	public void broadcast(BaseResponse msg, long excludeUniqueId) {
		for (LobbyPlayer r : roles.values()) {
			if (r != null) {
				if (excludeUniqueId != r.uniqueId) {
					r.send(msg);
				}
			}
		}
	}

	/**
	 * 大厅玩家广播
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
		for (LobbyPlayer r : roles.values()) {
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
	 * 心跳,每帧自动调用
	 */
	public void update() {

	}

	public void send(BaseResponse response, ChannelHandlerContext ctx, int siteId, int userId) {
		ByteBuf msgBuf = MessageUtil.packServer2GateMsg(response.msgType, siteId, userId, response.protoMsg);
		UtilsMgr.getMsgQueue().send(ctx, msgBuf);
	}

	public void send2Route(BaseResponse response) {

	}

	public void onBaseMsg(Request request) {
		int msgType = request.msgType;
		switch (msgType) {
		// 新增游戏
		case 100:
			try {
				GameData gameData = GameData.parseFrom(request.protoMsg);
				updateGameData(gameData);
				reply2Route(request, new Response(10000));
			} catch (Exception e) {
				reply2Route(request, new ErrResponse(-10000));
				log.error("proto解析出错: GameData, msgType:{}", LobbyMsg.ADD_GAME);
			}
			break;
		case 101:// 删除游戏
			try {
				GameData delGame = GameData.parseFrom(request.protoMsg);
				int gameID = delGame.getGameID();
				if (gameID < 0) {// 如果传过来的gameID小于0，则删除当前所有游戏
					gameDatas.clear();
				} else {
					gameDatas.removeIf(GameData -> {
						return GameData.getGameID() == gameID;
					});
				}
				onDelGameData(gameID);
				reply2Route(request, new Response(10000));
			} catch (Exception e) {
				reply2Route(request, new ErrResponse(-10000));
				log.error("proto解析出错: GameData, msgType:{} error:{}", LobbyMsg.ADD_GAME, e);
			}
			break;
		case 266:// 更新游戏列表
			try {
				UpdateGameDataList gameList = UpdateGameDataList.parseFrom(request.protoMsg);
				ArrayList<GameData> gameArray = new ArrayList<GameData>(gameList.getGameDatasList());
				for (GameData game : gameArray) {
					updateGameData(game);
				}
				reply2Route(request, new Response(10000));
			} catch (Exception e) {
				reply2Route(request, new ErrResponse(-10000));
				log.error("proto解析出错: GameData, msgType:{}", LobbyMsg.ADD_GAME);
			}
			break;
        case FrameMsg.UPDATA_TURNCONFIG:
        	try {
				updataTurnConfig turnConfig = updataTurnConfig.parseFrom(request.protoMsg);
				double prizeMoney = turnConfig.getPrizeMoney();
				log.info("大厅ID:{},后台修改配置--奖池金额:{}",hallId,prizeMoney);
				LobbyMain.getInstance().getHallMgr().setPrizeMoney(prizeMoney);
			} catch (Exception e) {
				log.error("turnConfig解析出错");
				e.printStackTrace();
			}
        	break;
		default:
			onMsg(request);
			break;
		}
	}

	public void onMsg(Request request) {

	}

	public void doStop() {

		onStop();
	}

	public void doDestroy() {

		onDestroy();
	}

	public void doTerminate() {

		onTerminate();
	}

	public void onStop() {

	}

	public void onDestroy() {

	}

	public void onTerminate() {

	}

	public void updateGameData(GameData gameDataIn) {
		for (int i = 0; i < gameDatas.size(); i++) {
			GameData gameData = gameDatas.get(i);
			if (gameData.getGameID() == gameDataIn.getGameID()) {
				gameDatas.set(i, gameDataIn);
				onUpdateGameData(gameDataIn.getGameID());
				return;
			}
		}
		// 没有找到游戏,新增游戏
		gameDatas.add(gameDataIn);
		onCreateGameData(gameDataIn.getGameID());
	}

	public void onUpdateGameData(int gameID) {
		log.info("更新游戏配置数据成功: gameID: {}", gameID);
	}

	public void onCreateGameData(int gameID) {
		log.info("新增游戏配置数据成功: gameID: {}", gameID);
	}

	public void onDelGameData(int gameID) {
		log.info("删除游戏配置数据成功: gameID: {}", gameID);
	}

	@Override
	public void send(BaseResponse response) {

	}

	/**
	 * 发送消息给路由(不期待回应)
	 * 
	 * @response: 回复的消息
	 * @targetServerType: 目标服务器类型
	 */
	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType) {
		RouteMgr.send(response, targetServerType, hallId, 0);
	}

	/**
	 * 发送消息给路由(期待回应)
	 * 
	 * @response: 回复的消息
	 * @targetServerType: 目标服务器类型
	 * @callback: 路由回复的消息
	 * @example: send2Route(new Response(1000001 , protoMsg) ,
	 *           Server_Type.SERVER_TYPE_DATABASE, new Callback(){
	 * @Override public void func() { Request req = (Request) this.getData();
	 *           if(req.isError()){ //错误处理走这里 }else{ //正确的处理走这里 } } } )
	 * 
	 */

	@Override
	public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
		RouteMgr.send(response, targetServerType, hallId, 0, callback);
	}

	/**
	 * 回复消息给路由
	 * 
	 * @receivedRequest: 收到的请求
	 * @response: 回复的消息
	 */
	@Override
	public void reply2Route(Request receivedRequest, BaseResponse response) {
		RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType, receivedRequest.targetServerSubType,
				receivedRequest.siteid, receivedRequest.userid, receivedRequest.seqId);
	};
}