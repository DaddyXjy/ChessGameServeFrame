package frame.socket;

import com.alibaba.fastjson.JSON;
import frame.*;
import frame.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import frame.log;

import java.util.Map;

/**
 * @author sam
 * @ClassName: WebSocketFrameHandler
 * @Description: 连接握手断开消息收发处理类
 * @date 2018-07-23
 */
@Sharable
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	int index = 0;

	/**
	 * 消息处理业务线程池核心线程数
	 */
//	private Integer corePoolSize;

	/**
	 * 消息处理业务线程池最大线程数
	 */
//	private Integer maxQueueSize;

	/**
	 * websocket 是否加密
	 */
//	private Integer isEncrypt;

	/**
	 * 接收客户端发过来的消息
	 *
	 * @param ctx 管道对象
	 * @param msg 消息对象
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
		Channel incoming = ctx.channel();
		execute(msg.text(), ctx);

	}

	private void execute(String request, ChannelHandlerContext ctx) {
		// 解密
//		if (isEncrypt == 1) {
//			request = CodeUtils.decode(request);
//		}
		Map map = JSON.parseObject(request, Map.class);
		// UtilsMgr.getMsgQueue().receive(new Request(ctx, map));
	}

	/**
	 * 有客户端来连接第一个执行的方法
	 *
	 * @param ctx 管道对象
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		log.info("用户连接：" + (++index));
		// UtilsMgr.getMsgQueue().receive(new Request(ctx, Config.MSG_TYPE_CONNECT));
	}

	/**
	 * 客户端失去连接执行的第一个方法
	 *
	 * @param ctx 管道对象
	 */
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		brokeConnection(ctx);
	}

	/**
	 * 有客户端来连接第2个执行的方法
	 *
	 * @param ctx 管道对象
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
	}

	/**
	 * 客户端失去连接执行的第二个方法
	 *
	 * @param ctx 管道对象
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
	}

	/**
	 * 连接出现异常执行的方法
	 *
	 * @param ctx   管道对象
	 * @param cause 异常对象
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	/**
	 * 超时机制
	 *
	 * @param ctx 管道对象
	 * @param evt 消息对象
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			switch (e.state()) {
			case READER_IDLE:
				ctx.close();
				break;
			case WRITER_IDLE:
				break;
			case ALL_IDLE:
				break;
			default:
				break;
			}
		}
	}

	/**
	 * websocket 断开操作
	 *
	 * @param ctx
	 */
	private void brokeConnection(ChannelHandlerContext ctx) {
//		log.info("用户掉线：" + (--index));
		// UtilsMgr.getMsgQueue().receive(new Request(ctx, Config.MSG_TYPE_DISCONNECT));
	}
}