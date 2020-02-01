//Date: 2019/02/27
//Author: dylan
//Desc: 网关管理类
package frame.socket.gate;

import java.util.ArrayList;

import frame.*;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

import frame.socket.gate.proto.*;
import frame.socket.common.proto.Type.Server_Type;

public final class GateMgr {
    public static ArrayList<ServerItem> serverList = new ArrayList<ServerItem>();

    private static Callback disconnectCallback;

    /**
     * 初始化所有网关
     */
    public static void init() {
        log.debug("=========开始连接网关========");
        int index = 0;
        for (Map.Entry<Integer, String> gate : Config.GATE_URL_LIST.entrySet()) {
            int serverId = gate.getKey();
            String url = gate.getValue();
            ServerItem serverItem = getServerItemById(serverId);
            if (serverItem == null) {
                serverItem = new ServerItem(serverId, Server_Type.SERVER_TYPE_GATE, url);
                serverList.add(serverItem);
                index++;
            }
            if (serverItem.isUninit()) {
                log.info("连接:{}", url);
                serverItem.connect();
            }
        }
        log.debug("=========连接网关结束========");
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
     * 添加网关ctx
     */
    public static void addGateCtx(Integer serverId, ChannelHandlerContext ctx) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("GateMgr 没有找到对应的server");
            return;
        }
        server.onConnected(ctx);
    }

    /**
     * 注册网关服务器
     */
    public static void onRegistGateServer(int serverId) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("GateMgr 没有找到对应的server");
            return;
        }
        server.onRegisted();
    }

    /**
     * 广播给所有网关
     * 
     * @param gateInedx 网关索引
     * @param msgType   消息类型
     * @param protoMsg  proto数据
     */
    public static void broadCast(int msgType, byte[] protoMsg) {
        for (ServerItem server : serverList) {
            if (server.ctx != null) {
                ByteBuf msgBuf = MessageUtil.packServer2GateMsg(msgType, 0, 0, protoMsg);
                UtilsMgr.getMsgQueue().send(server.ctx, msgBuf);
            }
        }
    }

    /**
     * 广播给所有网关
     * 
     * @param siteId    要广播的站点
     * @param gateInedx 网关索引
     * @param msgType   消息类型
     * @param protoMsg  proto数据
     */
    public static void broadCast(int siteId, int msgType, byte[] protoMsg) {
        for (ServerItem server : serverList) {
            if (server.ctx != null) {
                ByteBuf msgBuf = MessageUtil.packServer2GateMsg(msgType, siteId, 0, protoMsg);
                UtilsMgr.getMsgQueue().send(server.ctx, msgBuf);
            }
        }
    }

    /**
     * 发送给单台网关
     * 
     * @param serverId 网关服务器ID
     * @param msgType  消息类型
     * @param protoMsg proto数据
     */
    public static void send(int serverId, Response response) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("send失败 没有找到对应的gateServer");
            return;
        }
        if (server.ctx == null) {
            log.error("send失败 gate 通道断开");
            return;
        }
        ByteBuf msgBuf = MessageUtil.packServer2GateMsg(response.msgType, 0, 0, response.protoMsg);
        UtilsMgr.getMsgQueue().send(server.ctx, msgBuf);
    }

    /**
     * 网关断开
     * 
     * @param ctx 通道
     */
    public static void onChanelDisconnect(ChannelHandlerContext ctx) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.ctx == ctx) {
                serverItem.onDisconnected();
                Config.GATE_URL_LIST.remove(serverItem.serverId);
                serverList.remove(serverItem);
                break;
            }
        }
        if (disconnectCallback != null) {
            disconnectCallback.setData(ctx);
            disconnectCallback.func();
        }
    }

    /**
     * 网关断开
     * 
     * @param serverId 服务器索引
     */
    public static void onConnectError(int serverId) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.serverId == serverId) {
                serverItem.onConnectedError();
                Config.GATE_URL_LIST.remove(serverItem.serverId);
                serverList.remove(serverItem);
                break;
            }
        }
    }

    /**
     * 注册网关断开回调
     * 
     * @param ctx 通道
     */
    public static void registChanelDisconnect(Callback callback) {
        disconnectCallback = callback;
    }

    /**
     * 心跳
     * 
     */
    public static void update() {
        for (ServerItem server : serverList) {
            server.update();
        }
        serverList.removeIf(serverItem -> {
            return serverItem.isExpired();
        });
    }

    /**
     * 获取注册的网关个数
     * 
     */
    public static int getRegistedNum() {
        return getRegistedServerList().size();
    }

    /**
     * 获取注册的网关
     * 
     */
    public static ArrayList<ServerItem> getRegistedServerList() {
        ArrayList<ServerItem> targetServerList = new ArrayList<ServerItem>();
        for (ServerItem server : serverList) {
            if (server.isRegisted() && server.ctx != null) {
                targetServerList.add(server);
            }
        }
        return targetServerList;
    }

}