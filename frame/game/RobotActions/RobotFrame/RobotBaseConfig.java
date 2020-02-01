package frame.game.RobotActions.RobotFrame;

import frame.util.RandomUtil;
import frame.socket.common.proto.LobbySiteRoom.*;
import frame.log;

//机器人基本配置
public class RobotBaseConfig {
    // one 下注类 range 对战(不固定人数) fix 对战(固定人数) solo 捕鱼
    public enum Type {
        One, Range, Fix, Solo
    }

    // 房间类型 试玩 财富
    public enum RoomType {
        Try, Money
    }

    // 游戏类型
    protected Type type;
    // 房间类型 1 体验房 2 财富房间
    protected RoomType roomType;
    // 最大参与人数
    protected int maxPlayer;
    // 最小参与人数
    protected int minPlayer;
    // 是否在游戏过程中匹配机器人
    protected boolean isRobotPairOnGameing;
    // 是否在游戏过程中匹配玩家
    protected boolean isPlayerPairOnGameing;
    // 是否中途玩家可以加入
    protected boolean isPair;
    // 是否在游戏开始匹配机器人
    protected boolean isPairOnGameStart;
    // 是否在游戏结束踢出机器人
    protected boolean isKickOnGameOver;
    // 是否进房间就开始匹配
    protected boolean isEnterRoomPair;
    // 是否在房间没有真实玩家关闭房间
    protected boolean isRobotOnlyShutDown;
    // 是否需要在启动时预热
    protected boolean isNeedPreheatStart;
    // 是否需要在启动时准备桌子
    protected boolean isNeedPrepareTable;
    // 是否一直等待角色
    protected boolean isAlwaysWaitRole;
    // 是否只有一个真实玩家
    protected boolean isOneRealPlayer;
    // 游戏过程匹配机器人间隔时间(秒)
    protected int gamingPairGapTime = 15;
    // 游戏匹配时间
    protected int gamingPairTime = 0;
    // 最少游戏玩家人数
    protected int leastGamingPlayerNum = 0;
    // 回给服务器的游戏类型
    protected int backServerGameType;
    // 是否必须加入一个机器人
    protected boolean isMustHaveRobot;
    // 桌子的最小人数
    protected int minTableRoleCount;
    // 桌子的最大人数
    protected int maxTableRoleCount;
    // 是否允许机器人进入
    private boolean _isHaveRobot;
    // 是否可以留桌
    private boolean _isNoChangeTable;
    // 最小携带金钱
    private long _minCarryScore;
    // 最大携带金钱
    private long _maxCarryScore;
    // 最小机器人数量
    private int _minRobotCount;
    // 最大机器人数量
    private int _maxRobotCount;
    // 机器人达到多少钱时取钱
    private long _robotMinTakeScore;
    private long _robotMaxTakeScore;
    // 取钱范围
    private long _minTakeScore;
    private long _maxTakeScore;
    // 昵称更换时间(分)
    private int _minChangeNickName;
    private int _maxChangeNickName;
    // 机器人匹配时间
    private int _gamePairTime;
    // 机器人进入房间人数峰值
    private int _RobotCountForRoomCount;

    public RobotBaseConfig(Type type, int minPlayer, int maxPlayer) {
        this.type = type;
        this.maxPlayer = maxPlayer;
        this.minPlayer = minPlayer;
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
            this.isMustHaveRobot = false;
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
            this.leastGamingPlayerNum = (type == Type.Range) ? 2 : this.minPlayer;
            this.backServerGameType = 3;
            this.isMustHaveRobot = true;
        } else if (type == Type.Solo) {
            this.isPairOnGameStart = true;
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
            this.isMustHaveRobot = true;
        }
    }

    // 下注类初始化
    public boolean Init(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        // 基本初始化
        SetDefaultData();
        if (betRoomCfg != null || fishRoomCfg != null || pkRoomCfg != null) {
            SetData(betRoomCfg, fishRoomCfg, pkRoomCfg);
        }

        return true;
    }

    // 更新数据
    public boolean UpDataConfig(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        SetDefaultData();
        if (betRoomCfg != null || fishRoomCfg != null || pkRoomCfg != null) {
            SetData(betRoomCfg, fishRoomCfg, pkRoomCfg);
        }

        return true;
    }

    // 设置默认值
    private void SetDefaultData() {
        // 默认房间为财富房间
        roomType = RoomType.Money;
        _isHaveRobot = true;
        _minCarryScore = 1000000;
        _maxCarryScore = 5000000;
        _minRobotCount = 10;
        _maxRobotCount = 30;
        _robotMinTakeScore = 100000;
        _robotMaxTakeScore = 150000;
        _minTakeScore = 100000;
        _maxTakeScore = 500000;
        _minChangeNickName = 5;
        _maxChangeNickName = 10;
        _gamePairTime = 3;
        _RobotCountForRoomCount = 200;
        minTableRoleCount = 20;
        maxTableRoleCount = 40;
        // 是否可以留桌
        _isNoChangeTable = false;
    }

    // 读取配置文件
    private void SetData(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        if (type == Type.One) {
            roomType = GetRoomType(betRoomCfg.getRoomType());
            _isHaveRobot = betRoomCfg.getRobotJoining() == 1 ? true : false;
            _RobotCountForRoomCount = betRoomCfg.getRoomPersons();
            // 携带金钱
            if (betRoomCfg.getRobotGold1() > 0 && betRoomCfg.getRobotGold2() > 0
                    && betRoomCfg.getRobotGold1() <= betRoomCfg.getRobotGold2()) {
                _minCarryScore = betRoomCfg.getRobotGold1();
                _maxCarryScore = betRoomCfg.getRobotGold2();
            }
            if (betRoomCfg.getRobotCount1() > 0 && betRoomCfg.getRobotCount2() > 0
                    && betRoomCfg.getRobotCount1() <= betRoomCfg.getRobotCount2()) {
                _minRobotCount = betRoomCfg.getRobotCount1();
                _maxRobotCount = betRoomCfg.getRobotCount2();
            }

            // 手中金钱
            if (betRoomCfg.getRobotMinHaveScore() > 0 && betRoomCfg.getRobotMaxHaveScore() > 0
                    && betRoomCfg.getRobotMinHaveScore() <= betRoomCfg.getRobotMaxHaveScore()) {
                _minTakeScore = betRoomCfg.getRobotMinHaveScore();
                _maxTakeScore = betRoomCfg.getRobotMaxHaveScore();
            }

            // 取钱
            if (betRoomCfg.getRobotMinTakeScore() > 0 && betRoomCfg.getRobotMaxTakeScore() > 0
                    && betRoomCfg.getRobotMinTakeScore() <= betRoomCfg.getRobotMaxTakeScore()) {
                _robotMinTakeScore = betRoomCfg.getRobotMinTakeScore();
                _robotMaxTakeScore = betRoomCfg.getRobotMaxTakeScore();
            }

            // 更换昵称时间
            if (betRoomCfg.getRobotMinChangeNickName() > 0 && betRoomCfg.getRobotMaxChangeNickName() > 0
                    && betRoomCfg.getRobotMinChangeNickName() <= betRoomCfg.getRobotMaxChangeNickName()) {
                _minChangeNickName = betRoomCfg.getRobotMinChangeNickName();
                _maxChangeNickName = betRoomCfg.getRobotMaxChangeNickName();
            }
            if (betRoomCfg.getRoomPersons() > 0) {
                minTableRoleCount = betRoomCfg.getRoomPersons();

                maxTableRoleCount = betRoomCfg.getRoomPersons();
            }
        } else if (type == Type.Solo) {
            roomType = GetRoomType(fishRoomCfg.getRoomType());
            _isHaveRobot = fishRoomCfg.getRobotJoining() == 1 ? true : false;
            // 携带金钱
            if (fishRoomCfg.getRobotGold1() > 0 && fishRoomCfg.getRobotGold2() > 0
                    && fishRoomCfg.getRobotGold1() <= fishRoomCfg.getRobotGold2()) {
                _minCarryScore = fishRoomCfg.getRobotGold1();
                _maxCarryScore = fishRoomCfg.getRobotGold2();
            }
            if (fishRoomCfg.getRobotCount1() > 0 && fishRoomCfg.getRobotCount2() > 0
                    && fishRoomCfg.getRobotCount1() <= fishRoomCfg.getRobotCount2()) {
                _minRobotCount = fishRoomCfg.getRobotCount1();
                _maxRobotCount = fishRoomCfg.getRobotCount2();
            }

            // 手中金钱
            if (fishRoomCfg.getRobotMinHaveScore() > 0 && fishRoomCfg.getRobotMaxHaveScore() > 0
                    && fishRoomCfg.getRobotMinHaveScore() <= fishRoomCfg.getRobotMaxHaveScore()) {
                _minTakeScore = fishRoomCfg.getRobotMinHaveScore();
                _maxTakeScore = fishRoomCfg.getRobotMaxHaveScore();
            }

            // 取钱
            if (fishRoomCfg.getRobotMinTakeScore() > 0 && fishRoomCfg.getRobotMaxTakeScore() > 0
                    && fishRoomCfg.getRobotMinTakeScore() <= fishRoomCfg.getRobotMaxTakeScore()) {
                _robotMinTakeScore = fishRoomCfg.getRobotMinTakeScore();
                _robotMaxTakeScore = fishRoomCfg.getRobotMaxTakeScore();
            }

            // 更换昵称时间
            if (fishRoomCfg.getRobotMinChangeNickName() > 0 && fishRoomCfg.getRobotMaxChangeNickName() > 0
                    && fishRoomCfg.getRobotMinChangeNickName() <= fishRoomCfg.getRobotMaxChangeNickName()) {
                _minChangeNickName = fishRoomCfg.getRobotMinChangeNickName();
                _maxChangeNickName = fishRoomCfg.getRobotMaxChangeNickName();
            }
            if (fishRoomCfg.getRoomPersons() > 0) {
                minTableRoleCount = fishRoomCfg.getRoomPersons();

                maxTableRoleCount = fishRoomCfg.getRoomPersons();
            }
        } else if (type == Type.Range || type == Type.Fix) {
            roomType = GetRoomType(pkRoomCfg.getRoomType());
            _isHaveRobot = pkRoomCfg.getRobotJoining() == 1 ? true : false;
            // 携带金钱
            if (pkRoomCfg.getRobotGold1() > 0 && pkRoomCfg.getRobotGold2() > 0
                    && pkRoomCfg.getRobotGold1() <= pkRoomCfg.getRobotGold2()) {
                _minCarryScore = pkRoomCfg.getRobotGold1();
                _maxCarryScore = pkRoomCfg.getRobotGold2();
            }
            if (pkRoomCfg.getRobotCount1() > 0 && pkRoomCfg.getRobotCount2() > 0
                    && pkRoomCfg.getRobotCount1() <= pkRoomCfg.getRobotCount2()) {
                _minRobotCount = pkRoomCfg.getRobotCount1();
                _maxRobotCount = pkRoomCfg.getRobotCount2();
            }

            // 手中金钱
            if (pkRoomCfg.getRobotMinHaveScore() > 0 && pkRoomCfg.getRobotMaxHaveScore() > 0
                    && pkRoomCfg.getRobotMinHaveScore() <= pkRoomCfg.getRobotMaxHaveScore()) {
                _minTakeScore = pkRoomCfg.getRobotMinHaveScore();
                _maxTakeScore = pkRoomCfg.getRobotMaxHaveScore();
            }

            // 取钱
            if (pkRoomCfg.getRobotMinTakeScore() > 0 && pkRoomCfg.getRobotMaxTakeScore() > 0
                    && pkRoomCfg.getRobotMinTakeScore() <= pkRoomCfg.getRobotMaxTakeScore()) {
                _robotMinTakeScore = pkRoomCfg.getRobotMinTakeScore();
                _robotMaxTakeScore = pkRoomCfg.getRobotMaxTakeScore();
            }

            // 更换昵称时间
            if (pkRoomCfg.getRobotMinChangeNickName() > 0 && pkRoomCfg.getRobotMaxChangeNickName() > 0
                    && pkRoomCfg.getRobotMinChangeNickName() <= pkRoomCfg.getRobotMaxChangeNickName()) {
                _minChangeNickName = pkRoomCfg.getRobotMinChangeNickName();
                _maxChangeNickName = pkRoomCfg.getRobotMaxChangeNickName();
            }
            if (pkRoomCfg.getRoomPersons() > 0) {
                minTableRoleCount = pkRoomCfg.getRoomPersons();

                maxTableRoleCount = pkRoomCfg.getRoomPersons();
            }
            _gamePairTime = pkRoomCfg.getStartTime();
        }

    }

    // 获取房间类型
    private RoomType GetRoomType(int roomTypeIndex) {
        switch (roomTypeIndex) {
        case 1: {
            return RoomType.Try;
        }
        case 2: {
            return RoomType.Money;
        }
        }
        return RoomType.Money;
    }

    // 游戏类型
    public Type GetGameType() {
        return type;
    }

    // 房间类型
    public RoomType GetRoomType() {
        return roomType;
    }

    public int GetMinPlayer() {
        return this.minPlayer;
    }

    // 获取最大桌子人数
    public int GetMaxPlayer() {
        return this.maxPlayer;
    }

    // 是否在游戏过程中匹配机器人
    public boolean GetIsRobotPairOnGameing() {
        return isRobotPairOnGameing;
    }

    // 是否在游戏过程中匹配玩家
    public boolean GetIsPlayerPairOnGameing() {
        return isPlayerPairOnGameing;
    }

    // 是否中途玩家可以加入
    public boolean GetIsPair() {
        return isPair;
    }

    // 是否在游戏开始匹配机器人
    public boolean GetIsPairOnGameStart() {
        return isPairOnGameStart;
    }

    // 是否在游戏结束踢出机器人
    public boolean GetIsKickOnGameOver() {
        return isKickOnGameOver;
    }

    // 是否进房间就开始匹配
    public boolean GetIsEnterRoomPair() {
        return isEnterRoomPair;
    }

    // 是否在房间没有真实玩家关闭房间
    public boolean GetIsRobotOnlyShutDown() {
        return isRobotOnlyShutDown;
    }

    // 是否需要在启动时预热
    public boolean GetIsNeedPreheatStart() {
        return isNeedPreheatStart;
    }

    // 是否需要在启动时准备桌子
    public boolean GetIsNeedPrepareTable() {
        return isNeedPrepareTable;
    }

    // 是否一直等待角色
    public boolean GetIsAlwaysWaitRole() {
        return isAlwaysWaitRole;
    }

    // 是否只有一个真实玩家
    public boolean GetIsOneRealPlayer() {
        return isOneRealPlayer;
    }

    // 游戏过程匹配机器人间隔时间(秒)
    public int GetGamingPairGapTime() {
        return gamingPairGapTime;
    }

    // 游戏匹配时间
    public int GetGamingPairTime() {
        return gamingPairTime;
    }

    // 最少游戏玩家人数
    public int GetLeastGamingPlayerNum() {
        return leastGamingPlayerNum;
    }

    // 返回服务器游戏类型
    public int GetBackServerGameType() {
        return backServerGameType;
    }

    // 是否必须有一个机器人
    public boolean GetIsMustHaveRobot() {
        return isMustHaveRobot;
    }

    // 桌子的最小人数
    public int GetMinTableRoleCount() {
        return minTableRoleCount;
    }

    // 桌子的最大人数
    public int GetMaxTableRoleCount() {
        return maxTableRoleCount;
    }

    // 是否允许机器人进入
    public boolean IsHaveRobot() {
        return _isHaveRobot;
    }

    // 是否可以留桌
    public boolean IsNoChangeTable() {
        return _isNoChangeTable;
    }

    // 携带金钱
    public long GetCarryScore() {
        if (this._minCarryScore == this._maxCarryScore) {
            log.debug("携带金钱:" + _maxCarryScore);
            return this._maxCarryScore;
        }
        long x = ((RandomUtil.ramdom(this._minCarryScore, this._maxCarryScore) / 10) * 10);
        log.debug("携带金钱:" + x);
        return x;
    }

    // 获取最大机器人数量
    public int GetMaxRobotCount() {
        return _maxRobotCount;
    }

    // 加载机器人数量
    public int GetRobotCount() {
        if (this._minRobotCount == this._maxRobotCount) {
            log.debug("加载机器人数量:" + _maxRobotCount);
            return this._maxRobotCount;
        }
        int x = RandomUtil.ramdom(this._minRobotCount, this._maxRobotCount);
        log.debug("加载机器人数量:" + x);
        return x;
    }

    // 游戏中携带最小金钱
    public long GetRobotTakeScore() {
        if (this._robotMinTakeScore == this._robotMaxTakeScore) {
            log.debug("游戏中携带最小金钱:" + this._robotMaxTakeScore);
            return this._robotMaxTakeScore;
        }
        long x = RandomUtil.ramdom(this._robotMinTakeScore, this._robotMaxTakeScore);
        log.debug("游戏中携带最小金钱:" + x);
        return x;
    }

    // 取钱
    public long GetTakeScore() {
        if (this._minTakeScore == this._maxTakeScore) {
            return this._maxTakeScore;
        }
        return RandomUtil.ramdom(this._minTakeScore, this._maxTakeScore);
    }

    // 更换昵称时间 转毫秒
    public long GetChangeNickName() {
        if (this._minChangeNickName == this._maxChangeNickName) {
            log.debug("更换昵称时间:" + this._maxChangeNickName * 60 * 1000);
            return this._maxChangeNickName * 60 * 1000;
        }
        long x = RandomUtil.ramdom(this._minChangeNickName, this._maxChangeNickName) * 60 * 1000;
        log.debug("更换昵称时间:" + x);
        return x;
    }

    // 匹配游戏时间
    public int GetGamePairTime() {
        if (this._gamePairTime <= 1) {
            this._gamePairTime = 1;
        }
        int x = RandomUtil.ramdom(1, this._gamePairTime);
        log.debug("匹配游戏时间:" + x);
        return x;
    }

    // 机器人数量根据房间人数峰值
    public int GetRobotCountForRoomCount() {
        return _RobotCountForRoomCount;
    }

}