package frame;

import java.lang.*;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;

public interface PlayerProtocol extends MsgDealProtocol {
    boolean getInited();

    public ChannelHandlerContext getCtx();

    public void setCtx(ChannelHandlerContext ctx);

    /**
     * 玩家连接到服务器的回调 业务层在这里处理,连接进来的玩家,回复给玩家的消息
     */
    public void onConnected();

    /**
     * 玩家断线重连到服务器的回调 业务层在这里处理,断线重连进来的玩家,回复给玩家的消息
     */
    public void onReconnect();

    /**
     * 玩家断线回调
     */
    public void onDisconnect();

    public void getPlayerHistory(Request req);

    public void onBaseMsg(Request req);

}