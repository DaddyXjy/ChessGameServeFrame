//Date: 2019/04/17
//Author: dylan
//Desc: 监控管理类

package frame.socket.monitor;

import java.util.ArrayList;

import frame.*;
import frame.game.GameMain;
import frame.lobby.LobbyMain;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.HashMap;

import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.GateMgr;
import frame.socket.monitor.proto.Monica.GameServer_State;
import frame.socket.monitor.proto.Monica.GetGameServerInfoRes;
import frame.socket.monitor.proto.Monica.RegisterServerReq;
import frame.socket.monitor.proto.Monica.RegisterServerRes;
import frame.socket.monitor.proto.Monica.ServerErrorLogNotify;
import frame.socket.route.RouteMgr;

public final class MonitorMgr {

    public static ArrayList<ServerItem> serverList = new ArrayList<ServerItem>();

    private static Callback disconnectCallback;

    public static boolean isInit = false;

    // 服务器状态
    public @Setter @Getter static GameServer_State gameServerState = GameServer_State.GAME_SERVER_STATE_NO_ROUTE;

    /**
     * 初始化监控
     */
    public static void init() {
        log.debug("=========开始连接监控========");
        ServerItem serverItem = new ServerItem(0, Server_Type.SERVER_TYPE_GLOBAL, Config.MonitorURL);
        serverList.add(serverItem);
        if (serverItem.isUninit()) {
            serverItem.connect();
        }
    }

    public static ServerItem getServerItemById(int serverId) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.serverId == serverId) {
                return serverItem;
            }
        }
        return null;
    }

    /**
     * 添加监控ctx
     */
    public static void addMonitorCtx(Integer serverId, ChannelHandlerContext ctx) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("MonitorMgr 没有找到对应的server");
            return;
        }
        server.onConnected(ctx);
    }

    /**
     * 新服务器连接
     */
    public static void onServerConnected(Request request) {
        try {
            RegisterServerReq serverReq = RegisterServerReq.parseFrom(request.protoMsg);
            if (serverReq.getType() == Server_Type.SERVER_TYPE_GATE) {
                String url = String.format("ws://%s:%d/se", serverReq.getUrl(), serverReq.getPort());
                log.debug("从监控服务器获取到网关:{}", url);
                Config.GATE_URL_LIST.put(serverReq.getId(), url);
                if (Config.serverType == Server_Type.SERVER_TYPE_GAME) {
                    GateMgr.init();
                }
            } else if (serverReq.getType() == Server_Type.SERVER_TYPE_ROUTE) {
                String url = String.format("ws://%s:%d/ro", serverReq.getUrl(), serverReq.getPort());
                log.debug("从监控服务器获取到路由:{}", url);
                Config.ROUTE_URL_LIST.put(serverReq.getId(),
                        String.format("ws://%s:%d/ro", serverReq.getUrl(), serverReq.getPort()));
                RouteMgr.init();
            }
        } catch (Exception err) {
            log.error("处理注册监控回调失败", err);
        }
    }

    /**
     * 注册监控服务器
     */
    public static void onRegiste2MonitorSuccess(Request request) {
        ServerItem server = getServerItemById(request.serverId);
        if (server == null) {
            log.error("MonitorMgr 没有找到对应的server");
            return;
        }
        server.onRegisted();
        try {
            RegisterServerRes serverRes = RegisterServerRes.parseFrom(request.protoMsg);
            MessageUtil.UniqueServerID = serverRes.getId();
            Config.GATE_URL_LIST.clear();
            Config.ROUTE_URL_LIST.clear();
            for (RegisterServerReq serverReq : serverRes.getServerInfoList()) {
                if (serverReq.getType() == Server_Type.SERVER_TYPE_GATE) {
                    String url = String.format("ws://%s:%d/se", serverReq.getUrl(), serverReq.getPort());
                    log.debug("从监控服务器获取到网关:{}", url);
                    Config.GATE_URL_LIST.put(serverReq.getId(), url);
                } else if (serverReq.getType() == Server_Type.SERVER_TYPE_ROUTE) {
                    String url = String.format("ws://%s:%d/ro", serverReq.getUrl(), serverReq.getPort());
                    log.debug("从监控服务器获取到路由:{}", url);
                    Config.ROUTE_URL_LIST.put(serverReq.getId(),
                            String.format("ws://%s:%d/ro", serverReq.getUrl(), serverReq.getPort()));
                }
            }
            // 初始化网关
            if (Config.serverType == Server_Type.SERVER_TYPE_GAME) {
                GateMgr.init();
            }
            // 初始化路由
            RouteMgr.init();
        } catch (Exception err) {
            log.error("处理注册监控回调失败", err);
        }
    }

    /**
     * 监控断开
     * 
     * @param ctx 通道
     */
    public static void onChanelDisconnect(ChannelHandlerContext ctx) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.ctx == ctx) {
                serverItem.onDisconnected();
                break;
            }
        }
        if (disconnectCallback != null) {
            disconnectCallback.func();
        }
    }

    /**
     * 发送给监控
     * 
     * @param msgType  消息类型
     * @param protoMsg proto数据
     */
    public static void send(Response response) {
        ServerItem server = getServerItemById(0);
        if (server == null) {
            System.err.println("send失败 没有找到对应的 监控 server");
            return;
        }
        if (server.ctx == null) {
            System.err.println("send失败 monitor 通道断开");
            return;
        }
        ByteBuf msgBuf = MessageUtil.packServer2MonitorMsg(response.msgType, response.protoMsg);
        UtilsMgr.getMsgQueue().send(server.ctx, msgBuf);
    }

    /**
     * 注册监控断开回调
     * 
     * @param ctx 通道
     */
    public static void registChanelDisconnect(Callback callback) {
        disconnectCallback = callback;
    }

    /**
     * 监控断开
     * 
     * @param index 服务器索引
     */
    public static void onConnectError(int serverId) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("MonitorMgr 没有找到对应的server");
            return;
        }
        server.onConnectedError();
    }

    /**
     * 发送服务器状态
     */

    public static void sendServerState() {
        GetGameServerInfoRes.Builder getGameServerInfoRes = GetGameServerInfoRes.newBuilder()
                .setGameServerstate(gameServerState);
        ArrayList<ServerItem> targetServerList = new ArrayList<ServerItem>();
        targetServerList.addAll(GateMgr.getRegistedServerList());
        targetServerList.addAll(RouteMgr.getRegistedServerList());

        for (ServerItem server : targetServerList) {
            RegisterServerReq.Builder registerServerReq = RegisterServerReq.newBuilder();
            registerServerReq.setId(server.serverId);
            registerServerReq.setType(server.type);
            registerServerReq.setUrl(server.url);
            getGameServerInfoRes.addServerInfo(registerServerReq.build());
        }
        if (Config.serverType == Server_Type.SERVER_TYPE_GAME) {
            if (Config.GAME_ID == 0) {
                getGameServerInfoRes.setOnlinePlayerNum(LobbyMain.getInstance().getRoleMgr().getPlayerCount());
            } else {
                getGameServerInfoRes.setOnlinePlayerNum(GameMain.getInstance().getRoleMgr().getPlayerCount());
            }
        }
        send(new Response(FrameMsg.MONITOR_GET_SERVER_INFO_A, getGameServerInfoRes.build().toByteArray()));
    }

    /**
     * 更新服务器状态
     */

    public static void updateServerState() {
        if (RouteMgr.getRegistedNum() == 0) {
            gameServerState = GameServer_State.GAME_SERVER_STATE_NO_ROUTE;
            return;
        }
        if (Config.serverType == Server_Type.SERVER_TYPE_GAME) {
            if (GateMgr.getRegistedNum() == 0) {
                gameServerState = GameServer_State.GAME_SERVER_STATE_NO_GATE;
                return;
            }
            if (!UtilsMgr.isSiteDataAcquired()) {
                gameServerState = GameServer_State.GAME_SERVER_STATE_NO_SITE_INFO;
                return;
            }
            if (!UtilsMgr.isRoomDataAcquired()) {
                gameServerState = GameServer_State.GAME_SERVER_STATE_NO_ROOM_INFO;
                return;
            }
            if (!UtilsMgr.isStorageDataAcquired()) {
                gameServerState = GameServer_State.GAME_SERVER_STATE_NO_STORAGE_INFO;
                return;
            }
        }
        if (!UtilsMgr.isServerRun()) {
            gameServerState = GameServer_State.GAME_SERVER_STATE_BOOTING;
            return;
        }
        gameServerState = GameServer_State.GAME_SERVER_STATE_RUNING;
        return;
    }

    /**
     * 心跳
     */
    public static void update() {
        updateServerState();
        for (ServerItem server : serverList) {
            server.update();
        }
    }

    /**
     * 报告错误消息
     */
    public static void reportErrorLog(String log) {
        ServerErrorLogNotify errorLog = ServerErrorLogNotify.newBuilder().setLog(log).build();
        send(new Response(FrameMsg.MONITOR_NOTIFY_SERVER_LOG, errorLog.toByteArray()));
    }

    /**
     * 获取注册的网关
     * 
     */
    public static boolean isRegisted() {
        ArrayList<ServerItem> targetServerList = new ArrayList<ServerItem>();
        for (ServerItem server : serverList) {
            if (server.isRegisted() && server.ctx != null) {
                targetServerList.add(server);
            }
        }
        return targetServerList.size() > 0;
    }

}