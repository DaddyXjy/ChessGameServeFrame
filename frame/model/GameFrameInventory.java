package frame.model;

import lombok.Data;

/**
 * 各个厅主的各个游戏的库存,给游戏框架用
 * @author 
 */
@Data
public class GameFrameInventory {


    /**
     * 厅主库存状态
     */
    private Integer gameId;

    public GameFrameInventory() {
    }


    public GameFrameInventory(Integer gameId) {
        this.gameId = gameId;
    }
}