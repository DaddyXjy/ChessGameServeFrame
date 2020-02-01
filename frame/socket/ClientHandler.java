package frame.socket;

import frame.Callback;
import frame.UtilsMgr;
import frame.log;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import frame.socket.*;
import frame.socket.common.proto.Type.Server_Type;
import frame.socket.gate.*;
import frame.socket.monitor.MonitorMgr;
import frame.socket.route.*;

public class ClientHandler extends SimpleChannelInboundHandler<Object> {
    WebSocketClientHandshaker handshaker;
    ChannelPromise handshakeFuture;
    int serverId;
    Server_Type serverType;
    EventLoopGroup eventLoopGroup;

    public ClientHandler(EventLoopGroup eventLoopGroup, Server_Type type, int serverId) {
        super();
        this.serverType = type;
        this.serverId = serverId;
        this.eventLoopGroup = eventLoopGroup;
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public WebSocketClientHandshaker getHandshaker() {
        return handshaker;
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("netty,通道激活");
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        try {
            log.error("netty,通道断开");
            UtilsMgr.getTaskMgr().createTrigger(new Callback() {
                @Override
                public void func() {
                    if (serverType == Server_Type.SERVER_TYPE_GATE) {
                        GateMgr.onChanelDisconnect(ctx);
                    } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
                        RouteMgr.onChanelDisconnect(ctx);
                    } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
                        MonitorMgr.onChanelDisconnect(ctx);
                    }
                }
            }).fire();
        } catch (Exception e) {
            log.error("netty,通道断开 eventLoopGroup shutdown error :", e);
        } finally {
            if (this.eventLoopGroup != null) {
                this.eventLoopGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("netty,通道异常:", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("netty,通道不活跃");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        FullHttpResponse response;
        log.debug("monitor channelRead0");
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse) msg;
                this.handshaker.finishHandshake(ch, response);
                this.handshakeFuture.setSuccess();
                log.debug("monitor handshaker");
                UtilsMgr.getTaskMgr().createTrigger(new Callback() {
                    @Override
                    public void func() {
                        if (serverType == Server_Type.SERVER_TYPE_GATE) {
                            GateMgr.addGateCtx(serverId, ctx);
                        } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
                            RouteMgr.addRouteCtx(serverId, ctx);
                        } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
                            log.debug("添加monitor ctx");
                            MonitorMgr.addMonitorCtx(serverId, ctx);
                        }
                    }
                }).fire();
            } catch (Exception e) {
                log.error("握手失败:", e);
                FullHttpResponse res = (FullHttpResponse) msg;
                String errorMsg = String.format("WebSocket Client failed to connect,status:%s,reason:%s", res.status(),
                        res.content().toString(CharsetUtil.UTF_8));
                this.handshakeFuture.setFailure(new Exception(errorMsg));
                // 握手失败
                UtilsMgr.getTaskMgr().createTrigger(new Callback() {
                    @Override
                    public void func() {
                        if (serverType == Server_Type.SERVER_TYPE_GATE) {
                            GateMgr.onConnectError(serverId);
                        } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
                            RouteMgr.onConnectError(serverId);
                        } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
                            MonitorMgr.onConnectError(serverId);
                        }
                    }
                }).fire();
            }
        } else if (msg instanceof FullHttpResponse) {
            response = (FullHttpResponse) msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                    + response.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame;
                ByteBuf buf = binFrame.content();
                if (serverType == Server_Type.SERVER_TYPE_GATE) {
                    if (buf.readableBytes() >= MessageUtil.gate2ServerHeadSize) {
                        Request req = MessageUtil.unpackGate2ServerMsg(buf);
                        req.ctx = ctx;
                        req.serverType = Server_Type.SERVER_TYPE_GATE;
                        req.serverId = serverId;
                        UtilsMgr.getMsgQueue().receive(req);
                    } else {
                        log.error("接收到网关的非法数据:消息长度小于:{} Byte", MessageUtil.gate2ServerHeadSize);
                    }
                } else if (serverType == Server_Type.SERVER_TYPE_ROUTE) {
                    if (buf.readableBytes() >= MessageUtil.route2ServerHeadSize) {
                        Request req = MessageUtil.unpackRoute2ServerMsg(buf);
                        req.ctx = ctx;
                        req.serverId = serverId;
                        req.serverType = Server_Type.SERVER_TYPE_ROUTE;
                        UtilsMgr.getMsgQueue().receive(req);
                    }
                } else if (serverType == Server_Type.SERVER_TYPE_GLOBAL) {
                    if (buf.readableBytes() >= MessageUtil.monitorHeadSize) {
                        Request req = MessageUtil.unpackMonitor2ServerMsg(buf);
                        req.ctx = ctx;
                        req.serverId = serverId;
                        req.serverType = Server_Type.SERVER_TYPE_GLOBAL;
                        UtilsMgr.getMsgQueue().receive(req);
                    }
                } else {
                    log.error("接收到监控服务器的非法数据:消息长度小于:{} Byte", MessageUtil.monitorHeadSize);
                }
            } else if (frame instanceof PongWebSocketFrame) {
                log.debug("WebSocket Client received pong");
            } else if (frame instanceof CloseWebSocketFrame) {
                log.error("WebSocket Client received CloseWebSocketFrame");
                ch.close();
            }
        }
    }
}