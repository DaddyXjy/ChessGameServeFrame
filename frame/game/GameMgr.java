package frame.game;

import frame.*;
import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.game.RobotActions.RobotFrame.RobotBaseConfig.Type;
import frame.util.RandomNameUtil;
import lombok.Getter;
import lombok.Setter;

public abstract class GameMgr {

    // protected @Getter Config.RobotPairType robotPairType;
    protected @Getter int gameId;
    // test by zy 2019.3.24
    protected @Getter RobotBaseConfig robotConfig;

    public GameMgr() {

    }

    public void setGameId(int id) {
        this.gameId = id;
        Config.GAME_ID = id;
    }

    public abstract Player createPlayer();

    public abstract Robot createRobot();

    public abstract Table createTable();

    public GameHall createHall(int id) {
        return new GameHall(id);
    };

    // 判断游戏类型
    public boolean isGameType(RobotBaseConfig.Type type) {
        return robotConfig.GetGameType() == type;
    }

    public void onPrepare() {
        RandomNameUtil.loadConfig();
    }

    public void onStop() {
    }

    public void onTerminate() {
    }

    public void onDestroy() {
    }
}