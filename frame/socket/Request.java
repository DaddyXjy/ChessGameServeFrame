package frame.socket;

import frame.Config;
import frame.FrameMsg;
import frame.log;
import frame.socket.common.proto.Error.ErrorRes;
import frame.socket.common.proto.Type.Server_Type;
import io.netty.channel.ChannelHandlerContext;

/**
 * Request
 */
public class Request {
    public ChannelHandlerContext ctx;
    public long millisecond;
    // 消息编号
    public int msgType;
    public int userid = 0;
    public int siteid = 0;
    public int seqId;
    public boolean isdone = false;
    // 数据类型 0 proto 1 json
    public int dataType = 0;
    // 服务器来源
    public Server_Type serverType;
    // 路由相关
    public int targetServerid;
    public int sendType;
    public Server_Type targetServerType;
    public int targetServerSubType = 0;
    public int serverId = 0;
    // proto数据
    public byte[] protoMsg;
    // json数据
    public String jsonMsg;

    public long uniqueId;

    public Request() {
        this.millisecond = System.currentTimeMillis();
        this.isdone = false;
    }

    public boolean isError() {
        return msgType == 0;
    }

    public boolean isTimeout() {
        try {
            if (isError()) {
                ErrorRes err = ErrorRes.parseFrom(protoMsg);
                if (err.getType() == Config.ERR_TIMEOUT.code) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("error parseMsg:", e);
            return false;
        }
        return false;
    }

    public boolean isRouteMsg() {
        return serverType == Server_Type.SERVER_TYPE_ROUTE;
    };

    public boolean isRouteResponse() {
        return serverType == Server_Type.SERVER_TYPE_ROUTE && sendType == 1;
    };

    public void logInfo() {
        log.debug("[网络消息:{}] [类型:{}] [siteId:{}] [userId:{}] [转发服务器:{}] [消息发送端:{}] [服务器子类型:{}]", msgType,
                sendType == 0 ? "请求" : "回复", siteid, userid, getServerTypeText(), getTargetServerTypeText(),
                targetServerSubType);
        if (msgType == FrameMsg.COMMON_ERROR) {
            logErrorDetailInfo();
        }
    }

    // 通用错误消息处理
    public void logErrorDetailInfo() {
        try {
            ErrorRes errorRes = ErrorRes.parseFrom(protoMsg);
            String errorMsg = String.format("siteId:%d , userId:%d , 消息号:%d , 错误类型: %d , 错误消息描述:%s", siteid, userid,
                    errorRes.getCode(), errorRes.getType(), errorRes.getMsg());
            if (errorRes.getCode() > 0) {
                log.info(errorMsg);
                // 网关路由的错误消息,消息号为负
            } else {
                log.error(errorMsg);
            }
        } catch (Exception e) {
            log.error("logErrorDetailInfo:", e);
        }
    }

    public String getServerTypeText() {
        if (serverType == Server_Type.SERVER_TYPE_GATE) {
            return "网关";
        } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
            return "路由";
        } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
            return "监控";
        } else {
            return "未知中转服务";
        }
    }

    public String getTargetServerTypeText() {
        if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
            return "监控";
        } else {
            return targetServerType == null ? "玩家" : targetServerType.toString();
        }
    }
}
