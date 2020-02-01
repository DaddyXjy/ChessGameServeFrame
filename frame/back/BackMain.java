package frame.back;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;

import frame.Call;
import frame.CallRegisterMgr;
import frame.Callback;
import frame.Config;
import frame.FrameMsg;
import frame.MultiCallMgr;
import frame.TaskMgr;
import frame.UtilsMgr;
import frame.log;
import frame.socket.MsgQueue;
import frame.socket.Request;
import frame.socket.BaseResponse;
import frame.socket.CallbackQueue;
import frame.socket.ErrorMsg.ErrorRes;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.monitor.MonitorMgr;
import frame.socket.monitor.proto.Monica.AdjustStorage;
import frame.socket.route.RouteMgr;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class BackMain {
    public BackMain() {
        instance = this;
    }

    private static @Getter BackMain instance;
    private @Getter long millisecond;
    private @Getter long lastDestroyTime;
    private long lastUpdate;
    private @Getter long delta;

    public enum Status {
        END, START, RUN, STOP, TERMINATE
    }

    protected @Getter Status status = Status.END;

    protected abstract void onStart();

    protected void onReadyRun() {
        this.setStatus(Status.RUN);
    }

    protected void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
            UtilsMgr.setServerRun(this.status == Status.RUN);
        }
    }

    protected void start() {
        setServerType();
        UtilsMgr.setTaskMgr(new TaskMgr());
        UtilsMgr.setCallRegisterMgr(new CallRegisterMgr());
        UtilsMgr.setMultiCallMgr(new MultiCallMgr());
        UtilsMgr.setMsgQueue(new MsgQueue());
        onStart();
        setStatus(Status.START);
        prepare();
    }

    private void prepare() {
        UtilsMgr.getMultiCallMgr().start();
        MonitorMgr.init();
    }

    private void step() {
        if (status == Status.START) {
            MonitorMgr.update();
            RouteMgr.update();
            UtilsMgr.getTaskMgr().update();
            onReadyRun();
        } else {
            MonitorMgr.update();
            RouteMgr.update();
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
        }
    }

    // 处理接受的消息
    private void dealRequests() {
        Iterable<Request> it = UtilsMgr.getMsgQueue().getAll();
        if (it != null) {
            for (Request req : it) {
                req.logInfo();
                // 通用错误消息处理
                if (req.msgType == FrameMsg.COMMON_ERROR) {
                    try {
                        ErrorRes errorRes = ErrorRes.parseFrom(req.protoMsg);
                        log.error("siteId:{} , userId:{} , 消息号:{} , 错误类型: {} , 错误消息描述:{}", req.siteid, req.userid,
                                errorRes.getCode(), errorRes.getType(), errorRes.getMsg());
                    } catch (Exception e) {
                        log.error("error :", e);
                    }
                }
                if (req.msgType == FrameMsg.REGIST_SERVER_2_ROUTE_A) {
                    RouteMgr.onRegiste2RouteSuccess(req.serverId);
                    continue;
                } else if (req.msgType == FrameMsg.REGIST_SERVER_2_MONITOR_A) {
                    MonitorMgr.onRegiste2MonitorSuccess(req);
                    continue;
                } else if (req.msgType == FrameMsg.REGIST_SERVER_2_MONITOR_Q) {
                    MonitorMgr.onServerConnected(req);
                    continue;
                } else if (req.msgType == FrameMsg.MONITOR_GET_SERVER_INFO_Q) {
                    MonitorMgr.sendServerState();
                    continue;
                } else if (req.msgType == FrameMsg.MODIFY_GAME_STORAGE) {
                    try {
                        AdjustStorage adjustStorage = AdjustStorage.parseFrom(req.protoMsg);

                    } catch (Exception e) {
                        log.error("AdjustStorage error :", e);
                    }
                    continue;
                }

                // 路由应答,走async
                if (req.sendType == 1) {
                    RouteMgr.dealReq(req);
                    if (!req.isdone) {
                        CallbackQueue.dealReq(req);
                    }
                } else {
                    Call call = new Call();
                    call.setCall(new Callback() {
                        @Override
                        public void func() {
                            onMsg(req);
                        }
                    });
                    UtilsMgr.getMultiCallMgr().call(call);
                }
            }
        }
    }

    /**
     * 非应答的消息全部在这里处理
     * 
     * @request: 其他服务器请求
     */
    protected abstract void onMsg(Request request);

    /**
     * 设置服务器类型
     * 
     * example: Config.serverType = Server_Type.SERVER_TYPE_GAME
     */
    protected abstract void setServerType();

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
     * 发送请求(协程式)
     * 
     * @response: 回应的消息
     * @serverType: 服务器类型
     * @subServerType: 服务器子类型
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public CompletableFuture<Request> aSend(BaseResponse response, Server_Type serverType, int subServerType,
            int siteid, int userid) {
        try {
            return RouteMgr.aSend(response, serverType, subServerType, siteid, userid);
        } catch (Exception e) {
            log.error("aSend2Lobby error:", e);
            return completedFuture(null);
        }
    }

    /**
     * 发送请求给游戏服务器
     * 
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
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     * @callback: 回调函数
     */
    public void send2Game(BaseResponse response, int gameID, int siteid, int userid, Callback callback) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid);
    }

    /**
     * 发送请求给游戏服务器与游戏DB
     * 
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2GameAndDB(BaseResponse response, int gameID, int siteid, int userid) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid);
        RouteMgr.send(response, Server_Type.SERVER_TYPE_DATABASE, gameID, siteid, userid);
    }

    /**
     * 发送请求给游戏服务器与游戏DB
     * 
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     * @callback: 回调函数
     */
    public void send2GameAndDB(BaseResponse response, int gameID, int siteid, int userid, Callback callback) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid);
        RouteMgr.send(response, Server_Type.SERVER_TYPE_DATABASE, gameID, siteid, userid);
    }

    /**
     * 发送请求给游戏服务器(协程式)
     * 
     * @response: 回应的消息
     * @gameID: 游戏ID
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public CompletableFuture<Request> aSend2Game(BaseResponse response, int gameID, int siteid, int userid) {
        try {
            return RouteMgr.aSend(response, Server_Type.SERVER_TYPE_GAME, gameID, siteid, userid);
        } catch (Exception e) {
            log.error("aSend2Lobby error:", e);
            return completedFuture(null);
        }
    }

    /**
     * 发送请求给游戏大厅
     * 
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
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2Lobby(BaseResponse response, int siteid, int userid, Callback callback) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_GAME, 0, siteid, userid, callback);
    }

    /**
     * 发送请求给游戏大厅(协程式)
     * 
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public CompletableFuture<Request> aSend2Lobby(BaseResponse response, int siteid, int userid) {
        try {
            return RouteMgr.aSend(response, Server_Type.SERVER_TYPE_GAME, 0, siteid, userid);
        } catch (Exception e) {
            log.error("aSend2Lobby error:", e);
            return completedFuture(null);
        }
    }

    /**
     * 发送请求给数据库
     * 
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
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public void send2DB(BaseResponse response, int siteid, int userid, Callback callback) {
        RouteMgr.send(response, Server_Type.SERVER_TYPE_DATABASE, 0, siteid, userid, callback);
    }

    /**
     * 发送请求给数据库(协程式)
     * 
     * @response: 回应的消息
     * @siteid: 站点ID
     * @userid: 用户ID
     */
    public CompletableFuture<Request> aSend2DB(BaseResponse response, int siteid, int userid) {
        try {
            return RouteMgr.aSend(response, Server_Type.SERVER_TYPE_DATABASE, 0, siteid, userid);
        } catch (Exception e) {
            log.error("aSend2DB error:", e);
            return completedFuture(null);
        }
    }

    /**
     * 回应请求(请求消息,直接回应)
     * 
     * @receivedRequest: 其他服务器的请求
     * @response: 回应的消息
     */
    public void reply(Request receivedRequest, BaseResponse response) {
        RouteMgr.reply(response, receivedRequest.targetServerid, receivedRequest.targetServerType,
                receivedRequest.targetServerSubType, receivedRequest.siteid, receivedRequest.userid,
                receivedRequest.seqId);
    }

    protected void doDestroy() {
        UtilsMgr.getTaskMgr().doDestroy();
        UtilsMgr.getMsgQueue().doDestroy();
        onDestroy();

    }

    protected void onDestroy() {

    }

    /**
     * 框架入口函数
     * 
     * @args: 启动应用程序的命令行参数
     */
    public void run(String[] args) {
        Config.MAX_CALL_THREAD = 100;
        Config.loadEnv(args);
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
}
