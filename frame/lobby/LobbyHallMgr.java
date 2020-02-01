package frame.lobby;

import java.util.HashMap;

import frame.Callback;
import frame.Config;
import frame.HallMgrProtocol;
import frame.UtilsMgr;
import frame.log;
import frame.socket.BaseResponse;
import frame.socket.ErrResponse;
import frame.socket.OnMsgProtocol;
import frame.socket.Request;
import frame.socket.Response;
import frame.socket.common.proto.LobbySiteRoom.AllGameData;
import frame.socket.common.proto.LobbySiteRoom.GameData;
import frame.socket.common.proto.LobbySiteRoom.GameDataList;
import frame.socket.common.proto.LobbySiteRoom.TbSiteIdentificationReq;
import frame.socket.common.proto.LobbySiteRoom.TbSiteIdentificationRes;
import frame.socket.route.RouteMgr;
import lombok.Getter;
import lombok.Setter;

public final class LobbyHallMgr implements HallMgrProtocol, OnMsgProtocol {
    private @Getter HashMap<Integer, LobbyHall> halls = new HashMap<Integer, LobbyHall>();

    private @Getter boolean ready;
    private @Getter int retryTimes = 0;
    
    private @Getter@Setter double prizeMoney;

    public void add(int id, LobbyHall hall) {
        halls.put(id, hall);
    }

    public LobbyHall get(int id) {
        return halls.get(id);
    }

    public boolean canTerminate() {
        for (LobbyHall hall : halls.values()) {
            if (!hall.canTerminate()) {
                return false;
            }
        }
        return true;
    }

    public void doPrepare() {

    }

    public void doStop() {
        for (LobbyHall hall : halls.values()) {
            hall.doStop();
        }
    }

    public void doTerminate() {
        for (LobbyHall hall : halls.values()) {
            hall.doTerminate();
        }
    }

    public void doDestroy() {
        for (LobbyHall hall : halls.values()) {
            hall.doDestroy();
        }
    }

    public void update() {
        for (LobbyHall hall : halls.values()) {
            hall.update();
        }
    }

    LobbyHall createHall(int key) {
        LobbyHall hall = LobbyMain.getInstance().getLobbyMgr().createHall(key);
        add(key, hall);
        return hall;
    }

    // 初始化所有大厅
    public void init(Callback callback) {
        ready = false;
        TbSiteIdentificationReq siteReq = TbSiteIdentificationReq.newBuilder().setSiteId(Config.SITE_ID).build();
        LobbyMain.getInstance().send2Account(new Response(LobbyMsg.GET_ALL_HALL_DATA_Q, siteReq.toByteArray()), 0, 0,
                new Callback() {
                    @Override
                    public void func() {
                        Request req = (Request) this.getData();
                        if (req.isError()) {
                            log.error("从后台获取大厅数据错误,正在重试...第{}次", ++retryTimes);
                            UtilsMgr.getTaskMgr().createTimer(10, new Callback() {
                                @Override
                                public void func() {
                                    init(callback);
                                }
                            });
                            return;
                        }
                        try {
                            StringBuilder logBuilder = new StringBuilder();
                            logBuilder.append("后台获取大厅数据成功返回:");
                            TbSiteIdentificationRes siteData = TbSiteIdentificationRes.parseFrom(req.protoMsg);
                            int siteId = siteData.getTbSiteIdentification().getSiteId();
                            LobbyHall hall = createHall(siteId);
                            hall.initSiteVip();
                            hall.updateHall(siteData.getTbSiteIdentification());
                            logBuilder.append("siteId:").append(siteId).append("\t");
                            callback.func();
                            ready = true;
                            UtilsMgr.setSiteDataAcquired(true);
                            UtilsMgr.setRoomDataAcquired(true);
                            UtilsMgr.setStorageDataAcquired(true);
                            log.info(logBuilder.toString());
                        } catch (Exception e) {
                            log.error("解析proto协议失败, SelectTbSiteIdentification");
                        }
                    }
                });
    }

    public void onMsg(Request request) {
        int msgType = request.msgType;
        switch (msgType) {
        case 270://更新多个站点游戏状态
            try {
                GameDataList gameList = GameDataList.parseFrom(request.protoMsg);
                for (AllGameData game : gameList.getAllGameDataList()) {
                    for (int hallSiteId : halls.keySet()) {
                        if (hallSiteId == game.getSiteId()) {
                            for (GameData gameData : game.getGameDataList()) {
                                halls.get(hallSiteId).updateGameData(gameData);
                            }
                        }
                    }
                }
                reply2Route(request, new Response(10000));
            } catch (Exception e) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("proto解析出错: GameData, msgType:{}", LobbyMsg.ADD_GAME);
            }
            break;
        default:
            int siteId = request.siteid;
            for (int hallSiteId : halls.keySet()) {
                if (hallSiteId == siteId) {
                    halls.get(hallSiteId).onBaseMsg(request);
                }
            }
            break;
        }
    }

    
    /**
     * 回复消息给路由
     * 
     * @receivedRequest: 收到的请求
     * @response: 回复的消息
     */
    public void reply2Route(Request receivedRequest, BaseResponse response) {
        RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
                receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
                receivedRequest.seqId);
    }


}