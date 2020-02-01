package frame.socket;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import frame.socket.*;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.GateMgr;
import frame.socket.monitor.MonitorMgr;
import frame.socket.route.RouteMgr;
import frame.*;

public class ClientThread {
    public static void connectTo(String url, Server_Type serverType, int serverId)
            throws InterruptedException, UnsupportedEncodingException {
        log.info("------------------");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).group(group)
                    .handler(new LoggingHandler(LogLevel.ERROR)).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new ChannelHandler[] { new HttpClientCodec(),
                                    new HttpObjectAggregator(1024 * 1024 * 10) });
                            p.addLast(new IdleStateHandler(0, 0, 30));
                            p.addLast("Client", new ClientHandler(group, serverType, serverId));
                            p.addLast("HeartBeat", new HeartBeatHandler());
                        }
                    });
            URI webUri = new URI(url);
            WebSocketClientHandshaker shak = WebSocketClientHandshakerFactory.newHandshaker(webUri,
                    WebSocketVersion.V13, (String) null, true, new DefaultHttpHeaders(), 6553600);
            final Channel channel = b.connect(webUri.getHost(), webUri.getPort()).sync().channel();
            ClientHandler handler = (ClientHandler) channel.pipeline().get("Client");
            handler.setHandshaker(shak);
            shak.handshake(channel);
            // handler.handshakeFuture().sync();
        } catch (Exception e) {
            if (serverType == Server_Type.SERVER_TYPE_GATE) {
                GateMgr.onConnectError(serverId);
            } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
                RouteMgr.onConnectError(serverId);
            } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
                MonitorMgr.onConnectError(serverId);
            }
            group.shutdownGracefully().sync();
            log.error("clientThread error", e);
        }
    }
}