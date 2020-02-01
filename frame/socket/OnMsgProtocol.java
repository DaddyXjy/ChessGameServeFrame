//Date: 2019/02/28
//Author: dylan
//Desc: 网络消息收发接口

package frame.socket;

import frame.socket.Request;

public interface OnMsgProtocol {
    /**
     * 消息处理
     * 
     * @param request example: if(request.msgType == 1000){ Gate.RegisterG2A_S req =
     *                Gate.RegisterG2A_S.parseFrom(request.protoMsg); int serverId =
     *                req.getServerId() }
     */
    public void onMsg(Request request);
}