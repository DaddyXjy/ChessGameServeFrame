package frame.lobby;

import frame.*;
import frame.game.proto.Game.*;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import frame.socket.ErrResponse;
import frame.socket.MessageUtil;
import frame.socket.Request;
import frame.socket.Response;
import frame.socket.common.proto.Error.ErrorRes;
import frame.socket.common.proto.Login.QueryLoginRes;

import java.util.List;

public final class LobbyRoleMgr implements RoleMgrProtocol {
    private HashMap<Long, LobbyPlayer> players = new HashMap<>();
    private @Getter int playerCount;
    private @Getter int robotCount;
    public List<Map> nameMap;
    public String hallId;

    public LobbyPlayer getOrCreatePlayer(long uniqueId, ChannelHandlerContext ctx) {
        try {
            if (players.get(uniqueId) == null) {
                return createPlayer(uniqueId, ctx);
            } else {
                return players.get(uniqueId);
            }
        } catch (Exception e) {
            log.error("获取玩家:{},失败.", uniqueId);
            log.error("StackTrace:", e);
            return null;
        }
    }

    public LobbyPlayer createPlayer(long uniqueId, ChannelHandlerContext ctx) {
        try {
            LobbyPlayer player = LobbyMain.getInstance().getLobbyMgr().createPlayer();
            player.uniqueId = uniqueId;
            player.userId = MessageUtil.getUserID(uniqueId);
            player.siteId = MessageUtil.getSiteID(uniqueId);
            player.setCtx(ctx);
            players.put(uniqueId, player);
            enterHall(player);
            ++playerCount;
            log.info("创建玩家, siteid:{} , userid:{},成功 , 当前服务器人数:{}人", MessageUtil.getSiteID(uniqueId),
                    MessageUtil.getUserID(uniqueId), playerCount);
            return player;
        } catch (Exception e) {
            log.info("创建玩家, siteid:{} , userid:{},失败", MessageUtil.getSiteID(uniqueId),
                    MessageUtil.getUserID(uniqueId));
            log.error("StackTrace:", e);
            return null;
        }
    }

    private void enterHall(LobbyPlayer player) {
        LobbyHall hall = LobbyMain.getInstance().getHallMgr().get(player.siteId);
        if (hall != null) {
            player.enterHall(hall);
        } else {
            log.warn("玩家找不到大厅,userid:{} , siteid:{}", player.userId, player.siteId);
            player.send(new ErrResponse(Config.ERR_CANNOT_FIND_HALL));
            player.exitHall();
            --playerCount;
        }
    }

    public LobbyPlayer getPlayer(long uniqueId) {
        return players.get(uniqueId);
    }

    public LobbyPlayer getPlayer(int siteId, int userId) {
        long uniqueId = MessageUtil.genUniqueId(siteId, userId);
        return players.get(uniqueId);
    };

    public void reconnect(LobbyPlayer player, ChannelHandlerContext ctx) {
        player.setCtx(ctx);
        player.onConnected();
    }

    public boolean connect(long uniqueId, ChannelHandlerContext ctx) {
        LobbyPlayer player = getPlayer(uniqueId);
        if (player == null) {
            player = createPlayer(uniqueId, ctx);
            if (player == null) {
                return false;
            }
        }
        getPlayerDataFromDB(uniqueId, new Callback() {
            @Override
            public void func() {
                LobbyPlayer player = getPlayer(uniqueId);
                if (player != null) {
                    reconnect(player, ctx);
                }
            }
        });
        log.debug("玩家连接成功,siteId:{} , userId:{}", MessageUtil.getSiteID(uniqueId), MessageUtil.getUserID(uniqueId));
        return true;
    }

    public void getPlayerDataFromDB(long uniqueId, Callback callback) {
        int siteId = MessageUtil.getSiteID(uniqueId);
        int userId = MessageUtil.getUserID(uniqueId);
        QueryLoginRes loginRes = QueryLoginRes.newBuilder().setEnterId(Config.GAME_ID).build();
        LobbyMain.getInstance().send2DB(new Response(FrameMsg.GET_DB_USER_INFO, loginRes.toByteArray()), siteId, userId,
                new Callback() {
                    @Override
                    public void func() {
                        LobbyPlayer player = getPlayer(uniqueId);
                        if (player == null) {
                            log.error("获取玩家数据失败:服务器找不到玩家,siteId:{} , userId:{}", siteId, userId);
                            return;
                        }
                        Request req = (Request) this.getData();
                        if (req.isError()) {
                            if (req.isTimeout()) {
                                log.error("获取玩家数据超时,siteId:{} , userId:{}", siteId, userId);
                                player.send(new ErrResponse("获取玩家数据超时"));
                                return;
                            } else {
                                try {
                                    ErrorRes errorRes = ErrorRes.parseFrom(req.protoMsg);
                                    log.warn("获取玩家数据失败:{},siteId:{} , userId:{}", errorRes.getMsg(), siteId, userId);
                                    player.send(new ErrResponse(String.format("获取玩家数据失败:%s", errorRes.getMsg())));
                                } catch (Exception e) {
                                    log.error("error :", e);
                                }
                                return;
                            }
                        }
                        try {
                            GetUserInfoRes userInfo = GetUserInfoRes.parseFrom(req.protoMsg);
                            player.initInfoFromDB(userInfo);
                            player.setInited(true);
                            if (callback != null) {
                                callback.func();
                            }
                        } catch (Exception e) {
                            log.error("初始化玩家数据失败:", e);
                            player.send(new ErrResponse("初始化玩家数据失败"));
                        }
                    }
                });
    }

    public void disconnect(long uniqueId, boolean bKickDb) {

        LobbyPlayer player = getPlayer(uniqueId);
        if (player == null) {
            log.error("玩家掉线请求失败,找不到玩家,siteId:{} , userId:{}", MessageUtil.getSiteID(uniqueId),
                    MessageUtil.getUserID(uniqueId));
        } else {
            if (bKickDb) {
                LobbyMain.getInstance().notifyPlayerLogOut(uniqueId);
            }
            player.onDisconnect();
            player.exitHall();
            --playerCount;
            log.debug("玩家掉线成功,siteId:{} , userId:{} , 当前服务器人数:{}人", MessageUtil.getSiteID(uniqueId),
                    MessageUtil.getUserID(uniqueId), playerCount);
        }
    }

    public void removePlayer(LobbyPlayer player) {
        players.remove(player.uniqueId);
    }

    public void removeAllRobots() {
        // @TODO
    }

    public void removeAllPlayers() {
        // @TODO
    }

    public void doPrepare() {
        // TODO
    }

    public void doDestroy() {
        for (LobbyPlayer role : players.values()) {
            role.doDestroy();
        }
    }

    public void doStop() {
        for (LobbyPlayer role : players.values()) {
            role.doStop();
        }
    }

    public void doTerminate() {
        for (LobbyPlayer role : players.values()) {
            role.doTerminate();
        }
    }

    public void disconnect(ChannelHandlerContext ctx, boolean bKickDb) {
        ArrayList<LobbyPlayer> disconnectPlayers = new ArrayList<LobbyPlayer>();
        for (LobbyPlayer player : players.values()) {
            if (player.getCtx() == ctx) {
                disconnectPlayers.add(player);
            }
        }
        for (LobbyPlayer player : disconnectPlayers) {
            disconnect(player.uniqueId, bKickDb);
        }
        disconnectPlayers.clear();
    }

    protected void onDisconnect() {

    }

    protected void onReconnect() {

    }

}