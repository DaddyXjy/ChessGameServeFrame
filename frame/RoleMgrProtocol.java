package frame;

import io.netty.channel.ChannelHandlerContext;

public interface RoleMgrProtocol {
    void doPrepare();

    void doStop();

    void doTerminate();

    void doDestroy();

    public PlayerProtocol getPlayer(long uniqueId);

    public PlayerProtocol getPlayer(int siteId, int userId);

    public boolean connect(long uniqueId, ChannelHandlerContext ctx);

    public void disconnect(long uniqueId, boolean bKickDb);

    // 网关断开
    public void disconnect(ChannelHandlerContext ctx, boolean bKickDb);
}