package frame.game.RobotActions.RobotFrame;

public abstract class GameData {
    public int GameID;

    public GameData(int GameID) {
        this.GameID = GameID;
    }
    //清空数据
    public abstract void ResetData();

}