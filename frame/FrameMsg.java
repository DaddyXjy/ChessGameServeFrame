//Date: 2019/03/01
//Author: dylan
//Desc: 框架消息定义
package frame;

public final class FrameMsg {

    // Q 代表请求 A代表回应

    // 网关注册服务器请求
    public final static int REGIST_SERVER_2_GATE_Q = 500001;
    // 网关注册服务器请求(调试)
    public final static int REGIST_SERVER_2_GATE_DEBUG_Q = 500003;
    // 网关注册服务器回应
    public final static int REGIST_SERVER_2_GATE_A = 500002;

    // 玩家登录服务器请求
    public final static int PLAYER_LOGIN_SERVER_Q = 500103;
    // 玩家登录服务器失败回应
    public final static int PLAYER_LOGIN_SERVER_FAIL_A = 500104;
    // 玩家游戏准入请求
    public final static int PLAYER_GAME_PERMIT_Q = 500107;
    // 玩家游戏准入回应
    public final static int PLAYER_GAME_PERMIT_A = 500108;
    // 玩家登出服务器请求
    public final static int PLAYER_LOGOUI_SERVER_Q = 500109;

    public final static int PLAYER_GET_RECORD = 888;

    public final static int DEBUG_MESSAGE = 409024;

    // 网关消息,下限
    public final static int GATE_MSG_LIMIT_DOWN = 500000;
    // 网关消息,上限
    public final static int GATE_MSG_LIMIT_UP = 500999;

    // 路由注册DEBUG请求
    public final static int DEBUG_REGIST_SERVER_2_ROUTE_Q = 503003;
    // 路由注册请求
    public final static int REGIST_SERVER_2_ROUTE_Q = 503001;
    // 路由注册回应
    public final static int REGIST_SERVER_2_ROUTE_A = 503002;

    // 强踢玩家
    public final static int KICK_OUT_PLAYER = 500116;

    // 监控服务器注册请求
    public final static int REGIST_SERVER_2_MONITOR_Q = 900003;
    // 监控服务器注册回应
    public final static int REGIST_SERVER_2_MONITOR_A = 900004;

    // DB获取用户信息
    public final static int GET_DB_USER_INFO = 600005;
    // DB登出处理
    public final static int DB_LOG_OUT = 600105;

    // 监控获取服务器状态
    public final static int MONITOR_GET_SERVER_INFO_Q = 900013;

    // 监控获取服务器状态
    public final static int MONITOR_GET_SERVER_INFO_A = 900014;

    // 监控获取服务器错误日志
    public final static int MONITOR_NOTIFY_SERVER_LOG = 900017;

    // 修改游戏库存
    public final static int MODIFY_GAME_STORAGE = 900020;

    // 通用错误消息
    public final static int COMMON_ERROR = 0;

    // 更新多个站点的游戏房间状态
    public final static int UPDATE_SITE_STATU = 270;
	/**
	 * 修改转盘配置
	 */
	public final static int UPDATA_TURNCONFIG = 80000;
}