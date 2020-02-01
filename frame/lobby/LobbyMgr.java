package frame.lobby;

import frame.*;
import lombok.Getter;
import lombok.Setter;

public abstract class LobbyMgr {

    public LobbyMgr() {

    }

    public abstract LobbyPlayer createPlayer();

    public LobbyHall createHall(int id) {
        return new LobbyHall(id);
    };

    public void onPrepare() {

    }

    public void onStop() {
    }

    public void onTerminate() {
    }

    public void onDestroy() {
    }
}