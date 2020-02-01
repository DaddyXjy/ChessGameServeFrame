package frame.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import frame.*;

public class HeartBeatHandler extends CustomHeartbeatHandler {
    public HeartBeatHandler() {
        super("HeartBeat");
    }

    @Override
    protected void handleData(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        byte[] data = new byte[byteBuf.readableBytes() - 5];
        byteBuf.skipBytes(5);
        byteBuf.readBytes(data);
        String content = new String(data);
        log.debug("{} get content: {}", name, content);
    }

    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        log.debug("---READER_IDLE---");
        super.handleReaderIdle(ctx);
        sendPingMsg(ctx);
    }

    @Override
    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        log.debug("---WRITER_IDLE---");
        super.handleWriterIdle(ctx);
        sendPingMsg(ctx);
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        super.handleAllIdle(ctx);
        sendPingMsg(ctx);
    }
}