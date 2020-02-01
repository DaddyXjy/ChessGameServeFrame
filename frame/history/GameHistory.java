package frame.history;

import java.util.Map;

import lombok.Data;
@Data

public class GameHistory{
    public String gameId;
    public String roomName;
    public long startTime;
    public long endTime;
}