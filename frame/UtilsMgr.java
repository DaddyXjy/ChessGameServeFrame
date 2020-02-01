//Date: 2019/02/25
//Author: dylan
//Desc: 工具管理类
package frame;

import lombok.Getter;
import lombok.Setter;
import frame.socket.*;
import io.netty.channel.ChannelHandlerContext;

public final class UtilsMgr {
    public static @Setter @Getter TaskMgr taskMgr;
    public static @Setter @Getter CallRegisterMgr callRegisterMgr;
    public static @Setter @Getter MultiCallMgr multiCallMgr;
    public static @Setter @Getter long millisecond;
    public static @Setter @Getter MsgQueue msgQueue;
    public static @Setter @Getter long delta;
    public @Setter @Getter ChannelHandlerContext ctx;

    public static @Setter @Getter boolean siteDataAcquired = false;
    public static @Setter @Getter boolean roomDataAcquired = false;
    public static @Setter @Getter boolean storageDataAcquired = false;
    public static @Setter @Getter boolean serverRun = false;

    /**
     * 性能测试
     * 
     * @Callback 待测试的方法
     * @funcInfo 方法信息
     * @timeOut 超时时间
     */
    public static void profileDebug(Callback profileFunc, String funcInfo, long timeOut) {
        if (Config.DEBUG) {
            long millisecond = System.currentTimeMillis();
            profileFunc.func();
            long deltaTime = System.currentTimeMillis() - millisecond;
            if (timeOut < deltaTime) {
                log.debug("任务:{} , 处理超时,用时:{} ms", funcInfo, deltaTime);
            }
        } else {
            profileFunc.func();
        }
    };

    public static void profileDebug(Callback profileFunc, Object funcInfo, long timeOut) {
        profileDebug(profileFunc, funcInfo.toString(), timeOut);
    }
}
