package frame.model;
import frame.game.*;
import lombok.Data;


@Data
public class GameClose {

    // 游戏名字
    private Integer gameId;

    // 游戏所在服务器ip
    private String gameIp;

    // 1:运行中,2,关闭,3,强制关闭
    private Integer close;

    private GameMain.Status status;

    public GameClose() {
    }

    public GameClose(Integer gameId, Integer close, GameMain.Status status) {
        this.gameId = gameId;
        this.close = close;
        this.status = status;
    }

}
