package frame.game;

import frame.socket.*;
import frame.socket.common.proto.LobbySiteRoom.*;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.GateMgr;
import frame.socket.monitor.proto.Monica.AdjustStorage;
import frame.socket.route.RouteMgr;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;

import com.google.protobuf.InvalidProtocolBufferException;

import frame.socket.common.proto.Storage.StorageConfig;
import frame.socket.common.proto.Storage.UpdateStorageReduce;
import frame.*;
import frame.game.proto.Game.updataTurnConfig;

public class GameHall implements BroadCastProtocol, MsgDealProtocol {
    private @Getter RoomMgr roomMgr;
    private @Getter HashMap<Long, Role> roles = new HashMap<>();
    private @Getter int hallId;
    // 所有房间配置
    private @Getter GetAllRoomRes allRoomCfg;
    // 下注类触发库存配置
    private @Getter @Setter BetTriggerStorage betTriggerStorage;

    GameHall(int id) {
        hallId = id;
        roomMgr = new RoomMgr();
        roomMgr.setHall(this);
    }

    public void initRoomCfgs(GetAllRoomRes roomCfgs) {
        allRoomCfg = roomCfgs;
    }

    public void createOrUpdateRoomCfgs(GetAllRoomRes roomCfgs) {
        allRoomCfg = roomCfgs;
    }

    public void enter(Role role) {
        roles.put(role.uniqueId, role);
    };

    void exit(Role role) {
        roles.remove(role.uniqueId);
    }

    void enterRoom(Role role, String id) {
        roomMgr.getRooms().get(id).enter(role);
    }

    boolean canTerminate() {
        return roomMgr.canTerminate();
    }

    /**
     * 心跳,每帧自动调用
     */
    public void update() {
        roomMgr.update();
    }

    /**
     * 大厅信息
     */
    public String getHallTextInfo() {
        return String.format("【大厅】: %d", hallId);
    }

    public void send(BaseResponse res) {

    }

    public void onBaseMsg(Request request) {
        switch (request.msgType) {
        case GameMsg.ADD_ROOM:
            try {
                RoomUpdateRes roomData = RoomUpdateRes.parseFrom(request.protoMsg);
                updateRoomCfg(roomData);
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("消息处理出错: RoomUpdateRes ", err);
            }
            break;
        case GameMsg.DEL_ROOM:
            try {
                DelRoomReq roomData = DelRoomReq.parseFrom(request.protoMsg);
                int roomID = roomData.getId();
                roomMgr.closeRoom(roomID);
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("消息处理出错:{}", GameMsg.ADD_ROOM);
            }
            break;
        case GameMsg.UPDATE_ROOM:
            try {
                RoomUpdateRes roomData = RoomUpdateRes.parseFrom(request.protoMsg);
                updateRoomCfg(roomData);
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("消息处理出错: UPDATE_ROOM ", err);
            }
            break;
        case GameMsg.OPEN_ROOM:
            try {
                OpenRoomReq roomData = OpenRoomReq.parseFrom(request.protoMsg);
                int roomID = roomData.getId();
                Room room = roomMgr.getRoom(roomID);
                if (room != null) {
                    room.forbid(1);
                }
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("消息处理出错: OPEN_ROOM ", err);
            }
            break;
        case GameMsg.FORBID_ROOM:
            try {
                ForbidRoomReq roomData = ForbidRoomReq.parseFrom(request.protoMsg);
                int roomID = roomData.getId();
                Room room = roomMgr.getRoom(roomID);
                if (room != null) {
                    room.forbid(2);
                }
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("消息处理出错: FORBID_ROOM ", err);
            }
            break;
        case GameMsg.UPDATE_ROOM_STORAGE_CFG:
            try {
                StorageConfig storageInfo = StorageConfig.parseFrom(request.protoMsg);
                int roomID = storageInfo.getRoomID();
                Room room = roomMgr.getRoom(roomID);
                if (room == null) {
                    log.error("站点:{} 房间:{} 库存更新配置失败, 原因:没有找到房间", hallId, roomID);
                    return;
                }
                room.updateStorageConfig(storageInfo);
                reply2Route(request, new Response(10000));
                log.info("站点:{} 房间:{} 库存更新配置成功, {}", hallId, roomID);
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("房间库存更新配置失败:", err);
            }
            break;
        case GameMsg.BATCH_UPDATE_STORAGE_REDUCE:
            try {
                UpdateStorageReduce updateStorageReduce = UpdateStorageReduce.parseFrom(request.protoMsg);
                int storageReduce = updateStorageReduce.getStorageReduce();
                for (Room room : roomMgr.getRooms().values()) {
                    room.updateStorageReduce(storageReduce);
                }
                reply2Route(request, new Response(10000));
                log.info("站点:{} 库存衰减值批量更新成功, {}", hallId, storageReduce);
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("房间库存更新配置失败:", err);
            }
            break;
        case GameMsg.UPDATE_ROOM_STORAGE_VALUE:
            try {
                // TODO 后期需求改库存当前值
                log.info("站点:{} 房间:{} 库存更新库存当前值成功", hallId, 0);
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("更新库存当前值失败:", err);
            }
            break;
        case GameMsg.BET_NUM_STORAGE_TRIGGER:
            try {
                BetTriggerStorage betTriggerStorage = BetTriggerStorage.parseFrom(request.protoMsg);
                this.betTriggerStorage = betTriggerStorage;
                log.info("站点:{} 房间:{} 更新下注触发下注触发库存当前值成功", hallId);
                reply2Route(request, new Response(10000));
            } catch (Exception err) {
                reply2Route(request, new ErrResponse(-10000));
                log.error("更新下注触发当前值失败:", err);
            }
            break;
        case FrameMsg.MODIFY_GAME_STORAGE:
            try {
                AdjustStorage adjustStorage = AdjustStorage.parseFrom(request.protoMsg);
                if (adjustStorage.getSiteId() == hallId) {
                    long storage = adjustStorage.getStorageCurrent() * Config.MONEY_RATIO;
                    updateGameStorage(storage);
                }
            } catch (Exception e) {
                log.error("AdjustStorage error :", e);
            }
        case FrameMsg.UPDATA_TURNCONFIG:
        	try {
        		log.info("收到活动配置更新消息:{}",Config.SITE_ID);
				updataTurnConfig turnConfig = updataTurnConfig.parseFrom(request.protoMsg);
				ActivityConfig config = GameMain.getInstance().getHallMgr().getActiConfig();
				if(config == null) {
					config = new ActivityConfig(turnConfig);
				}else {
					config.setActivityBegin(turnConfig.getStartime());
					config.setActivityEnd(turnConfig.getEndtime());
					config.setBetRate((int)turnConfig.getPercentage());
					config.setStatus(turnConfig.getStatus());
				}
			} catch (Exception e) {
				log.error("UpdataTurnConfig error :", e);
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

    /**
     * 大厅玩家广播
     *
     * @param msg 需要广播的消息
     */
    public void broadcast(BaseResponse msg) {
        for (Role r : roles.values()) {
            r.send(msg);
        }
    }

    /**
     * 大厅玩家广播
     *
     * @param msg             需要广播的消息
     * @param excludeUniqueId 不包含的玩家 uniqueId
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
     * 大厅玩家广播
     *
     * @param msg          需要广播的消息
     * @param self         发给自己的消息
     * @param other        发给桌子其他玩家的消息
     * @param selfUniqueId 自己的uniqueId
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

    private void updateRoomCfg(RoomUpdateRes roomCfg) {
        int roomID = -1;
        if (roomCfg.hasBetRoomCfg()) {
            roomID = roomCfg.getBetRoomCfg().getId();
            if (roomID != 0) {
                Room room = roomMgr.getRoom(roomID);
                if (room == null) {
                    room = roomMgr.createRoom(roomID, roomCfg.getBetRoomCfg());
                } else {
                    room.updateCfg(roomCfg.getBetRoomCfg());
                }
            }
        }
        if (roomCfg.hasFishRoomCfg()) {
            roomID = roomCfg.getFishRoomCfg().getId();
            if (roomID != 0) {
                Room room = roomMgr.getRoom(roomID);
                if (room == null) {
                    room = roomMgr.createRoom(roomID, roomCfg.getFishRoomCfg());
                } else {
                    room.updateCfg(roomCfg.getFishRoomCfg());
                }
                room.updateCfg(roomCfg.getFishRoomCfg());
            }
        }
        if (roomCfg.hasPkRoomCfg()) {
            roomID = roomCfg.getPkRoomCfg().getId();
            if (roomID != 0) {
                Room room = roomMgr.getRoom(roomID);
                if (room == null) {
                    room = roomMgr.createRoom(roomID, roomCfg.getPkRoomCfg());
                } else {
                    room.updateCfg(roomCfg.getPkRoomCfg());
                }
            }
        }
        if (roomID != -1) {
            log.info("站点:{} , 更新房间配置:{} 成功 ", hallId, roomID);
        }
    }

    public void updateGameStorage(long storage) {
        for (Room room : roomMgr.getRooms().values()) {
            room.updateCurrrentStorage(storage, storage);
        }
    }

    public void doStop() {
        roomMgr.doStop();
        onStop();
    }

    public void doDestroy() {
        roomMgr.doDestroy();
        onDestroy();
    }

    public void doTerminate() {
        roomMgr.doTerminate();
        onTerminate();
    }

    public void onStop() {

    }

    public void onDestroy() {

    }

    public void onTerminate() {

    }

    @Override
    public void send2Route(BaseResponse response, Server_Type targetServerType) {
        RouteMgr.send(response, targetServerType, hallId, 0, 0);
    }

    public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
        RouteMgr.send(response, targetServerType, hallId, 0, 0, callback);
    }

    @Override
    public void reply2Route(Request receivedRequest, BaseResponse response) {
        RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
                receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
                receivedRequest.seqId);
    };

}