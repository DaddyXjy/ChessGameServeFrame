package frame;
import frame.socket.*;
public interface BroadCastProtocol
{
    
    /**
     * 玩家广播
     *
     * @param response 
    */
    public void broadcast(BaseResponse response);

    /**
     * 玩家广播
     * 
     * @param response 
     * @param excludeUniqueId 不包含的玩家 uniqueId
    */
    public void broadcast(BaseResponse response , long excludeUniqueId);

    /**
     * 玩家广播
     *
     * @param selfResponse 
     * @param otherResponse 
     * @param selfUniqueId 自己的uniqueId
    */
    public void broadcast(BaseResponse selfResponse , BaseResponse otherResponse, long selfUniqueId);
        
}