
// Date: 2019/03/14
// Author: dylan
// Desc: 回调队列
package frame.socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import frame.Callback;
import frame.Config;
import frame.UtilsMgr;
import frame.log;
import frame.socket.common.proto.Error.ErrorRes;

public final class CallbackQueue {

    // 回调队列
    private static HashMap<Integer, Callback> callbackQueue = new HashMap();
    // 等待时间
    public static HashMap<Integer, Long> waitsTime = new HashMap();
    // 消息类型队列
    public static HashMap<Integer, Integer> msgTypeQueue = new HashMap();

    private static ReentrantLock lock = new ReentrantLock();

    /**
     * 处理请求
     */
    public static void register(Integer msgType, Integer msgIndex, Callback callback) {
        lock.lock();
        callbackQueue.put(msgIndex, callback);
        waitsTime.put(msgIndex, UtilsMgr.millisecond);
        msgTypeQueue.put(msgIndex, msgType);
        lock.unlock();
    }

    /**
     * 处理请求
     */
    public static void dealReq(Request req) {
        Callback callback = callbackQueue.remove(req.seqId);
        waitsTime.remove(req.seqId);
        msgTypeQueue.remove(req.seqId);
        if (callback != null) {
            callback.setData(req);
            req.isdone = true;
            callback.func();
        } else {
            log.error("回复消息未被处理,回复时间过长已做超时处理,或没有写回调函数,msgType:{},seqId:{}", req.msgType, req.seqId);
        }
    }

    public static void update() {
        ArrayList<Integer> expiredWaits = new ArrayList<Integer>();
        for (Integer index : waitsTime.keySet()) {
            Long beginTime = waitsTime.get(index);
            if (UtilsMgr.getMillisecond() - beginTime > Config.MSG_TIMEOUT_CB) {
                log.info("超时消息seqId:{}, beginTime:{} , nowTime:{}", index, beginTime, UtilsMgr.getMillisecond());
                expiredWaits.add(index);
            }
        }
        for (int expiredIndex : expiredWaits) {
            Callback callback = callbackQueue.remove(expiredIndex);
            waitsTime.remove(expiredIndex);
            int msgType = msgTypeQueue.remove(expiredIndex);
            if (callback != null) {
                log.warn("回调消息处理超时! , msgType:{}", msgType);
                Request req = new Request();
                req.msgType = 0;
                ErrorRes res = ErrorRes.newBuilder().setMsg(String.format("服务器繁忙,消息:%d处理超时", msgType)).setCode(0)
                        .setType(Config.ERR_TIMEOUT.code).build();
                req.protoMsg = res.toByteArray();
                req.isdone = true;
                callback.setData(req);
                callback.func();
            }
        }

    }
}