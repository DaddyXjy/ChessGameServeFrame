package frame.game.RobotActions.RobotFrame;

// 游戏状态
public class RobotGameState {

    public static class GameState {
        public int code;
        public String gameState;

        public GameState(int code, String gameState) {
            this.code = code;
            this.gameState = gameState;
        }
    }

    // 下注类游戏状态
    public final static GameState BETS_GAMESTATE_NULL = new GameState(0, "空闲状态");
    public final static GameState BETS_GAMESTATE_BEGIN = new GameState(1, "开始状态");
    public final static GameState BETS_GAMESTATE_BETTING = new GameState(2, "下注状态");
    public final static GameState BETS_GAMESTATE_STOP_BETTING = new GameState(3, "停止下注");
    public final static GameState BETS_GAMESTATE_END = new GameState(4, "结束状态");
    // 对战类游戏状态
    public final static GameState GAMESTATE_READY = new GameState(0, "准备状态");
    public final static GameState GAMESTATE_BEGIN = new GameState(1, "开始状态");
    public final static GameState GAMESTATE_END = new GameState(2, "结束状态");
}
