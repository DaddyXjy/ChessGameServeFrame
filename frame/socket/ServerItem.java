//Date: 2019/04/06
//Author: dylan
//Desc: 服务器类

package frame.socket;

import frame.Callback;
import frame.Config;
import frame.FrameMsg;
import frame.UtilsMgr;
import frame.log;
import frame.socket.ClientThread;
import frame.socket.MessageUtil;
import frame.socket.Response;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.GateMgr;
import frame.socket.gate.proto.Gate;
import frame.socket.monitor.MonitorMgr;
import frame.socket.monitor.proto.Monica.RegisterServerReq;
import frame.socket.route.RouteMgr;
import frame.socket.route.proto.Route.RegisterDebugA2R_R;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

public class ServerItem {
    public enum Status {
        Uninit, Connecting, Connected, Registed, Disconnected
    }

    // 服务器地址
    public String url;

    // 服务器类型
    public Server_Type type;

    public ChannelHandlerContext ctx;

    public Status status;

    public int index;

    public long connectTime = 0;

    private boolean isReconnecting = false;

    public int serverId;

    public ServerItem(int serverId, Server_Type type, String url) {
        this.type = type;
        this.url = url;
        this.status = Status.Uninit;
        this.serverId = serverId;
        this.ctx = null;
        this.connectTime = 0;
    }

    public void connect() {
        try {
            if (this.ctx != null) {
                this.ctx.disconnect();
            }
            this.status = Status.Connecting;
            this.isReconnecting = false;
            ClientThread.connectTo(url, type, serverId);
        } catch (Exception e) {
            log.error("server connect error", e);
            onConnectedError();
        }
    }

    public boolean isRegisted() {
        return this.status == Status.Registed;
    }

    public boolean isUninit() {
        return this.status == Status.Uninit;
    }

    public boolean canConnect() {
        return this.status == Status.Uninit || this.status == Status.Disconnected;
    }

    public void registe() {
        if (type == Server_Type.SERVER_TYPE_GATE) {
            Gate.RegisterDebugA2G_R req = Gate.RegisterDebugA2G_R.newBuilder().setId(MessageUtil.UniqueServerID)
                    .setType(Config.serverType)
                    // 服务器编号,大厅是0,其他的以游戏ID区分
                    .setSubType(Config.GAME_ID).setSiteId(Config.SITE_ID).build();
            Response response = new Response(FrameMsg.REGIST_SERVER_2_GATE_DEBUG_Q, req.toByteArray());
            GateMgr.send(serverId, response);
        } else if (type == Server_Type.SERVER_TYPE_ROUTE) {
            int siteID = Config.serverType == Server_Type.SERVER_TYPE_GAME ? Config.SITE_ID : 0;
            RegisterDebugA2R_R req = RegisterDebugA2R_R.newBuilder().setId(MessageUtil.UniqueServerID)
                    .setType(Config.serverType).setSiteId(siteID).setSubType(Config.GAME_ID).build();
            Response response = new Response(FrameMsg.DEBUG_REGIST_SERVER_2_ROUTE_Q, req.toByteArray());
            RouteMgr.send(serverId, response, Server_Type.SERVER_TYPE_ROUTE, 0, 0, 0);
        } else if (type == Server_Type.SERVER_TYPE_GLOBAL) {
            String localHostUrl = "";
            int siteID = Config.serverType == Server_Type.SERVER_TYPE_GAME ? Config.SITE_ID : 0;
            RegisterServerReq.Builder reqBuilder = RegisterServerReq.newBuilder();
            reqBuilder.setId(MessageUtil.UniqueServerID);
            reqBuilder.setType(Config.serverType);
            reqBuilder.setSubType(Config.GAME_ID);
            reqBuilder.setSiteId(siteID);
            reqBuilder.setUrl(localHostUrl);
            reqBuilder.setPort(0);
            reqBuilder.setVersion(Config.version);
            Response response = new Response(FrameMsg.REGIST_SERVER_2_MONITOR_Q, reqBuilder.build().toByteArray());
            MonitorMgr.send(response);
        } else {
            log.error("注册未知的服务器类型");
        }
    }

    public void onConnected(ChannelHandlerContext ctx) {
        this.status = Status.Connected;
        this.ctx = ctx;
        this.connectTime = UtilsMgr.getMillisecond();
        registe();
        log.info("成功连接{}:{} , 开始发起注册", getName(), url);
    }

    public boolean isExpired() {
        if (this.status == Status.Connected) {
            if (UtilsMgr.getMillisecond() - this.connectTime > 10 * 1000) {
                return true;
            }
        }
        return false;
    }

    public void onRegisted() {
        this.status = Status.Registed;
        log.debug("成功注册{}:{} , 已经正常运转", getName(), url);
    }

    public void onDisconnected() {
        this.status = Status.Disconnected;
        if (this.ctx != null) {
            this.ctx.disconnect();
        }
        this.ctx = null;
        this.isReconnecting = false;
        log.info("{}断开连接:{}", getName(), url);
    }

    public void onConnectedError() {
        this.status = Status.Disconnected;
        if (this.ctx != null) {
            this.ctx.disconnect();
        }
        this.ctx = null;
        this.isReconnecting = false;
        log.info("{}没有连接上:{}", getName(), url);
    }

    public String getName() {
        if (type == Server_Type.SERVER_TYPE_GATE) {
            return "网关";
        } else if (type == Server_Type.SERVER_TYPE_ROUTE) {
            return "路由";
        } else if (type == Server_Type.SERVER_TYPE_GLOBAL) {
            return "监控";
        }
        return "未知";
    }

    public void update() {
        if (type == Server_Type.SERVER_TYPE_GLOBAL) {
            if (this.status == Status.Disconnected && !isReconnecting) {
                isReconnecting = true;
                UtilsMgr.getTaskMgr().createTimer(5, new Callback() {
                    @Override
                    public void func() {
                        connect();
                    }
                });
            }
        }
    }
}