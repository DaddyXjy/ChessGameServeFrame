package frame.socket;

import com.alibaba.fastjson.JSON;
import frame.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.concurrent.LinkedBlockingQueue;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.channel.ChannelHandlerContext;

public class MsgQueue {
    private LinkedBlockingQueue<Request> receiveQ1 = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Request> receiveQ2 = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Response> sendQ1 = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Response> sendQ2 = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Request> currentReveiveQ = receiveQ1;
    private LinkedBlockingQueue<Response> currentSendQ = sendQ1;

    public MsgQueue() {

    }

    public void receive(Request o) {
        currentReveiveQ.add(o);
    }

    public void send(PlayerProtocol player, BaseResponse o) {
        if (player.getCtx() != null) {
            // String t2 = JsonUtil.parseJsonString(o);
            String t2 = JSON.toJSONString(o);
            try {
                player.getCtx().channel().eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        player.getCtx().writeAndFlush(new TextWebSocketFrame(t2));
                    }
                });
            } catch (Exception RejectedExecutionException) {
                log.warn("netty发送writeAndFlush被拒绝,放在主线程发送消息", RejectedExecutionException);
                player.getCtx().writeAndFlush(new TextWebSocketFrame(t2));
            }
        }
    }

    public void send(PlayerProtocol player, ByteBuf o) {
        if (player.getCtx() != null) {
            try {
                player.getCtx().channel().eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        player.getCtx().writeAndFlush(new BinaryWebSocketFrame(o));
                    }
                });
            } catch (Exception RejectedExecutionException) {
                log.warn("netty发送writeAndFlush被拒绝,放在主线程发送消息", RejectedExecutionException);
                player.getCtx().writeAndFlush(new TextWebSocketFrame(o));
            }
        }
    }

    public void send(ChannelHandlerContext ctx, ByteBuf o) {
        if (ctx != null) {
            try {
                ctx.channel().eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        ctx.writeAndFlush(new BinaryWebSocketFrame(o));
                    }
                });
            } catch (Exception RejectedExecutionException) {
                log.warn("netty发送writeAndFlush被拒绝,放在主线程发送消息", RejectedExecutionException);
                ctx.writeAndFlush(new TextWebSocketFrame(o));
            }
        }
    }

    public Request get() {
        return currentReveiveQ.poll();
    }

    public boolean allDone() {
        return receiveQ1.size() == 0 && receiveQ2.size() == 0 && sendQ1.size() == 0 && sendQ2.size() == 0;
    }

    public void doPrepare() {

    }

    public void doStop() {
        // TODO
    }

    public void doTerminate() {
        // TODO
    }

    public void doDestroy() {
        // TODO
    }

    public Iterable<Request> getAll() {
        if (currentReveiveQ.size() == 0) {
            return null;
        }
        Iterable<Request> ret = currentReveiveQ;

        LinkedBlockingQueue<Request> otherQ = currentReveiveQ == receiveQ1 ? receiveQ2 : receiveQ1;
        otherQ.clear();
        currentReveiveQ = otherQ;
        return ret;
    }
}