package frame.socket;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import frame.Config;
import frame.log;
import frame.socket.Request;
import frame.socket.common.proto.Type;
import frame.socket.common.proto.Type.Server_Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ThreadLocalRandom;

public class MessageUtil {
    public static final int gate2ServerHeadSize = 16;
    public static final int server2GateHeadSize = 20;

    public static final int route2ServerHeadSize = 28;
    public static final int server2RouteHeadSize = 28;

    public static final int monitorHeadSize = 8;

    // 服务器唯一ID
    public static int UniqueServerID = 0;

    public static long genUniqueId(int siteid, int userid) {
        return ((long) siteid << 32) | userid;
    }

    public static int getUserID(long uniqueID) {
        return (int) uniqueID;
    }

    public static int getSiteID(long uniqueID) {
        return (int) (uniqueID >> 32);
    }

    /**
     * S2G 服务器消息头数据流打包
     */
    public static void packServer2GateMsgHeadBuf(ByteBuf buf, int msgType, int sitid, int userid) {
        // 顺序不能随便调换,网关需要做字节对齐
        // 厅主标识
        buf.writeInt(sitid);
        // 用户标识
        buf.writeInt(userid);
        // 广播
        buf.writeByte(0);
        // 预留
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(0);
        // 协议编号
        buf.writeInt(msgType);
    }

    /**
     * S2G 服务器数据流打包
     */
    public static ByteBuf packServer2GateMsg(int msgType, int sitid, int userid, byte[] proto) {
        int size = proto.length;
        int broadNum = 0;
        ByteBuf buf = Unpooled.buffer(size + server2GateHeadSize + broadNum * 32);
        packServer2GateMsgHeadBuf(buf, msgType, sitid, userid);
        // proto长度
        buf.writeInt(size);
        buf.writeBytes(proto);
        return buf;
    }

    /**
     * G2S 服务器数据流拆包
     */
    public static Request unpackGate2ServerMsg(ByteBuf buf) {
        // 顺序不能随便调换,网关需要做字节对齐`
        Request request = new Request();
        request.siteid = buf.readInt();
        request.userid = buf.readInt();
        request.msgType = buf.readInt();
        request.uniqueId = genUniqueId(request.siteid, request.userid);
        int size = buf.readInt();
        request.protoMsg = new byte[size];
        buf.readBytes(request.protoMsg);
        return request;
    }

    /**
     * S2R 服务器消息头数据流打包
     */
    public static void packServer2RouteMsgHeadBuf(ByteBuf buf, int msgType, int serverID, int sitid, int userid,
            Server_Type serverType, int subServerType, int sendType, int msgIndex, int dataType) {
        // 厅主标识
        buf.writeInt(sitid);
        // 玩家ID
        buf.writeInt(userid);
        // 服务器ID
        buf.writeInt(serverID);
        // 发送类型 0请求 1回复
        buf.writeByte(sendType);
        // 目标服务器类型
        buf.writeByte(serverType.getNumber());
        // 目标服务器子类型
        buf.writeByte(subServerType);
        // 额外字段
        buf.writeByte(dataType);
        buf.writeInt(msgIndex);
        // 协议编号
        buf.writeInt(msgType);
    }

    /**
     * S2R 服务器数据流打包(proto)
     */
    public static ByteBuf packServer2RouteMsg(int msgType, int serverID, int sitid, int userid, Server_Type serverType,
            int subServerType, int sendType, int msgIndex, byte[] proto) {
        int size = proto.length;
        ByteBuf buf = Unpooled.buffer(size + server2RouteHeadSize);
        packServer2RouteMsgHeadBuf(buf, msgType, serverID, sitid, userid, serverType, subServerType, sendType, msgIndex,
                0);
        // 长度
        buf.writeInt(size);
        buf.writeBytes(proto);
        return buf;
    }

    /**
     * S2R 服务器数据流打包(proto)
     */
    public static ByteBuf packServer2RouteMsg(int msgType, int serverID, int sitid, int userid, Server_Type serverType,
            int subServerType, int sendType, int msgIndex, String json) {
        int size = json.length();
        ByteBuf buf = Unpooled.buffer(size + server2RouteHeadSize);
        packServer2RouteMsgHeadBuf(buf, msgType, serverID, sitid, userid, serverType, subServerType, sendType, msgIndex,
                1);
        // 长度
        buf.writeInt(size);
        buf.writeCharSequence(json, StandardCharsets.UTF_8);
        return buf;
    }

    /**
     * R2S 服务器数据流拆包(消息头)
     */
    public static void unpackRoute2ServerMsgHeadBuf(Request request, ByteBuf buf) {
        request.siteid = buf.readInt();
        request.userid = 0;
        request.userid = buf.readInt();
        request.uniqueId = genUniqueId(request.siteid, request.userid);
        request.targetServerid = buf.readInt();
        request.sendType = buf.readByte();
        request.targetServerType = Server_Type.forNumber(buf.readByte());
        request.targetServerSubType = buf.readByte();
        request.dataType = buf.readByte();
        request.seqId = buf.readInt();
        request.msgType = buf.readInt();
    }

    /**
     * R2S 服务器数据流拆包
     */
    public static Request unpackRoute2ServerMsg(ByteBuf buf) {
        Request request = new Request();
        unpackRoute2ServerMsgHeadBuf(request, buf);
        int size = buf.readInt();
        if (request.dataType == 0) {
            request.protoMsg = new byte[size];
            buf.readBytes(request.protoMsg);
        } else if (request.dataType == 1) {
            request.jsonMsg = buf.readCharSequence(size, StandardCharsets.UTF_8).toString();
        }
        return request;
    }

    /**
     * S2M 监控消息打包
     */
    public static ByteBuf packServer2MonitorMsg(int msgType, byte[] proto) {
        // 协议编号
        int size = proto.length;
        ByteBuf buf = Unpooled.buffer(size + monitorHeadSize);
        buf.writeInt(msgType);
        // 长度
        buf.writeInt(size);
        buf.writeBytes(proto);
        return buf;
    }

    /**
     * M2S 监控消息拆包
     */
    public static Request unpackMonitor2ServerMsg(ByteBuf buf) {
        // 协议编号
        Request request = new Request();
        request.msgType = buf.readInt();
        int size = buf.readInt();
        request.protoMsg = new byte[size];
        buf.readBytes(request.protoMsg);
        return request;
    }

    public static Message.Builder getBuildByname(String className) {
        try {
            Class cl = null;
            cl = Class.forName(className);
            Method method = cl.getMethod("newBuilder");
            return (Message.Builder) (method.invoke(null, new Object[] {}));
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }
}
