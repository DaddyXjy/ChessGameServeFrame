package frame;

import com.beust.jcommander.JCommander;
import frame.game.Robot;
import frame.socket.common.proto.Type.Server_Type;
import frame.util.Args;
import frame.util.VersionReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Config {
    // 更新频率
    public final static long RATE = 1000 / 120;
    // 消息处理超时时间
    public final static long MSG_TIMEOUT = 1000 * 5;
    // 协程消息处理超时时间
    public final static long MSG_TIMEOUT_CO = 1000 * 10;
    // 回调消息处理超时时间
    public final static long MSG_TIMEOUT_CB = 1000 * 10;

    // 是否开放体验房
    public static boolean OPEN_FREE_ROOM = true;

    public static float ROOM_CREATE_TIME = 30.0f; // 单一桌子类型的房间的启动时间延迟
    public static int MAX_CALL_THREAD = 10; // 最大多任务线程数
    public final static int CALL_TIME_OUT = 1000 * 10; // 任务默认超时时间
    public final static int MAX_DESTROY_TIME = 1000 * 60; // 最长强制关机时间
    public final static float AUTO_UPDATE_CONFIG_TIME = 3; // 拉取配置间隔时间
    public final static float CHECK_SHUTDOWN_TIME = 60; // 检测关机
    public final static int MSG_TYPE_CONNECT = -100;
    public final static int MSG_TYPE_DISCONNECT = -101;

    public static int GAME_ID = 0; // 0为大厅
    public static int SITE_ID = 0; // 当前站点

    public static HashMap<Integer, String> GATE_URL_LIST = new HashMap<Integer, String>(); // 网关列表
    public static HashMap<Integer, String> ROUTE_URL_LIST = new HashMap<Integer, String>(); // 网关列表
    public static String MonitorURL = "";

    public static String RedisHost = "";

    public static Server_Type serverType = Server_Type.SERVER_TYPE_GAME; // 当前服务器类型
    public static HashSet<Integer> NotFoundResponseSuccessCodes = new HashSet<>(Arrays.asList(600603));

    // 金额放大倍率(服务器内部逻辑不需要这个值,只有客户端关心金额值具体的含义)
    // (只有服务器打日志的时候需要这个值)
    public static int MONEY_RATIO = 1000;

    public static double MONEY_RATIO_FLOAT = 1000.0;

    // 是否为调试模式
    public static boolean DEBUG = false;
    // 是否打开测试CMD
    public static boolean OPEN_CMD = false;
    // 调试单个站点
    public static int DEBUG_SITE_ID = 36979;
    // 调试单个房间
    public static int DEBUG_ROOM_ID = 0;

    // 是否允许自动重连
    public static boolean ALLOW_AUTO_CONNECT = true;

    // 获取版本
    public static Integer version = 0;
    /**
     * 读取redis是否要密码
     */
    public static boolean passWord = false;

    // 加载环境
    public static void loadEnv(String[] argv) {
        Args args = new Args();
        try {
            JCommander.newBuilder().addObject(args).build().parse(filterArgs(argv));
        } catch (Exception e) {
            log.error("参数解析错误", e);
        }
        if (argv.length == 0) {
            SITE_ID = 36979;
            MonitorURL = "ws://172.20.101.9:9999/ws";
            ROOM_CREATE_TIME = 1.0f;
            Config.DEBUG = true;
            Config.OPEN_CMD = true;
            RedisHost = "127.0.0.1";
        } else if (argv[0].equals("dev")) {
        } else if (argv[0].equals("test")) {
            SITE_ID = args.siteID;
            MonitorURL = args.monitorUrl;
            ROOM_CREATE_TIME = 1.0f;
            Config.DEBUG = true;
            Config.OPEN_CMD = true;
            RedisHost = "172.20.100.207";
        } else if (argv[0].equals("prerelease")) {
            SITE_ID = args.siteID;
            MonitorURL = args.monitorUrl;
            Config.OPEN_CMD = true;
            RedisHost = "172.20.100.151";
            Config.OPEN_FREE_ROOM = true;
        } else if (argv[0].equals("prod")) {
            SITE_ID = args.siteID;
            MonitorURL = args.monitorUrl;
            Config.OPEN_CMD = true;
        } else if (argv[0].equals("release")) {
            SITE_ID = args.siteID;
            MonitorURL = args.monitorUrl;
            Config.OPEN_CMD = false;
            RedisHost = "r-3nsmp6t6hbvb2pbe75.redis.rds.aliyuncs.com";
            Config.OPEN_FREE_ROOM = true;
            Config.passWord = true;
        }  else if (argv[0].equals("bugfix")) {
            SITE_ID = args.siteID;
            MonitorURL = args.monitorUrl;
            Config.OPEN_CMD = false;
            RedisHost = "172.20.100.183";
            Config.OPEN_FREE_ROOM = true;
        } else {
            log.error("启动参数错误,argv[0] = {}", argv[0]);
        }
        version = VersionReader.read("/version.dat");
        log.info("jar version:{}", version);
    }

    public static String[] filterArgs(String[] argv) {
        ArrayList<String> filteredList = new ArrayList<String>();
        int count = 0;
        for (int i = 0; i < argv.length; i++) {
            if (!argv[i].startsWith("--spring") && !argv[i].startsWith("test") && !argv[i].startsWith("prerelease")
                    && !argv[i].startsWith("release") && !argv[i].startsWith("opedit")
                    && !argv[i].startsWith("bugfix")) {
                filteredList.add(argv[i]);
                count++;
            }
        }
        return filteredList.toArray(new String[count]);
    }

    public static class Error {
        public int code;
        public String msg;

        public Error(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }
    }

    public final static int ERR_TYPE_CODE = 9999;
    public final static int ERR_COMMON_CODE = 9999;

    public final static Error ERR_FAILURE = new Error(-1, "未知服务器错误");
    public final static Error ERR_PAIR_FAILURE = new Error(-2, "匹配桌子失败");
    public final static Error ERR_PAIR_CREATE_ERROR = new Error(-3, "创建桌子失败");
    public final static Error ERR_PAIR_TABLE_STATUS_ERROR = new Error(-4, "桌子状态错误");
    public final static Error ERR_STOP = new Error(-5, "停服维护中");
    public final static Error ERR_PAIR_DESTORY = new Error(-5, "房间已销毁");
    public final static Error ERR_ROOM_NOT_EXIST = new Error(-6, "房间不存在");
    public final static Error ERR_TABLE_FULL = new Error(-7, "房间已满员");
    public final static Error ERR_SERVER_BUSY = new Error(-8, "服务器繁忙，请稍后再试");
    public final static Error ERR_TABLE_DESTORY = new Error(-9, "游戏已经结束");
    public final static Error ERR_CANNOT_FIND_HALL = new Error(-10, "找不到游戏大厅");
    public final static Error ERR_TABLE_NOT_OPEN = new Error(-11, "桌子未开启");
    public final static Error ERR_TABLE_PLAYER_NOT_ENOUGH = new Error(-12, "桌子玩家人数未达到游戏人数要求");
    public final static Error ERR_GEN_ROBOT = new Error(-13, "机器人创建失败");
    public final static Error ERR_CREATE_TABLE = new Error(-14, "桌子启动失败,房间人数超过房间最大人数限制");
    public final static Error ERR_READY_CONTINUE = new Error(-15, "继续游戏,不换桌出错");
    public final static Error ERR_ROOM_FORBID = new Error(-16, "即将开启");
    public final static Error ERR_ROOM_FULL = new Error(-17, "房间人数已满,请稍后再试");
    public final static Error ERR_ROOM_CLOSE = new Error(-18, "当前房间正在维护,开启时间请留意公告");
    public final static Error ERR_DB_ERROR = new Error(-19, "从DB获取玩家数据错误-19");
    public final static Error ERR_MONEY_NOT_ENOUGH = new Error(-19, "游戏金额不足");
    public final static Error ERR_SUCCESS = new Error(0, "成功");
    public final static Error ERR_TIMEOUT = new Error(-99, "消息处理超时");

    public final static Error ROB_ERR_SUCCESS = ERR_SUCCESS;
    public final static Error ROB_ERR_PUT_ROBOT = new Error(-501, "添加机器人失败");
    public final static Error ROB_ERR_DELE_ROBOT = new Error(-502, "删除机器人失败");
    public final static Error ROB_ERR_CREATE_ROBOT = new Error(-503, "创建机器人失败");
    public final static Error ROB_ERR_ENTER_TABLE = new Error(-504, "进入桌子失败");
    public final static Error ROB_ERR_TABLE_FULL = new Error(-505, "桌子已满人");
    public final static Error ROB_ERR_NOTIN_TABLE = new Error(-506, "机器人不能参与游戏");
    public final static Error ROB_ERR_GAME_STATE = new Error(-507, "游戏状态错误");
    public final static Error ROB_ERR_ROBCOUNT_Betting = new Error(-508, "根据人数下注出错");
    public final static Error ROB_ERR_START_ERROR = new Error(-509, "游戏开始错误");
    public final static Error ROB_ERR_UPDATA_BANKERLIST = new Error(-510, "更新上庄列表出错");

    public final static class RobotPairType {
        public enum Type {
            One, Range, Fix, Solo
        }

        public Type type;
        // 房间最少人数
        public int min;
        // 房间最大人数
        public int max;
        // 是否在游戏过程中匹配机器人
        public boolean isRobotPairOnGameing;
        // 是否在游戏过程中匹配玩家
        public boolean isPlayerPairOnGameing;
        // 是否中途玩家可以加入
        public boolean isPair;
        // 是否在游戏开始匹配机器人
        public boolean isPairOnGameStart;
        // 是否在游戏结束踢出机器人
        public boolean isKickOnGameOver;
        // 是否进房间就开始匹配
        public boolean isEnterRoomPair;
        // 是否在房间没有真实玩家关闭房间
        public boolean isRobotOnlyShutDown;
        // 是否需要在启动时预热
        public boolean isNeedPreheatStart;
        // 是否需要在启动时准备桌子
        public boolean isNeedPrepareTable;
        // 是否一直等待角色
        public boolean isAlwaysWaitRole;
        // 是否只有一个真实玩家
        public boolean isOneRealPlayer;
        // 游戏过程匹配机器人间隔时间(秒)
        public int gamingPairGapTime = 15;
        // 游戏过程机器人最短生命周期
        public int gamingRobotMinLifeTime = 30;
        // 游戏过程机器人最长生命周期
        public int gamingRobotMaxLifeTime = 60;
        // 游戏匹配时间
        public int gamingPairTime = 0;
        // 最少游戏玩家人数
        public int leastGamingPlayerNum = 0;

        public int backServerGameType = 1;

        public RobotPairType(Type type, int min, int max) {
            this.type = type;
            this.min = min;
            this.max = max;
            if (type == Type.One) {
                this.isPairOnGameStart = true;
                this.isRobotPairOnGameing = false;
                this.isPlayerPairOnGameing = true;
                this.isKickOnGameOver = true;
                this.isEnterRoomPair = true;
                this.gamingPairTime = 0;
                this.isRobotOnlyShutDown = false;
                this.isNeedPreheatStart = true;
                this.isNeedPrepareTable = true;
                this.leastGamingPlayerNum = 0;
                this.isAlwaysWaitRole = true;
                this.isOneRealPlayer = false;
                this.backServerGameType = 1;
            } else if (type == Type.Range || type == Type.Fix) {
                this.isPairOnGameStart = false;
                this.isRobotPairOnGameing = false;
                this.isPlayerPairOnGameing = false;
                this.isKickOnGameOver = false;
                this.isEnterRoomPair = false;
                this.gamingPairTime = 10;
                this.isRobotOnlyShutDown = true;
                this.isNeedPreheatStart = false;
                this.isNeedPrepareTable = false;
                this.isAlwaysWaitRole = false;
                this.isOneRealPlayer = false;
                this.leastGamingPlayerNum = (type == Type.Range) ? 2 : this.max;
                this.backServerGameType = 3;
            } else if (type == Type.Solo) {
                this.isPairOnGameStart = false;
                this.isRobotPairOnGameing = true;
                this.isPlayerPairOnGameing = true;
                this.isKickOnGameOver = false;
                this.isEnterRoomPair = true;
                this.gamingPairTime = 0;
                this.isRobotOnlyShutDown = true;
                this.isNeedPreheatStart = false;
                this.isNeedPrepareTable = false;
                this.isAlwaysWaitRole = false;
                this.isOneRealPlayer = true;
                this.leastGamingPlayerNum = 1;
                this.backServerGameType = 2;
            }
        }
    }

    public static class RobotConfig {
        public int bornRate;
    }

    public final static int ROBOT_MAX_COUNT = 1000;
    public final static float ROBOT_COLLECT_TIME = 5 * 60;
    public final static float ROBOT_LIFE_TIME = 10 * 60;

    public final static HashMap<Robot.Type, RobotConfig> ROBOTTYPE = new HashMap<>();
    static {
        RobotConfig robot = new RobotConfig();
        robot.bornRate = 20;
        ROBOTTYPE.put(Robot.Type.Bold, robot);

        robot = new RobotConfig();
        robot.bornRate = 65;
        ROBOTTYPE.put(Robot.Type.Nomal, robot);

        robot = new RobotConfig();
        robot.bornRate = 15;
        ROBOTTYPE.put(Robot.Type.Timid, robot);
    }
}