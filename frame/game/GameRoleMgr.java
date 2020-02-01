package frame.game;

import frame.*;
import frame.game.proto.Game.GetUserInfoRes;

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

public final class GameRoleMgr implements RoleMgrProtocol {
    private HashMap<Long, Role> roles = new HashMap<>();
    private @Getter int playerCount;
    private @Getter int robotCount;
    public List<Map> nameMap;
    public String hallId;

    public Player getPlayer(long uniqueId) {
        Player player = (Player) roles.get(uniqueId);
        return player;
    }

    public Player getPlayer(int siteId, int userId) {
        long uniqueId = MessageUtil.genUniqueId(siteId, userId);
        return (Player) roles.get(uniqueId);
    };

    public Player createPlayer(long uniqueId, ChannelHandlerContext ctx) {
        try {
            Player player = GameMain.getInstance().getGameMgr().createPlayer();
            player.uniqueId = uniqueId;
            player.userId = (int) uniqueId;
            player.siteId = (int) (uniqueId >> 32);
            player.setCtx(ctx);
            player.setOnline(true);
            player.setReconnect(false);
            roles.put(uniqueId, player);
            requestPlayerInfo(player);
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

    private void requestPlayerInfo(Player player) {
        GameHall hall = GameMain.getInstance().getHallMgr().get(player.siteId);
        if (hall != null) {
            player.enterHall(hall);
        } else {
            log.error("玩家找不到大厅,player:{} , siteid:{}", player.userId, player.siteId);
            player.send(new ErrResponse(Config.ERR_CANNOT_FIND_HALL));
            player.exitHall();
        }
    }

    public Role getRole(long uniqueId) {
        return roles.get(uniqueId);
    }

    Robot createRobot() {
        Robot robot = GameMain.getInstance().getGameMgr().createRobot();
        ++robotCount;
        robot.init();
        roles.put(robot.uniqueId, robot);
        return robot;
    }

    public void reconnect(Player player, ChannelHandlerContext ctx) {
        player.setCtx(ctx);
        if (player.isOnline()) {
            log.warn("玩家没有退出,重复登入:{}", player.getPlayerInfo());
        }
        player.setOnline(true);
        player.setReconnect(true);
        try {
            if (player.table != null) {
                player.sendUserInfo2Client();
                player.onReconnect();
            } else {
                player.onConnected();
            }
        } catch (Exception err) {
            log.error("重连游戏失败:", err);
            player.send(new ErrResponse("重连游戏失败"));
            player.exitHall();
        }
    }

    public boolean connect(long uniqueId, ChannelHandlerContext ctx) {
        Player player = getPlayer(uniqueId);
        int siteId = MessageUtil.getSiteID(uniqueId);
        int userId = MessageUtil.getUserID(uniqueId);
        if (player == null) {
            player = createPlayer(uniqueId, ctx);
            if (player == null) {
                return false;
            }
            getPlayerDataFromDB(uniqueId, new Callback() {
                @Override
                public void func() {
                    Player player = getPlayer(uniqueId);
                    if (player != null) {
                        player.onConnected();
                    }
                }
            });
        } else {
        	//登陆 或 重连 都要 从数据库拉一次数据 
            getPlayerDataFromDB(uniqueId, new Callback() {
                @Override
                public void func() {
                    Player player = getPlayer(uniqueId);
                    if (player != null) {
                        reconnect(player, ctx);
                    }
                }
            });
        }
        log.info("玩家登入成功:siteId:{} , userId:{}", siteId, userId);
        return true;
    }

    public void getPlayerDataFromDB(long uniqueId, Callback callback) {
        int siteId = MessageUtil.getSiteID(uniqueId);
        int userId = MessageUtil.getUserID(uniqueId);
        QueryLoginRes loginRes = QueryLoginRes.newBuilder().setEnterId(Config.GAME_ID).build();
        GameMain.getInstance().send2DB(new Response(FrameMsg.GET_DB_USER_INFO, loginRes.toByteArray()), siteId, userId,
                new Callback() {
                    @Override
                    public void func() {
                        Player player = getPlayer(uniqueId);
                        if (player == null || callback == null) {
                            log.error("获取玩家数据失败:服务器找不到玩家,siteId:{} , userId:{}", siteId, userId);
                            return;
                        }
                        if(player.isInited()) {
                            callback.func();
                        	log.info("玩家没有退出，不用更新玩家信息");
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
                                    log.error("获取玩家数据失败:{},siteId:{} , userId:{}", errorRes.getMsg(), siteId, userId);
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
                            callback.func();
                        } catch (Exception e) {
                            log.error("初始化玩家失败:", e);
                            player.send(new ErrResponse("初始化玩家失败,请重试!"));
                        }
                    }
                });
    }

    // 玩家掉线判断
    public void disconnect(long uniqueId, boolean bKickDb) {
        log.info("网关发起踢人,玩家:siteId:{} , userId:{}, 是否从DB踢出玩家:{}", MessageUtil.getSiteID(uniqueId),
                MessageUtil.getUserID(uniqueId), bKickDb ? "是" : "否");
        Player player = getPlayer(uniqueId);
        if (player == null) {
            log.error("玩家掉线请求失败,找不到玩家,siteId:{} , userId:{}", MessageUtil.getSiteID(uniqueId),
                    MessageUtil.getUserID(uniqueId));
        } else {
            player.setOnline(false);
            player.setReconnect(false);
            if (!player.isDisconnectKickOut() && player.table != null) {
                log.info("玩家:siteId:{} , userId:{} , 在游戏中掉线,正在等待断线重连..", MessageUtil.getSiteID(uniqueId),
                        MessageUtil.getUserID(uniqueId));
            } else {
                if (bKickDb) {
                    GameMain.getInstance().notifyPlayerLogOut(uniqueId);
                }
                try {
                    player.onDisconnect();
                } catch (Exception e) {
                    log.error("下层游戏 onDisconnect 失败:", e);
                } finally {
                    player.exitHall();
                    log.info("玩家掉线成功,siteId:{} , userId:{}", MessageUtil.getSiteID(uniqueId),
                            MessageUtil.getUserID(uniqueId));
                }
            }
        }
    }

    public void disconnect(ChannelHandlerContext ctx, boolean bKickDb) {
        ArrayList<Player> disconnectPlayers = new ArrayList<Player>();
        for (Role role : roles.values()) {
            if (role instanceof Player) {
                Player player = (Player) role;
                if (player.getCtx() == ctx) {
                    disconnectPlayers.add(player);
                }
            }
        }
        for (Player player : disconnectPlayers) {
            disconnect(player.uniqueId, bKickDb);
        }
        disconnectPlayers.clear();
    }

    void removeRole(Role role) {
        if (role instanceof Player) {
            --playerCount;
            removePlayer((Player) role);
            log.info("玩家退出,当前服务器人数:{}人", playerCount);
        } else {
            removeRobot((Robot) role);
        }
    }

    private void removeRobot(Robot robot) {
        roles.remove(robot.uniqueId);
    }

    private void removePlayer(Player player) {
        roles.remove(player.uniqueId);
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
        for (Role role : roles.values()) {
            role.doDestroy();
        }
    }

    public void doStop() {
        for (Role role : roles.values()) {
            role.doStop();
        }
    }

    public void doTerminate() {
        for (Role role : roles.values()) {
            role.doTerminate();
        }
    }
}