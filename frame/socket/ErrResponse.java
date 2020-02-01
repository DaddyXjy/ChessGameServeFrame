package frame.socket;

import java.util.Map;

import frame.Config;
import frame.socket.common.proto.Error.ErrorRes;

public class ErrResponse extends BaseResponse {
    // 错误协议编号和客户端已约定好: 为0
    private final static int ERROR_MSG_TYPE = 0;

    /**
     * @param msg : 返回的出错消息
     */
    public ErrResponse(String msg) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(msg).setCode(0).setType(0).build();
        this.protoMsg = res.toByteArray();
    }

    /**
     * @param errMsgType: 对应哪条消息出错
     */
    public ErrResponse(int errMsgType) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg("").setCode(0).setType(errMsgType).build();
        this.protoMsg = res.toByteArray();
    }

    /**
     * @param errMsgType: 对应哪条消息出错
     * @param msg         : 返回的出错消息
     */

    public ErrResponse(int errMsgType, String msg) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(msg).setCode(errMsgType).setType(0).build();
        this.protoMsg = res.toByteArray();
    }

    /**
     * @param errMsgType:   对应哪条消息出错
     * @param errStatusType : 出错类型
     * @param msg           : 返回的出错消息
     */

    public ErrResponse(int errMsgType, int errStatusType, String msg) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(msg).setCode(errMsgType).setType(errStatusType).build();
        this.protoMsg = res.toByteArray();
    }

    public ErrResponse(Config.Error error) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(error.msg).setCode(error.code).setType(0).build();
        this.protoMsg = res.toByteArray();
    }

    /**
     * @param errMsgType: 对应哪条消息出错
     */
    public ErrResponse(int errMsgType, Config.Error error) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(error.msg).setCode(error.code).setType(0).build();
        this.protoMsg = res.toByteArray();
    }

    /**
     * @param errMsgType:   对应哪条消息出错
     * @param errStatusType : 出错类型
     */
    public ErrResponse(int errMsgType, int errStatusType, Config.Error error) {
        this.msgType = ERROR_MSG_TYPE;
        ErrorRes res = ErrorRes.newBuilder().setMsg(error.msg).setCode(error.code).setType(0).build();
        this.protoMsg = res.toByteArray();
    }

}