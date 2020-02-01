package frame.game;

import java.util.Map;

import frame.*;
import lombok.Getter;

public abstract class GameMain extends BaseMain {
    Map<String, Integer> msgtypeMap;

    public GameMain() {
        instance = this;
    }

    private static @Getter GameMain instance;
    protected @Getter GameMgr gameMgr;

    private boolean callReady = false;

    protected void onPrepare() {
        gameMgr.onPrepare();
    }

    public GameHallMgr getHallMgr() {
        return (GameHallMgr) hallMgr;
    }

    protected void setHallMgr() {
        hallMgr = (HallMgrProtocol) new GameHallMgr();
    }

    public GameRoleMgr getRoleMgr() {
        return (GameRoleMgr) roleMgr;
    }

    protected void setRoleMgr() {
        roleMgr = (RoleMgrProtocol) new GameRoleMgr();
    }

    protected void onReadyRun() {
        if (!callReady && UtilsMgr.getMultiCallMgr().isDone() && hallMgr.isReady()) {
            callReady = true;
            log.info("游戏服务启动");
            setStatus(GameMain.Status.RUN);
        }
    }

    @Override
    protected void doStop() {
        gameMgr.onStop();
        super.doStop();
    }

    @Override
    protected void doTerminate() {
        gameMgr.onTerminate();
        super.doTerminate();
    }

    @Override
    protected void doDestroy() {
        gameMgr.onDestroy();
        super.doDestroy();
    }

}