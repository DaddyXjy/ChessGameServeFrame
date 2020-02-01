package frame.game.RobotActions.PkRobot;

import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.util.RandomUtil;
import frame.socket.common.proto.LobbySiteRoom.*;

public class RobotPkConfig extends RobotBaseConfig {

    // 准备时间
    private int _minReadyTime;
    private int _maxReadyTime;
    // 离场概率
    private int _minOutTablePro;
    private int _maxOutTablePro;

    // 允许玩家匹配到一起
    private boolean _allowPlayerPlay;
    // 允许上次一起的玩家
    private boolean _allowLastSameTablePlayer;
    // 允许客户端Ip相同的玩家
    private boolean _allowSameIP;

    // 构造
    public RobotPkConfig(Type type, int minPlayer, int maxPlayer) {
        super(type, minPlayer, maxPlayer);
    }

    public boolean Init(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
        // 如果配置为空 直接获取默认值
        SetDefaultData();
        if (pkRoomCfg != null) {
            SetData(pkRoomCfg);
        }
        return true;
    }

    public boolean UpDataConfig(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.UpDataConfig(betRoomCfg, fishRoomCfg, pkRoomCfg);

        // 如果配置为空 直接获取默认值
        SetDefaultData();

        if (pkRoomCfg != null) {
            SetData(pkRoomCfg);
        }
        return true;
    }

    // 设置默认值
    private void SetDefaultData() {
        _minReadyTime = 3;
        _maxReadyTime = 5;

        _minOutTablePro = 20;
        _maxOutTablePro = 50;
        // 允许玩家匹配到一起
        _allowPlayerPlay = true;
        // 允许上次一起的玩家
        _allowLastSameTablePlayer = false;
        // 允许客户端Ip相同的玩家
        _allowSameIP = false;
    }

    // 设置数据
    private void SetData(PkRoomCfg pkRoomCfg) {
        // 准备时间
        if (pkRoomCfg.getRobotReadyTime() > 0 || pkRoomCfg.getStartTime() > 0) {
            _minReadyTime = pkRoomCfg.getStartTime();
        }
        // 离场概率
        if (pkRoomCfg.getRobotOutTablePro() > 0) {
            _minOutTablePro = pkRoomCfg.getRobotOutTablePro();
        }
        _allowPlayerPlay = pkRoomCfg.getAllowPlayerGame() == 1 ? true : false;
        _allowLastSameTablePlayer = pkRoomCfg.getAllowLastTimePlay() == 1 ? true : false;
        _allowSameIP = pkRoomCfg.getAllowIpEqualPlay() == 1 ? true : false;
        return;
    }

    // 获取准备时间
    public int GetReadyTime() {
        if (this._minReadyTime <= 1) {
            this._minReadyTime = 1;
        }
        return RandomUtil.ramdom(1, this._minReadyTime);
    }

    // 获取离场概率
    public int GetOutTablePro() {
        return this._minOutTablePro;
    }

    // 允许玩家匹配到一起
    public boolean GetAllowPlayerPlay() {
        return _allowPlayerPlay;
    }

    // 允许上次一起的玩家
    public boolean GetAllowLastSameTablePlayer() {
        return _allowLastSameTablePlayer;
    }

    // 允许客户端Ip相同的玩家
    public boolean GetAllowSameIP() {
        return _allowSameIP;
    }

}