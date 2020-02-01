//Date: 2019/02/28
//Author: dylan
//Desc: 网络消息收发接口

package frame.socket;

import frame.Callback;
import frame.socket.BaseResponse;
import frame.socket.Request;
import frame.socket.common.proto.Type.Server_Type;

public interface MsgDealProtocol extends OnMsgProtocol {

    /**
     * 发送消息
     * 
     * @param response
     * @example: Gate.RegisterA2G_R req = Gate.RegisterA2G_R.newBuilder()
     *           .setServerType(Gate.Server_Type.SERVER_TYPE_GAME) .setServerNo(1)
     *           .setSalt("").build(); SuccessResponse response = new
     *           SuccessResponse(1000001 , req.toByteArray()); send(response);
     */
    public void send(BaseResponse response);

    /**
     * 发送消息给路由
     * 
     * @param response
     * @targetServerType: 目标服务器类型
     * @example: Route.RegisterA2G_R req = Route.RegisterA2G_R.newBuilder()
     *           .setServerType(Route.Server_Type.SERVER_TYPE_GAME) .setServerNo(1)
     *           .setSalt("").build(); SuccessResponse response = new
     *           SuccessResponse(1000001 , req.toByteArray()); send(response ,
     *           ServerType.BackGround);
     */
    public void send2Route(BaseResponse response, Server_Type targetServerType);

    /**
     * 发送消息给路由
     * 
     * @param response
     * @targetServerType: 目标服务器类型
     * @callback: 回调消息
     * @example: Route.RegisterA2G_R req = Route.RegisterA2G_R.newBuilder()
     *           .setServerType(Route.Server_Type.SERVER_TYPE_GAME) .setServerNo(1)
     *           .setSalt("").build(); SuccessResponse response = new
     *           SuccessResponse(1000001 , req.toByteArray()); send(response ,
     *           ServerType.BackGround);
     */
    public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback);

    /**
     * 回复消息给路由
     * 
     * @receivedRequest: 收到的请求
     * @response: 回复的消息
     * @example: Route.RegisterA2G_R req = Route.RegisterA2G_R.newBuilder()
     *           .setServerType(Route.Server_Type.SERVER_TYPE_GAME) .setServerNo(1)
     *           .setSalt("").build(); SuccessResponse response = new
     *           SuccessResponse(1000001 , req.toByteArray()); send(response ,
     *           ServerType.BackGround);
     */
    public void reply2Route(Request receivedRequest, BaseResponse response);

}