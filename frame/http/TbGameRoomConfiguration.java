package frame.http;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * tb_game_room_configuration_doudizhu
 * 
 * @author
 */
@Data
public class TbGameRoomConfiguration extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间id,与tb_game_room的id相对应
     */
    private Long gameRoomId;

    /**
     * 厅主id
     */
    private Long siteId;

    /**
     * 开始时间
     */
    private Integer startTime;

    /**
     * 叫分时间
     */
    private Integer callTime;

    /**
     * 最大倍数
     */
    private Integer maxMultiple;

    /**
     * 出牌时间
     */
    private Integer shotTime;

    /**
     * 库存起始值
     */
    private BigDecimal stockStart;

    /**
     * 当前库存
     */
    private BigDecimal currentStock;

    /**
     * 库存上限
     */
    private BigDecimal stockAtten;
}