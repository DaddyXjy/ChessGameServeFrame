package frame.game.RobotActions.BetsRobot;

import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.util.RandomUtil;
import frame.socket.common.proto.LobbySiteRoom.*;
import frame.log;

//下注类游戏配置
public class RobotBetsConfig extends RobotBaseConfig {

    // 机器人性格
    public enum RobotCharacter {
        // 胆小
        Timid,
        // 普通
        Nomal,
        // 胆大
        Bold
    }

    // 游戏状态
    // 性格概率
    private int _timidRobotPro;
    private int _nomalRobotPro;
    private int _boldRobotPro;
    // ============================下注
    // 房间下注金额
    private long _minRoomBettingScore;
    private long _maxRoomBettingScore;

    // 根据性格的下注配置
    private int _timidBettingPro;
    private int _nomalBettingPro;
    private int _boldBettingPro;
    // 根据总盘口限额下注
    private int _allDoorScoreToBettingScorePro;

    // 机器人参与下注数
    private int _minRobotPlayCount;
    private int _maxRobotPlayCount;
    // 机器人下注次数
    private int _minBettingCount;
    private int _maxBettingCount;
    // 下注筹码个数
    private int _minChipsCount;
    private int _maxChipsCount;
    // 盘口限制
    private long _minDoorScore;
    private long _maxDoorScore;
    // =============================上庄
    // 是否允许机器人上庄
    private boolean _isUpBanker;
    // 上庄携带金币
    private long _minUpBankerScore;
    private long _maxUpBankerScore;
    // 上庄数量
    private int _minRobotUpBankerCount;
    private int _maxRobotUpBankerCount;
    // 坐庄次数
    private int _minUpBankerCnt;
    private int _maxUpBankerCnt;
    // 列表人数
    private int _minUpBankerListCount;
    private int _maxUpBankerListCount;
    // 空几局坐庄
    private int _minWaitNoBankerCount;
    private int _maxWaitNoBankerCount;

    // 游戏配置
    // 盘口数量
    private int _doorCount;
    // 盘口倍率
    private int[] _doorTimes;
    // 盘口下注概率
    private int[] _doorBettingPro;

    public RobotBetsConfig(Type type, int minPlayer, int maxPlayer) {
        super(type, minPlayer, maxPlayer);
        // 初始化基本数据
        // super.Init(null);
        // Init(null);
    }

    public boolean Init(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
        // 如果配置为空 直接获取默认值
        SetDefaultData();
        if (betRoomCfg != null) {
            SetData(betRoomCfg);
        }
        return true;
    }

    public boolean UpDataConfig(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.UpDataConfig(betRoomCfg, fishRoomCfg, pkRoomCfg);

        // 如果配置为空 直接获取默认值
        SetDefaultData();

        if (betRoomCfg != null) {
            SetData(betRoomCfg);
        }
        return true;
    }

    // 设置默认值
    private void SetDefaultData() {
        _timidRobotPro = 50;
        _nomalRobotPro = 40;
        _boldRobotPro = 10;
        // 下注
        _minRoomBettingScore = 10000;
        _maxRoomBettingScore = 100000;

        _timidBettingPro = 30;
        _nomalBettingPro = 60;
        _boldBettingPro = 100;

        _allDoorScoreToBettingScorePro = 70;

        _minRobotPlayCount = 50;
        _maxRobotPlayCount = 100;

        _minBettingCount = 5;
        _maxBettingCount = 10;

        _minChipsCount = 5;
        _maxChipsCount = 20;

        _minDoorScore = 1000000;
        _maxDoorScore = 1000000;
        // 上庄
        _isUpBanker = false;

        _minUpBankerScore = 500000;
        _maxUpBankerScore = 1000000;

        _minRobotUpBankerCount = 10;
        _maxRobotUpBankerCount = 20;

        _minUpBankerCnt = 3;
        _maxUpBankerCnt = 5;

        _minUpBankerListCount = 3;
        _maxUpBankerListCount = 5;

        _minWaitNoBankerCount = 3;
        _maxWaitNoBankerCount = 5;
    }

    private void SetData(BetRoomCfg betRoomCfg) {
        // 下注金额
        if (betRoomCfg.getBottomRed1() > 0 && betRoomCfg.getBottomRed2() > 0
                && betRoomCfg.getBottomRed1() <= betRoomCfg.getBottomRed2()) {
            _minRoomBettingScore = betRoomCfg.getBottomRed1();
            _maxRoomBettingScore = betRoomCfg.getBottomRed2();
        }
        // 筹码数
        if (betRoomCfg.getRobotMinChipsCount() > 0 && betRoomCfg.getRobotMaxChipsCount() > 0
                && betRoomCfg.getRobotMinChipsCount() <= betRoomCfg.getRobotMaxChipsCount()) {
            _minChipsCount = betRoomCfg.getRobotMinChipsCount();
            _maxChipsCount = betRoomCfg.getRobotMaxChipsCount();
        }

        // 参与下注数
        if (betRoomCfg.getRobotMinRobotPlayCount() > 0 && betRoomCfg.getRobotMaxRobotPlayCount() > 0
                && betRoomCfg.getRobotMinRobotPlayCount() <= betRoomCfg.getRobotMaxRobotPlayCount()) {
            _minRobotPlayCount = betRoomCfg.getRobotMinRobotPlayCount();
            _maxRobotPlayCount = betRoomCfg.getRobotMaxRobotPlayCount();
        }

        // 盘口限制
        if (betRoomCfg.getRobotDoorScore() > 0) {
            _minDoorScore = betRoomCfg.getRobotDoorScore();
            _maxDoorScore = betRoomCfg.getRobotDoorScore();
        }

        // 是否可以上庄
        _isUpBanker = betRoomCfg.getSysGold() == 1 ? true : false;
        // 上庄携带
        if (betRoomCfg.getGoldCoin1() > 0 && betRoomCfg.getGoldCoin2() > 0
                && betRoomCfg.getGoldCoin1() <= betRoomCfg.getGoldCoin2()) {
            _minUpBankerScore = betRoomCfg.getGoldCoin1();
            _maxUpBankerScore = betRoomCfg.getGoldCoin2();
        }

        // 上庄数量
        if (betRoomCfg.getGoldCount1() > 0 && betRoomCfg.getGoldCount2() > 0
                && betRoomCfg.getGoldCount1() <= betRoomCfg.getGoldCount2()) {
            _minRobotUpBankerCount = betRoomCfg.getGoldCount1();
            _maxRobotUpBankerCount = betRoomCfg.getGoldCount2();
        }

        // 坐庄次数
        if (betRoomCfg.getRobotMinUpBankerCnt() > 0 && betRoomCfg.getRobotMaxUpBankerCnt() > 0
                && betRoomCfg.getRobotMinUpBankerCnt() <= betRoomCfg.getRobotMaxUpBankerCnt()) {
            _minUpBankerCnt = betRoomCfg.getRobotMinUpBankerCnt();
            _maxUpBankerCnt = betRoomCfg.getRobotMaxUpBankerCnt();
        }

        // 列表人数
        if (betRoomCfg.getRobotMinUpBankerList() > 0 && betRoomCfg.getRobotMaxUpBankerList() > 0
                && betRoomCfg.getRobotMinUpBankerList() <= betRoomCfg.getRobotMaxUpBankerList()) {
            _minUpBankerListCount = betRoomCfg.getRobotMinUpBankerList();
            _maxUpBankerListCount = betRoomCfg.getRobotMaxUpBankerList();
        }

        // 空庄局数
        if (betRoomCfg.getRobotMinWaitNoBanker() > 0 && betRoomCfg.getRobotMaxWaitNoBanker() > 0
                && betRoomCfg.getRobotMinWaitNoBanker() <= betRoomCfg.getRobotMaxWaitNoBanker()) {
            _minWaitNoBankerCount = betRoomCfg.getRobotMinWaitNoBanker();
            _maxWaitNoBankerCount = betRoomCfg.getRobotMaxWaitNoBanker();
        }

        // 机器人性格
        _timidRobotPro = betRoomCfg.getRobotTimidRobotPro();
        _nomalRobotPro = betRoomCfg.getRobotNomalRobotPro();
        _boldRobotPro = betRoomCfg.getRobotBoldRobotPro();
        // 根据性格下注配置
        if (betRoomCfg.getRobotTimidBettingPro() > 0) {
            _timidBettingPro = betRoomCfg.getRobotTimidBettingPro();
        }
        if (betRoomCfg.getRobotNomalBettingPro() > 0) {
            _nomalBettingPro = betRoomCfg.getRobotNomalBettingPro();
        }
        if (betRoomCfg.getRobotBoldBettingPro() > 0) {
            _boldBettingPro = betRoomCfg.getRobotBoldBettingPro();
        }

        _allDoorScoreToBettingScorePro = betRoomCfg.getRobotBettingByBankerScore();
        return;
    }

    // 获得性格
    public int GetTimidRobotPro() {
        return _timidRobotPro;
    }

    public int GetNomalRobotPro() {
        return _nomalRobotPro;
    }

    public int GetBoldRobotPro() {
        return _boldRobotPro;
    }

    // 房间下注金额
    public long GetMinRoomBettingScore() {
        return _minRoomBettingScore;
    }

    public long GetMaxRoomBettingScore() {
        return _maxRoomBettingScore;

    }

    // 机器人下注金额
    public long GetRoomBettingScore() {
        if (this._minRoomBettingScore == this._maxRoomBettingScore) {
            log.debug("机器人下注金额" + this._maxRoomBettingScore);
            return this._maxRoomBettingScore;
        }
        long x = RandomUtil.ramdom(this._minRoomBettingScore, this._maxRoomBettingScore);
        log.debug("机器人下注金额" + x);
        return x;
    }

    // 根据性格的下注配置
    public int GetTimidBettingPro() {
        return _timidBettingPro;
    }

    public int GetNomalBettingPro() {
        return _nomalBettingPro;
    }

    public int GetBoldBettingPro() {
        return _boldBettingPro;
    }

    // 根据中盘口的下注限额
    public int GetAllDoorScoreToBettingScoreRro() {
        return _allDoorScoreToBettingScorePro;
    }

    // 根据中盘口的下注限额
    public long GetAllDoorScoreToBettingScore() {
        return (_minDoorScore * 100) / _allDoorScoreToBettingScorePro;
    }

    // 获得机器人参与下注数
    public int GetRobotPlayCount() {
        int randCount = 0;
        if (this._minRobotPlayCount == this._maxRobotPlayCount) {
            randCount = this._maxRobotPlayCount;
        } else {
            randCount = RandomUtil.ramdom(this._minRobotPlayCount, this._maxRobotPlayCount);

        }
        randCount = RandomUtil.ramdom(randCount / 2, randCount);
        log.debug("获得机器人参与下注数" + randCount);
        return randCount;
    }

    // 获得机器人下注次数
    public int GetBettingCount() {
        if (this._minBettingCount == this._maxBettingCount) {
            log.debug("获得机器人参与下注数" + this._maxBettingCount);
            return this._maxBettingCount;
        }
        int x = RandomUtil.ramdom(this._minBettingCount, this._maxBettingCount);
        log.debug("获得机器人参与下注数" + x);
        return x;
    }

    // 下注筹码个数
    public int GetChipsCount() {
        if (this._minChipsCount == this._maxChipsCount) {
            log.debug("下注筹码个数" + this._maxChipsCount);
            return this._maxChipsCount;
        }
        int x = RandomUtil.ramdom(this._minChipsCount, this._maxChipsCount);
        log.debug("下注筹码个数" + x);
        return x;
    }

    // 盘口限制
    public long GetMaxDoorScore() {
        return this._minDoorScore;
    }

    // 上庄
    // 是否允许机器人上庄
    public boolean IsUpBanker() {
        return this._isUpBanker;
    }

    // 获取上庄携带最小金额
    public long GetMinUpBankerScore() {
        return this._minUpBankerScore;
    }

    // 获取上庄携带金额
    public long GetUpBankerScore() {
        if (this._minUpBankerScore == this._maxUpBankerScore) {
            return this._maxUpBankerScore;
        }
        return RandomUtil.ramdom(this._minUpBankerScore, this._maxUpBankerScore);
    }

    // 上庄数量
    public int GetRobotUpBankerCount() {
        if (this._minRobotUpBankerCount == this._maxRobotUpBankerCount) {
            return this._maxRobotUpBankerCount;
        }
        return RandomUtil.ramdom(this._minRobotUpBankerCount, this._maxRobotUpBankerCount);
    }

    // 坐庄次数
    public int GetUpBankerCnt() {
        if (this._minUpBankerCnt == this._maxUpBankerCnt) {
            return this._maxUpBankerCnt;
        }
        return RandomUtil.ramdom(this._minUpBankerCnt, this._maxUpBankerCnt);
    }

    // 列表人数
    public int GetUpBankerListCount() {
        if (this._minUpBankerListCount == this._maxUpBankerListCount) {
            return this._maxUpBankerListCount;
        }
        return RandomUtil.ramdom(this._minUpBankerListCount, this._maxUpBankerListCount);
    }

    // 等待空局数
    public int GetWaitNoBankerCount() {
        if (this._minWaitNoBankerCount == this._maxWaitNoBankerCount) {
            return this._maxWaitNoBankerCount;
        }
        return RandomUtil.ramdom(this._minWaitNoBankerCount, this._maxWaitNoBankerCount);
    }
}