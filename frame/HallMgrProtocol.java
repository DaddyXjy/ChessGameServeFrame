package frame;
import java.lang.*;
import frame.socket.*;
 
public interface HallMgrProtocol extends OnMsgProtocol
{
    public void init(Callback callbackOut);
    public void update();

    void doPrepare();

    void doStop(); 

    void doTerminate();

    void doDestroy(); 

    boolean canTerminate();

    boolean isReady();
    /**
     * 消息处理
     * @param request
     * example:
     * if(request.msgType == 1000){
     *      Gate.RegisterG2A_S req = Gate.RegisterG2A_S.parseFrom(request.protoMsg);
     *      int serverId = req.getServerId()
     * }
    */
    public void onMsg(Request request);
}