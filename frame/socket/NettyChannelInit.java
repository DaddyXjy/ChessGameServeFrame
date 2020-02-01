package frame.socket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author sam
 * @ClassName: NettyChannelInit
 * @Description: tcp连接初始化，包装成WebSocket连接，并设置连接通道的各种配置
 * @date 2018-07-23
 */
public class NettyChannelInit extends ChannelInitializer<SocketChannel> {


    /**
     * 对超时时间
     */
    private Integer readerIdleTime;

    /**
     * 初始化管道，添加websocket协议，添加超时机制
     * 
     * @param ch channel 对象
     */
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        pipeline.addLast(new IdleStateHandler(1500, 1500, 1500, TimeUnit.SECONDS));
        pipeline.addLast(new WebSocketFrameHandler());

    }
}