//总管基类

package frame;

import frame.game.*;
import frame.socket.*;
import frame.socket.common.proto.Error.ErrorRes;
import frame.socket.common.proto.Login.UserLogoutNotify;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.*;
import lombok.Getter;
import lombok.Setter;
import frame.socket.gate.proto.*;
import frame.socket.gate.proto.Login.KickNotify;
import frame.socket.gate.proto.Login.LogoutNotifyG2LY_N;
import frame.socket.monitor.MonitorMgr;
import frame.socket.monitor.proto.Monica.AdjustStorage;
import frame.socket.route.RouteMgr;
import frame.FrameMsg.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import frame.util.*;
import frame.socket.gate.proto.*;

public abstract class BaseMain implements OnMsgProtocol {

    public enum Status {
        END, START, RUN, STOP, TERMINATE
    };

    protected @Getter Status status = Status.END;

    protected RoleMgrProtocol roleMgr;
    protected HallMgrProtocol hallMgr;

    private @Getter long millisecond;
    private @Getter long lastDestroyTime;
    private long lastUpdate;
    private @Getter long delta;
    private boolean callReady = false;
    private boolean isPrepare = false;

    protected void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
            UtilsMgr.setServerRun(this.status == Status.RUN);
        }
    }

    private void prepare() {
        this.hallMgr.init(new Callback() {
            @Override
            public void func() {
                roleMgr.doPrepare();
                hallMgr.doPrepare();
                UtilsMgr.getTaskMgr().doPrepare();
                UtilsMgr.getMsgQueue().doPrepare();
                onPrepare();
            }
        });

        GateMgr.registChanelDisconnect(new Callback() {
            @Override
            public void func() {
                ChannelHandlerContext ctx = (ChannelHandlerContext) this.getData();
                roleMgr.disconnect(ctx, true);
                log.warn("网关断开连接");
            }
        });
        isPrepare = true;
    }

    private void register() {
        Register.calls();
        UtilsMgr.getMultiCallMgr().start();
    }

    protected void start() {
        setRoleMgr();
        setHallMgr();
        UtilsMgr.setTaskMgr(new TaskMgr());
        UtilsMgr.setCallRegisterMgr(new CallRegisterMgr());
        UtilsMgr.setMultiCallMgr(new MultiCallMgr());
        UtilsMgr.setMsgQueue(new MsgQueue());

        register();
        onStart();
        MonitorMgr.init();
        setStatus(Status.START);
    }

    protected abstract void setHallMgr();

    protected abstract void setRoleMgr();

    protected abstract void onStart();

    protected abstract void onReadyRun();

    public void onMsg(Request request) {
        int msgType = request.msgType;
        // byte[] protoMsg = request.protoMsg;
        try {
            if (msgType == FrameMsg.REGIST_SERVER_2_GATE_A) {
                GateMgr.onRegistGateServer(request.serverId);
            } else if (msgType == FrameMsg.REGIST_SERVER_2_ROUTE_A) {
                RouteMgr.onRegiste2RouteSuccess(request.serverId);
            } else if (msgType == FrameMsg.REGIST_SERVER_2_MONITOR_A) {
                MonitorMgr.onRegiste2MonitorSuccess(request);
            } else if (msgType == FrameMsg.REGIST_SERVER_2_MONITOR_Q) {
                MonitorMgr.onServerConnected(request);
            } else if (msgType == FrameMsg.MONITOR_GET_SERVER_INFO_Q) {
                MonitorMgr.sendServerState();
            } else if (msgType == FrameMsg.MODIFY_GAME_STORAGE) {
                try {
                    AdjustStorage adjustStorage = AdjustStorage.parseFrom(request.protoMsg);
                    request.siteid = adjustStorage.getSiteId();
                    dealHalRequest(request);
                } catch (Exception e) {
                    log.error("AdjustStorage error :", e);
                }
            }
        } catch (Exception e) {
            log.error("BaseMain onMsgError:", e);
        }
    }

    protected void onPrepare() {
    }

    private void step() {
        if (status == Status.START) {
            GateMgr.update();
            RouteMgr.update();
            MonitorMgr.update();
            CallbackQueue.update();
            dealRequests();
            UtilsMgr.getTaskMgr().update();
            if (RouteMgr.getRegistedNum() > 0 && !isPrepare) {
                prepare();
            }
            onReadyRun();
        } else {
            GateMgr.update();
            RouteMgr.update();
            MonitorMgr.update();
            CallbackQueue.update();
            UtilsMgr.profileDebug(new Callback() {
                @Override
                public void func() {
                    dealRequests();
                }
            }, "BaseMain:dealRequests()", Config.RATE);
            UtilsMgr.profileDebug(new Callback() {
                @Override
                public void func() {
                    UtilsMgr.getTaskMgr().update();
                }
            }, "UtilsMgr.getTaskMgr().update()", Config.RATE);
            UtilsMgr.profileDebug(new Callback() {
                @Override
                public void func() {
                    hallMgr.update();
                }
            }, "hallMgr.update()", Config.RATE);
        }

        if (status == Status.STOP) {
            if (hallMgr.canTerminate()) {
                terminate();
            }
        } else if (status == Status.TERMINATE) {
            if ((UtilsMgr.getTaskMgr().allDone() && UtilsMgr.getMsgQueue().allDone())
                    || millisecond >= lastDestroyTime) {
                setStatus(Status.END);
            }
        }
    }

    public Boolean isRunning() {
        return status == Status.RUN;
    }

    // 停机步骤1：挂维护，不再创建新房间
    public void stop() {
        if (status == Status.STOP || status == Status.TERMINATE || status == Status.END) {
            return;
        }

        setStatus(Status.STOP);
        doStop();
    }

    // 停机步骤2：发送终止消息，做最后挣扎
    public void terminate() {
        if (status == Status.TERMINATE || status == Status.END) {
            return;
        }

        lastDestroyTime = millisecond + Config.MAX_DESTROY_TIME;
        setStatus(Status.TERMINATE);

        doTerminate();
    }

    protected void doStop() {

        hallMgr.doStop();
        roleMgr.doStop();
        UtilsMgr.getTaskMgr().doStop();
        UtilsMgr.getMsgQueue().doStop();
        onStop();
    }

    protected void doTerminate() {

        hallMgr.doTerminate();
        roleMgr.doTerminate();
        UtilsMgr.getTaskMgr().doTerminate();
        UtilsMgr.getMsgQueue().doTerminate();
        onTerminate();

    }

    // 停机步骤3：心跳骤停，死亡横线---------------
    protected void doDestroy() {

        roleMgr.doDestroy();
        hallMgr.doDestroy();
        UtilsMgr.getTaskMgr().doDestroy();
        UtilsMgr.getMsgQueue().doDestroy();
        onDestroy();

    }

    protected void onStop() {

    }

    protected void onTerminate() {

    }

    protected void onDestroy() {

    }

    private void dealPlayerRequests(Request req) {
        try {
            if (millisecond - req.millisecond > Config.MSG_TIMEOUT) {
                log.error("消息处理超时: siteid :{} , userid:{} , msgType:{}", req.siteid, req.userid, req.msgType);
                if (req.ctx != null) {
                    PlayerProtocol r = (PlayerProtocol) roleMgr.getPlayer(req.uniqueId);
                    if (r != null) {
                        r.send(new ErrResponse(Config.ERR_SERVER_BUSY));
                    }
                }
                return;
            }
            // 玩家登入请求
            if (req.msgType == FrameMsg.PLAYER_LOGIN_SERVER_Q) {
                boolean success = roleMgr.connect(req.uniqueId, req.ctx);
                if (!success) {
                    Login.LoginFailureLY2G_S resProto = Login.LoginFailureLY2G_S.newBuilder().build();
                    Response res = new Response(FrameMsg.PLAYER_LOGIN_SERVER_FAIL_A, resProto.toByteArray());
                    send(res, req.ctx, req.siteid, req.userid);
                    log.info("玩家登入失败:siteid :{} , userid:{}", req.siteid, req.userid);
                }
                return;
            }
            // 玩家下线
            else if (req.msgType == FrameMsg.PLAYER_LOGOUI_SERVER_Q) {
                try {
                    LogoutNotifyG2LY_N logoutNotifyG2LY_N = LogoutNotifyG2LY_N.parseFrom(req.protoMsg);
                    roleMgr.disconnect(req.uniqueId, logoutNotifyG2LY_N.getLogoutDB());
                } catch (Exception e) {
                    log.error("玩家下线失败:", e);
                }
                return;
            }
            PlayerProtocol player = roleMgr.getPlayer(req.uniqueId);
            if (player == null) {
                if(Config.NotFoundResponseSuccessCodes.contains(req.msgType)){
                    reply(req, new Response(10000));
                    return;
                }
                log.error("消息处理异常:{}, 找不到玩家: siteid :{} , userid:{}", req.msgType, req.siteid, req.userid);
                // 强踢玩家
                KickNotify kicknotify = KickNotify.newBuilder()
                        .setReason(KickNotify.KickNotifyReason.KICKNOTIFY_REASON_NOT_EXIST).build();
                send(new Response(FrameMsg.KICK_OUT_PLAYER, kicknotify.toByteArray()), req.ctx, req.siteid, req.userid);
                if (req.isRouteMsg()) {
                    reply(req, new ErrResponse(-20000, "游戏服务器找不到玩家"));
                }
                return;
            }

            // 未初始化玩家消息回流
            if (!player.getInited()) {
                UtilsMgr.getMsgQueue().receive(req);
                return;
            }
            UtilsMgr.profileDebug(new Callback() {
                @Override
                public void func() {
                    player.onBaseMsg(req);
                }
            }, req.msgType, Config.RATE / 2);

        } catch (Exception err) {
            log.error("消息处理错误: siteid :{} , userid:{} , msgType:{}", req.siteid, req.userid, req.msgType);
            log.error("StackTrace:", err);
        }
    }

    private void dealControlRequest(Request req) {
        try {
            onMsg(req);
        } catch (Exception err) {
            log.error("消息处理错误: msgType:{}", req.msgType);
            log.error("StackTrace:", err);
        }
    }

    private void dealHalRequest(Request req) {
        try {
            hallMgr.onMsg(req);
        } catch (Exception err) {
            log.error("消息处理错误: msgType:{}", req.msgType);
            log.error("StackTrace:", err);
        }
    }

    // 处理接受的消息
    private void dealRequests() {
        Iterable<Request> it = UtilsMgr.getMsgQueue().getAll();

        if (it != null) {
            for (Request req : it) {
                req.logInfo();
                // 路由回复的消息走这里
                if (req.isRouteResponse()) {
                    CallbackQueue.dealReq(req);
                }
                if (!req.isdone) {
                    if (req.userid == 0 && req.siteid == 0) {
                        dealControlRequest(req);
                    } else if (req.userid == 0 && req.siteid != 0) {
                        dealHalRequest(req);
                    } else {
                        dealPlayerRequests(req);
                    }
                }
            }
        }
    }

    /**
     * 框架入口函数
     * 
     * @argv: 启动应用程序的命令行参数
     */
    public void run(String[] argv) {
        Config.loadEnv(argv);
        start();
        while (status != Status.END) {
            millisecond = System.currentTimeMillis();
            UtilsMgr.setMillisecond(millisecond);
            long delta = lastUpdate == 0 ? 0 : (millisecond - lastUpdate);
            UtilsMgr.setDelta(delta);
            try {
                step();
            } catch (Exception err) {
                log.error("服务器未知错误！！！ stackTrace:", err);
            }
            lastUpdate = millisecond;
            long sleepTime = Config.RATE - (System.currentTimeMillis() - millisecond);
            if (sleepTime < 0) {
                log.debug("当前逻辑帧处理超时,用时:{} ms", Config.RATE - sleepTime);
            } else {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    log.error("thread.sleep error:", e);
                }
            }
        }

        doDestroy();
    }

    public void send(BaseResponse response, ChannelHandlerContext ctx, int siteId, int userId) {
        ByteBuf msgBuf = MessageUtil.packServer2GateMsg(response.msgType, siteId, userId, response.protoMsg);
        UtilsMgr.getMsgQueue().send(ctx, msgBuf);
    }

    /**
     * 发送请求
     * 
     * @response: 回应的消息
     * @serverType: 服务器类型
     * @subServerType: 服务器子类型
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send(BaseResponse response, Server_Type serverType, int subServerType, int siteid, int userid) {
        RouteMgr.send(response, serverType, subServerType, siteid, userid);
    }

    /**
     * 发送请求
     * 
     * @response: 回应的消息
     * @serverType: 服务器类型
     * @subServerType: 服务器子类型
     * @siteid: 站点ID
     * @userid: 用户ID
     * @callback: 回调函数
     */
    public void send(BaseResponse response, Server_Type serverType, int subServerType, int siteid, int userid,
            Callback callback) {
        RouteMgr.send(response, serverType, subServerType, siteid, userid, callback);
    }

    /**
     * 发送请求给游戏服务器
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Game(BaseResponse response, int gameID, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid);
    }

    /**
     * 发送请求给游戏服务器
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     * @callback: 回调函数
     */
    public void send2Game(BaseResponse response, int gameID, int siteid, int userid, Callback callback) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid, callback);
    }

    /**
     * 发送请求给游戏大厅
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Lobby(BaseResponse response, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, 0, siteid, userid);
    }

    /**
     * 发送请求给游戏大厅
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Lobby(BaseResponse response, int siteid, int userid, Callback callback) {
        int msgIndex = RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, 0, siteid, userid, callback);
    }

    /**
     * 发送请求给数据库
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2DB(BaseResponse response, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_DATABASE, 0, siteid, userid);
    }

    /**
     * 发送请求给数据库
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2DB(BaseResponse response, int siteid, int userid, Callback callback) {
        int msgIndex = RouteMgr.send(response, Server_Type.SERVER_TYPE_DATABASE, 0, siteid, userid, callback);
    }

    /**
     * 发送请求给游戏记录服务器
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Record(BaseResponse response, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_RECORD, 0, siteid, userid);
    }

    /**
     * 发送请求给游戏记录服务器
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Record(BaseResponse response, int siteid, int userid, Callback callback) {
        int msgIndex = RouteMgr.send(response, Server_Type.SERVER_TYPE_RECORD, 1, siteid, userid, callback);
    }

    /**
     * 发送请求给游戏记录服务器
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Account(BaseResponse response, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_ACCOUNT, 1, siteid, userid);
    }

    /**
     * 发送请求给数据库
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Account(BaseResponse response, int siteid, int userid, Callback callback) {
        int msgIndex = RouteMgr.send(response, Server_Type.SERVER_TYPE_ACCOUNT, 0, siteid, userid, callback);
    }

    /*
     * 回应请求(请求消息,直接回应)
     * 
     * @receivedRequest: 其他服务器的请求
     * 
     * @response: 回应的消息
     */
    public void reply(Request receivedRequest, BaseResponse response) {
        RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
                receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
                receivedRequest.seqId);
    }

    public void notifyPlayerLogOut(long uniqueId) {
        UserLogoutNotify userLogoutNotify = UserLogoutNotify.newBuilder().setEnterId(Config.GAME_ID).build();
        send2DB(new Response(FrameMsg.DB_LOG_OUT, userLogoutNotify.toByteArray()), MessageUtil.getSiteID(uniqueId),
                MessageUtil.getUserID(uniqueId));
    }

}