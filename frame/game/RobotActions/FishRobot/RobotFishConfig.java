package frame.game.RobotActions.FishRobot;

import frame.game.RobotActions.RobotFrame.*;
import frame.socket.common.proto.LobbySiteRoom.*;

public class RobotFishConfig extends RobotBaseConfig {

    // 盘口限制
    private long _minDoorScore;
    private long _maxDoorScore;

    public RobotFishConfig(Type type, int minPlayer, int maxPlayer) {
        super(type, minPlayer, maxPlayer);
    }

    public boolean Init(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.Init(betRoomCfg, fishRoomCfg, pkRoomCfg);
        // 如果配置为空 直接获取默认值
        SetDefaultData();
        if (fishRoomCfg != null) {
            SetData(fishRoomCfg);
        }
        return true;
    }

    public boolean UpDataConfig(BetRoomCfg betRoomCfg, FishRoomCfg fishRoomCfg, PkRoomCfg pkRoomCfg) {
        super.UpDataConfig(betRoomCfg, fishRoomCfg, pkRoomCfg);
        // 如果配置为空 直接获取默认值
        SetDefaultData();
        if (fishRoomCfg != null) {
            SetData(fishRoomCfg);
        }
        return true;
    }

    // 设置默认值
    private void SetDefaultData() {
        _minDoorScore = 100000;
        _maxDoorScore = 100000;
    }

    private void SetData(FishRoomCfg fishRoomCfg) {

    }
    // //获取盘口限制
    // public long GetDoorScore()
    // {
    // return _minDoorScore;
    // }
}