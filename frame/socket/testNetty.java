package frame.socket;

import java.nio.channels.spi.SelectorProvider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

public class testNetty{
    public static void satrSever(int prot) throws InterruptedException{
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        EventLoopGroup boos=new NioEventLoopGroup(2,new DefaultThreadFactory("server1", true));
        //int num=Runtime.getRuntime().availableProcessors();
        EventLoopGroup work =new NioEventLoopGroup(4,new DefaultThreadFactory("server2", true),SelectorProvider.provider());
        serverBootstrap.group(boos, work);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.handler(new LoggingHandler(LogLevel.ERROR));
        serverBootstrap.childHandler(new NettyChannelInit());
        serverBootstrap.bind(prot).sync().channel().closeFuture().sync();
    }
}