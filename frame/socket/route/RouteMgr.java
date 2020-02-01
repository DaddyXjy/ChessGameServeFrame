//Date: 2019/03/06
//Author: dylan
//Desc: 路由管理类
package frame.socket.route;

import java.util.ArrayList;

import frame.*;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import frame.socket.gate.proto.*;
import frame.socket.route.proto.Route.RegisterA2R_R;
import frame.socket.route.proto.Route.RegisterDebugA2R_R;
import frame.socket.common.proto.Error.ErrorRes;
import frame.socket.common.proto.Type.Server_Type;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class RouteMgr {

    public static ArrayList<ServerItem> serverList = new ArrayList<ServerItem>();

    public static int routeSendIndex = 0;
    public static int msgIndex = 1;

    public static HashMap<Integer, CompletableFuture<Request>> waits = new HashMap();
    public static HashMap<Integer, Long> waitsTime = new HashMap();
    private static Callback disconnectCallback;

    private static ArrayList<Callback> prepareCallbackList = new ArrayList<Callback>();

    public static boolean isInit = false;

    /**
     * 初始化所有路由
     */
    public static void init() {
        log.debug("=========开始连接路由========");
        int index = 0;
        for (Map.Entry<Integer, String> route : Config.ROUTE_URL_LIST.entrySet()) {
            int serverId = route.getKey();
            String url = route.getValue();
            ServerItem serverItem = getServerItemById(serverId);
            if (serverItem == null) {
                serverItem = new ServerItem(serverId, Server_Type.SERVER_TYPE_ROUTE, url);
                serverList.add(serverItem);
                index++;
            }
            if (serverItem.isUninit()) {
                log.info("连接:{}", url);
                serverItem.connect();
            }
        }
        log.debug("=========连接路由结束========");
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
     * 添加路由ctx
     */
    public static void addRouteCtx(Integer serverId, ChannelHandlerContext ctx) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("RouteMgr 没有找到对应的server");
            return;
        }
        server.onConnected(ctx);
    }

    /**
     * 注册路由服务器
     */
    public static void onRegiste2RouteSuccess(int serverId) {
        ServerItem server = getServerItemById(serverId);
        if (server == null) {
            log.error("RouteMgr 没有找到对应的server");
            return;
        }
        server.onRegisted();

        if (isInit) {
            return;
        }
        isInit = true;
        sendAllPrepare();
    }

    /**
     * 处理请求
     */
    public static void dealReq(Request req) {
        CompletableFuture<Request> future = RouteMgr.waits.remove(req.seqId);
        RouteMgr.waitsTime.remove(req.seqId);
        if (future != null) {
            req.isdone = true;
            future.complete(req);
        }
    }

    /**
     * 请求单台路由: 按轮询顺序
     * 
     * @param response   回复消息
     * @param serverType 服务器类型
     * @param siteid     站点ID
     * @param userid     玩家ID
     */
    public static int send(BaseResponse response, Server_Type serverType, int siteid, int userid) {
        return send(response, serverType, 0, siteid, userid);
    }

    /**
     * 请求单台路由: 按轮询顺序
     * 
     * @param response            回复消息
     * @param serverType          服务器类型
     * @param targetServerSubType 服务器子类型
     * @param siteid              站点ID
     * @param userid              玩家ID
     */
    public static int send(BaseResponse response, Server_Type serverType, int targetServerSubType, int siteid,
            int userid) {
        ByteBuf msgBuf = packMsg(response, MessageUtil.UniqueServerID, siteid, userid, serverType, targetServerSubType,
                0, msgIndex);
        trySend(msgBuf, response.msgType, msgIndex++, null);
        return msgIndex;
    }

    /**
     * 请求单台路由: 按轮询顺序
     * 
     * @param response   回复消息
     * @param serverType 服务器类型
     * @param siteid     站点ID
     * @param userid     玩家ID
     */
    public static int send(BaseResponse response, Server_Type serverType, int siteid, int userid, Callback callback) {
        return send(response, serverType, 0, siteid, userid, callback);
    }

    /**
     * 请求单台路由: 按轮询顺序
     * 
     * @param response            回复消息
     * @param serverType          服务器类型
     * @param targetServerSubType 服务器子类型
     * @param siteid              站点ID
     * @param userid              玩家ID
     */
    public static int send(BaseResponse response, Server_Type serverType, int targetServerSubType, int siteid,
            int userid, Callback callback) {
        ByteBuf msgBuf = packMsg(response, MessageUtil.UniqueServerID, siteid, userid, serverType, targetServerSubType,
                0, msgIndex);
        trySend(msgBuf, response.msgType, msgIndex++, callback);
        return msgIndex;
    }

    /**
     * 请求单台路由: 按轮询顺序(async)
     * 
     * @param response            回复消息
     * @param serverType          服务器类型
     * @param targetServerSubType 服务器子类型
     * @param siteid              站点ID
     * @param userid              玩家ID
     */
    public static CompletableFuture<Request> aSend(BaseResponse response, Server_Type serverType,
            int targetServerSubType, int siteid, int userid) throws InterruptedException {
        log.info("发送 [协程消息:{}] [接收服务器:{}] [siteId:{}] [userId:{}]", response.msgType, serverType.toString(), siteid,
                userid);
        CompletableFuture<Request> future = new CompletableFuture<Request>();
        UtilsMgr.getTaskMgr().createTrigger(new Callback() {
            @Override
            public void func() {
                ArrayList<ChannelHandlerContext> ctxList = getAllActiveCtx();
                if (ctxList.size() == 0) {
                    log.error("消息发往路由失败,没有有效的路由服务器");
                    routeSendIndex = 0;
                    return;
                } else {
                    routeSendIndex = (++routeSendIndex) % (ctxList.size());
                }
                msgIndex++;
                waits.put(msgIndex, future);
                waitsTime.put(msgIndex, new Long(UtilsMgr.millisecond));
                ByteBuf msgBuf = packMsg(response, MessageUtil.UniqueServerID, siteid, userid, serverType,
                        targetServerSubType, 0, msgIndex);
                UtilsMgr.getMsgQueue().send(ctxList.get(routeSendIndex), msgBuf);
            }
        }).fire();

        return future;
    }

    /**
     * 发单台路由: 按轮询顺序(async)
     * 
     * @param response   回复消息
     * @param serverType 服务器类型
     * @param siteid     站点ID
     * @param userid     玩家ID
     */
    public static CompletableFuture<Request> aSend(BaseResponse response, Server_Type serverType, int siteid,
            int userid) throws InterruptedException {
        return aSend(response, serverType, 0, siteid, userid);
    }

    /**
     * 请求单台路由: 指定路由发
     * 
     * @param serverId   路由服务器ID
     * @param response   回复消息
     * @param serverType 服务器类型
     * @param siteid     站点ID
     * @param userid     玩家ID
     */
    public static void send(int serverId, BaseResponse response, Server_Type serverType, int siteid, int userid,
            int sendType) {
        ServerItem serverItem = getServerItemById(serverId);
        if (serverItem.ctx != null) {
            ByteBuf msgBuf = packMsg(response, MessageUtil.UniqueServerID, siteid, userid, serverType, 0, 0,
                    msgIndex++);
            UtilsMgr.getMsgQueue().send(serverItem.ctx, msgBuf);
        }
    }

    /**
     * 回复单台路由: 按轮询顺序
     * 
     * @param msgType  消息类型
     * @param serverID 目标服务器ID
     * @param siteid   站点ID
     * @param userid   玩家ID
     * 
     */
    public static void reply(BaseResponse response, int serverID, Server_Type serverType, int targetServerSubType,
            int siteid, int userid, int msgIndex) {
        UtilsMgr.getTaskMgr().createTrigger(new Callback() {
            @Override
            public void func() {
                ArrayList<ChannelHandlerContext> ctxList = getAllActiveCtx();
                if (ctxList.size() == 0) {
                    log.error("消息发往路由失败,没有有效的路由服务器");
                    return;
                }
                routeSendIndex = (++routeSendIndex) % (ctxList.size());
                ByteBuf msgBuf = packMsg(response, serverID, siteid, userid, serverType, targetServerSubType, 1,
                        msgIndex);
                trySend(msgBuf, response.msgType, -1, null);
            }
        }).fire();
    }

    /**
     * 路由断开
     * 
     * @param ctx 通道
     */
    public static void onChanelDisconnect(ChannelHandlerContext ctx) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.ctx == ctx) {
                serverItem.onDisconnected();
                Config.ROUTE_URL_LIST.remove(serverItem.serverId);
                serverList.remove(serverItem);
                break;
            }
        }
        if (disconnectCallback != null) {
            disconnectCallback.func();
        }
        ArrayList<ChannelHandlerContext> ctxList = getAllActiveCtx();
        isInit = ctxList.size() != 0;
    }

    /**
     * 注册路由断开回调
     * 
     * @param ctx 通道
     */
    public static void registChanelDisconnect(Callback callback) {
        disconnectCallback = callback;
    }

    /**
     * 路由断开
     * 
     * @param serverId 服务器索引
     */
    public static void onConnectError(int serverId) {
        for (ServerItem serverItem : serverList) {
            if (serverItem.serverId == serverId) {
                serverItem.onConnectedError();
                Config.ROUTE_URL_LIST.remove(serverItem.serverId);
                serverList.remove(serverItem);
                break;
            }
        }
    }

    /**
     * 心跳
     */
    public static void update() {
        for (ServerItem server : serverList) {
            server.update();
        }
        serverList.removeIf(serverItem -> {
            boolean isExpired = serverItem.isExpired();
            if (isExpired) {
                if (serverItem.ctx != null) {
                    serverItem.ctx.disconnect();
                    serverItem.ctx = null;
                }
            }
            return isExpired;
        });
        ArrayList<Integer> expiredWaits = new ArrayList<Integer>();
        for (Integer index : waitsTime.keySet()) {
            Long beginTime = waitsTime.get(index);
            if (UtilsMgr.getMillisecond() - beginTime > Config.MSG_TIMEOUT_CO) {
                expiredWaits.add(index);
            }
        }
        for (int expiredIndex : expiredWaits) {
            CompletableFuture<Request> future = RouteMgr.waits.remove(expiredIndex);
            RouteMgr.waitsTime.remove(expiredIndex);
            if (future != null) {
                log.error("协程消息超时! :{}", expiredIndex);
                Request req = new Request();
                req.msgType = 0;
                ErrorRes res = ErrorRes.newBuilder().setMsg("服务器繁忙,消息处理超时").setCode(0).setType(Config.ERR_TIMEOUT.code)
                        .build();
                req.protoMsg = res.toByteArray();
                future.complete(req);
            }
        }

    }

    /**
     * 路由消息组装
     * 
     */
    private static ByteBuf packMsg(BaseResponse response, int serverID, int siteid, int userid, Server_Type serverType,
            int subServerType, int sendType, int msgIndex) {
        ByteBuf msgBuf = Unpooled.buffer(0);
        if (response.dataType == 0) {
            msgBuf = MessageUtil.packServer2RouteMsg(response.msgType, serverID, siteid, userid, serverType,
                    subServerType, sendType, msgIndex, response.protoMsg);
        } else if (response.dataType == 1) {
            msgBuf = MessageUtil.packServer2RouteMsg(response.msgType, serverID, siteid, userid, serverType,
                    subServerType, sendType, msgIndex, response.jsonMsg);
        }
        return msgBuf;
    }

    private static void trySend(ByteBuf msgBuf, int msgType, int scrMsgIndex, Callback callback) {
        if (isInit) {
            ArrayList<ChannelHandlerContext> ctxList = getAllActiveCtx();
            routeSendIndex = (++routeSendIndex) % (ctxList.size());
            UtilsMgr.getMsgQueue().send(ctxList.get(routeSendIndex), msgBuf);
            if (callback != null) {
                CallbackQueue.register(msgType, scrMsgIndex, callback);
            }
        } else {
            prepareCallbackList.add(new Callback() {
                @Override
                public void func() {
                    ArrayList<ChannelHandlerContext> ctxList = getAllActiveCtx();
                    routeSendIndex = (++routeSendIndex) % (ctxList.size());
                    UtilsMgr.getMsgQueue().send(ctxList.get(routeSendIndex), msgBuf);
                    if (callback != null) {
                        CallbackQueue.register(msgType, scrMsgIndex, callback);
                    }
                }
            });
            log.info("路由未初始化完成,发送消息进入等待队列");
        }
    }

    private static void sendAllPrepare() {
        for (Callback callback : prepareCallbackList) {
            callback.func();
        }
        prepareCallbackList.clear();
    }

    private static ArrayList<ChannelHandlerContext> getAllActiveCtx() {
        ArrayList<ChannelHandlerContext> ctxList = new ArrayList<ChannelHandlerContext>();
        for (ServerItem server : serverList) {
            if (server.ctx != null && server.isRegisted()) {
                ctxList.add(server.ctx);
            }
        }
        return ctxList;
    }

    /**
     * 获取注册的路由个数
     * 
     */
    public static int getRegistedNum() {
        return getRegistedServerList().size();
    }

    /**
     * 获取注册的路由
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
