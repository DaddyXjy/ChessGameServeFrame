package frame.game;

import frame.*;
import frame.lobby.LobbyMsg;
import frame.socket.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import frame.socket.ErrorMsg.ErrorRes;
import frame.socket.common.proto.LobbySiteRoom.*;
import frame.socket.common.proto.Storage.*;
import frame.socket.redis.redisPool;
import frame.socket.route.RouteMgr;
import frame.socket.common.proto.LobbySiteRoom.RoomCfgReq.Builder;

public final class GameHallMgr implements HallMgrProtocol, OnMsgProtocol {
    private Map<String, List> lastUpdateRoomConfig;
    private @Getter HashMap<Integer, GameHall> halls = new HashMap<Integer, GameHall>();

    private @Getter boolean ready;

    private @Getter ActivityConfig actiConfig = new ActivityConfig();

    private @Getter int retryTimes = 0;

    public void add(int id, GameHall hall) {
        halls.put(id, hall);
    }

    public GameHall get(int id) {
        return halls.get(id);
    }

    public boolean canTerminate() {
        for (GameHall hall : halls.values()) {
            if (!hall.canTerminate()) {
                return false;
            }
        }

        return true;
    }

    public void doPrepare() {

    }

    public void doStop() {
        for (GameHall hall : halls.values()) {
            hall.doStop();
        }
    }

    public void doTerminate() {
        for (GameHall hall : halls.values()) {
            hall.doTerminate();
        }
    }

    public void doDestroy() {
        for (GameHall hall : halls.values()) {
            hall.doDestroy();
        }
    }

    public void update() {
        for (GameHall hall : halls.values()) {
            hall.update();
        }
    }

    GameHall createHall(Integer key) {
        GameHall hall = GameMain.getInstance().getGameMgr().createHall(key);
        add(key, hall);
        log.info("创建站点:{} 成功", key);
        return hall;
    }

    public void init(Callback callback) {
        ready = false;
        TbSiteIdentificationReq siteReq = TbSiteIdentificationReq.newBuilder().setSiteId(Config.SITE_ID).build();
        GameMain.getInstance().send2Account(new Response(LobbyMsg.GET_ALL_HALL_DATA_Q, siteReq.toByteArray()),
                Config.SITE_ID, 0, new Callback() {
                    @Override
                    public void func() {
                        Request req = (Request) this.getData();
                        if (req.isError()) {
                            if (req.isTimeout()) {
                                log.warn("从后台获取大厅:{}数据超时,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                if (retryTimes > 10) {
                                    log.error("从后台获取大厅:{}数据超时超过10次,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                }
                            } else {
                                try {
                                    ErrorRes errorRes = ErrorRes.parseFrom(req.protoMsg);
                                    log.error("从后台获取大厅:{}数据错误:{},正在重试...第{}次", Config.SITE_ID, errorRes.getMsg(),
                                            ++retryTimes);
                                } catch (Exception e) {
                                    log.error("error :", e);
                                }
                            }
                            UtilsMgr.getTaskMgr().createTimer(5, new Callback() {
                                @Override
                                public void func() {
                                    init(callback);
                                }
                            });
                            return;
                        }
                        try {
                            retryTimes = 0;
                            StringBuilder logBuilder = new StringBuilder();
                            logBuilder.append("后台获取大厅数据成功返回:");
                            TbSiteIdentificationRes siteData = TbSiteIdentificationRes.parseFrom(req.protoMsg);
                            int siteId = siteData.getTbSiteIdentification().getSiteId();
                            GameHall hall = createHall(siteId);
                            logBuilder.append("siteId:").append(siteId).append("\t");
                            hall.setBetTriggerStorage(siteData.getTbSiteIdentification().getBetTriggerStorage());
                            log.info(logBuilder.toString());
                            UtilsMgr.setSiteDataAcquired(true);
                            initRoomDatas(callback);
                        } catch (Exception e) {
                            log.error("处理获取大厅出错, SelectTbSiteIdentification", e);
                        }
                    }
                });
    }

    // 从后台获取房间
    public void initRoomDatas(Callback callback) {
        Builder roomBuilder = RoomCfgReq.newBuilder();
        roomBuilder.setGameId(Config.GAME_ID);
        int backServerGameType = GameMain.getInstance().getGameMgr().getRobotConfig().GetBackServerGameType();
        roomBuilder.setGameType(backServerGameType);
        for (int hallID : halls.keySet()) {
            roomBuilder.addSiteId(hallID);
        }
        GameMain.getInstance().send2Account(new Response(GameMsg.GET_ALL_ROOM_DATA, roomBuilder.build().toByteArray()),
                0, 0, new Callback() {
                    @Override
                    public void func() {
                        Request req = (Request) this.getData();
                        if (req.isError()) {
                            if (req.isTimeout()) {
                                log.warn("从后台获取站点:{} 房间数据超时,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                if (retryTimes > 10) {
                                    log.error("从后台获取站点:{} 房间数据超时超过10次,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                }
                            } else {
                                try {
                                    ErrorRes errorRes = ErrorRes.parseFrom(req.protoMsg);
                                    log.error("从后台获取站点:{} 房间数据错误:{},正在重试...第{}次", Config.SITE_ID, errorRes.getMsg(),
                                            ++retryTimes);
                                } catch (Exception e) {
                                    log.error("error :", e);
                                }
                            }
                            UtilsMgr.getTaskMgr().createTimer(5, new Callback() {
                                @Override
                                public void func() {
                                    initRoomDatas(callback);
                                }
                            });
                            return;
                        }
                        try {
                            log.info("从后台获取房间数据成功");
                            GetAllRoomRes allRoomCfgList = GetAllRoomRes.parseFrom(req.protoMsg);
                            if (allRoomCfgList.getBetRoomCfgCount() > 0) {
                                for (BetRoomCfg roomCfg : allRoomCfgList.getBetRoomCfgList()) {
                                    int siteID = roomCfg.getSiteId();
                                    int roomID = roomCfg.getId();
                                    GameHall hall = get(siteID);
                                    if (hall != null) {
                                        Room room = hall.getRoomMgr().getRoom(roomID);
                                        if (room == null) {
                                            if (Config.DEBUG_ROOM_ID == 0) {
                                                room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                            } else {
                                                if (Config.DEBUG_ROOM_ID == roomID) {
                                                    room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (allRoomCfgList.getFishRoomCfgCount() > 0) {
                                for (FishRoomCfg roomCfg : allRoomCfgList.getFishRoomCfgList()) {
                                    int siteID = roomCfg.getSiteId();
                                    int roomID = roomCfg.getId();
                                    GameHall hall = get(siteID);
                                    if (hall != null) {
                                        // 是否开放体验房
                                        if (!Config.OPEN_FREE_ROOM && roomCfg.getRoomType() == 1) {
                                            continue;
                                        }
                                        Room room = hall.getRoomMgr().getRoom(roomID);
                                        if (room == null) {
                                            if (Config.DEBUG_ROOM_ID == 0) {
                                                room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                            } else {
                                                if (Config.DEBUG_ROOM_ID == roomID) {
                                                    room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (allRoomCfgList.getPkRoomCfgCount() > 0) {
                                for (PkRoomCfg roomCfg : allRoomCfgList.getPkRoomCfgList()) {
                                    int siteID = roomCfg.getSiteId();
                                    int roomID = roomCfg.getId();
                                    GameHall hall = get(siteID);
                                    if (hall != null) {
                                        Room room = hall.getRoomMgr().getRoom(roomID);
                                        if (room == null) {
                                            // 是否开放体验房
                                            if (!Config.OPEN_FREE_ROOM && roomCfg.getRoomType() == 1) {
                                                continue;
                                            }
                                            if (Config.DEBUG_ROOM_ID == 0) {
                                                room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                            } else {
                                                if (Config.DEBUG_ROOM_ID == roomID) {
                                                    room = hall.getRoomMgr().createRoom(roomID, roomCfg);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            UtilsMgr.setRoomDataAcquired(true);
                            initRoomStorage(callback);
                        } catch (Exception e) {
                            log.error("初始化房间列表失败:", e);
                        }
                    }
                });

    }

    // 从游戏DB读取当前房间库存值
    public void initRoomStorage(Callback callback) {
        GetAllStorageReq.Builder storageReqBuilder = GetAllStorageReq.newBuilder();
        storageReqBuilder.setGameID(Config.GAME_ID);
        GameHall hall = halls.get(Config.SITE_ID);
        if (hall != null) {
            for (Integer roomID : hall.getRoomMgr().getRooms().keySet()) {
                storageReqBuilder.addRoomID(roomID);
            }
        }
        GameMain.getInstance().send2DB(
                new Response(GameMsg.GET_ALL_ROOM_STORAGE_CFG, storageReqBuilder.build().toByteArray()), Config.SITE_ID,
                0, new Callback() {
                    @Override
                    public void func() {
                        Request req = (Request) this.getData();
                        if (req.isError()) {
                            if (req.isTimeout()) {
                                log.warn("从游戏DB获取房间库存:{} 房间数据超时,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                if (retryTimes > 10) {
                                    log.error("从游戏DB获取房间库存:{} 房间数据超时超过10次,正在重试...第{}次", Config.SITE_ID, ++retryTimes);
                                }
                            } else {
                                try {
                                    ErrorRes errorRes = ErrorRes.parseFrom(req.protoMsg);
                                    log.error("从游戏DB获取房间库存:{} 房间数据错误:{},正在重试...第{}次", Config.SITE_ID, errorRes.getMsg(),
                                            ++retryTimes);
                                } catch (Exception e) {
                                    log.error("error :", e);
                                }
                            }
                            UtilsMgr.getTaskMgr().createTimer(5, new Callback() {
                                @Override
                                public void func() {
                                    initRoomStorage(callback);
                                }
                            });
                            return;
                        }
                        try {
                            retryTimes = 0;
                            GetAllStorageRes allStorageRes = GetAllStorageRes.parseFrom(req.protoMsg);
                            GameHall hall = halls.get(req.siteid);
                            if (hall != null) {
                                for (StorageInfo storageInfo : allStorageRes.getStorageInfosList()) {
                                    int roomID = storageInfo.getRoomID();
                                    Room room = hall.getRoomMgr().getRoom(roomID);
                                    room.updateCurrrentStorage(storageInfo.getStorage1(), storageInfo.getStorage2());
                                }
                            }
                            UtilsMgr.setStorageDataAcquired(true);
                            initActivityData(callback);
                        } catch (Exception e) {
                            log.error("初始化房间库存数据失败:", e);
                        }
                    }
                });
    }

    // 从redis 获取活动数据
    public void initActivityData(Callback callback) {
        try {
            String val = redisPool.getString("ACCOUNT_SERVICE:SITE_ACT:" + Config.SITE_ID + "6");
            this.actiConfig = JSON.parseObject(val, ActivityConfig.class);
            ready = true;
            callback.func();
        } catch (Exception e) {
            log.error("initActivityData error:", e);
        }
    }

    public void onMsg(Request request) {
        int siteId = request.siteid;
        boolean isFindHall = false;
        for (int hallSiteId : halls.keySet()) {
            if (hallSiteId == siteId) {
                halls.get(hallSiteId).onBaseMsg(request);
                isFindHall = true;
                break;
            }
        }
        if (!isFindHall) {
            reply2Route(request, new ErrResponse("没有找到对应站点"));
        }
    }

    public void reply2Route(Request receivedRequest, BaseResponse response) {
        RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
                receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
                receivedRequest.seqId);
    };
}