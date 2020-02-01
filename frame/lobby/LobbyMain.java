package frame.lobby;

import lombok.Getter;
import frame.*;
import frame.game.proto.Game.GetPrizeMoney;
import frame.game.proto.Game.SetPrizeMoney;
import frame.game.proto.Game.TurnUpdateMoney;
import frame.socket.Request;
import frame.socket.Response;
import frame.socket.common.proto.Error.ErrorRes;

public abstract class LobbyMain extends BaseMain {

    protected @Getter LobbyMgr lobbyMgr;

    public LobbyMain() {
        instance = this;
    }

    private static @Getter LobbyMain instance;

    public LobbyHallMgr getHallMgr() {
        return (LobbyHallMgr) hallMgr;
    }

    protected void setHallMgr() {
        hallMgr = (HallMgrProtocol) new LobbyHallMgr();
    }

    public LobbyRoleMgr getRoleMgr() {
        return (LobbyRoleMgr) roleMgr;
    }

    protected void setRoleMgr() {
        roleMgr = (RoleMgrProtocol) new LobbyRoleMgr();
    }

    protected void onPrepare() {
        lobbyMgr.onPrepare();
    }

    protected void onReadyRun() {
        if (hallMgr.isReady()) {
            log.info("游戏服务启动");
            setStatus(Status.RUN);
            getPrizeMoney();
        }
    }

    
    
    private void getPrizeMoney() {
    	GetPrizeMoney.Builder prizeMoney = GetPrizeMoney.newBuilder();
		byte[] val = prizeMoney.build().toByteArray();
		Response resp = new Response(700005, val);
		send2DB(resp, Config.SITE_ID, 0,new Callback() {
		@Override
		public void func() {
            Request dbReq = (Request) this.getData();
            if (dbReq.isError()) {
                if (dbReq.isTimeout()) {
                    log.error("从DB获取奖池金额超时");
                } else {
                    try {
                        ErrorRes errorRes = ErrorRes.parseFrom(dbReq.protoMsg);
                        log.error("从DB获取奖池金额失败:{}", errorRes.getMsg());
                    } catch (Exception e) {
                        log.error("error :", e);
                    }
                }
                return;
            }
            try {
            	SetPrizeMoney getPrizeMoney = SetPrizeMoney.parseFrom(dbReq.protoMsg);
            	log.info("启动服务器时拉取到的奖池金额:{}",getPrizeMoney.getPrizeMoney());
            	getHallMgr().setPrizeMoney(getPrizeMoney.getPrizeMoney());
			} catch (Exception e) {
				log.error("从DB获取奖池金额出错:{}",e);
				e.printStackTrace();
			}
		}
	});
    	
    }
    
    
    
    
    
    
    @Override
    protected void doStop() {
        lobbyMgr.onStop();
        super.doStop();
    }

    @Override
    protected void doTerminate() {
        lobbyMgr.onTerminate();
        super.doTerminate();
    }

    @Override
    protected void doDestroy() {
        lobbyMgr.onDestroy();
        super.doDestroy();
    }

}