package frame.http;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * tb_game_room_configuration_bet
 * 
 * @author
 */
@Data
public class TbGameRoomConfigurationBet implements Serializable {

	private int betsum1;// '随机局数开始',
	private int betsum2;// '随机局数结束',

	private int stockMaintain1;// '维持局数开始',
	private int stockMaintain2;// '维持局数结束',

	private int sysGold;// '是否允许机器人上庄，1是可以,2是不可以',
	private int robotJoining;// '是否允许机器人加入 1是可以，2是不可以',

	private BigDecimal goldCoin1;// '上庄携带金币1',
	private BigDecimal goldCoin2;// '上庄携带金币2',

	private BigDecimal goldCount1;// '上庄数量1',
	private BigDecimal goldCount2;// '上庄数量2',

	private BigDecimal robotCount1;// '机器人数量1',
	private BigDecimal robotCount2;// '机器人数量2',

	private BigDecimal robotGold1;// '机器人携带金币1',
	private BigDecimal robotGold2;// '机器人携带金币2',

	private Long id;
	/**
	 * 关联tb_game_room表id
	 */
	private Long gameRoomId;

	/**
	 * 空闲时间
	 */
	private Integer freeTime;

	/**
	 * 下注时间
	 */
	private Integer betTime;

	/**
	 * 最低下注条件
	 */
	private BigDecimal lowestBetCondition;

	/**
	 * 库存起始值
	 */
	private BigDecimal stockStart;

	/**
	 * 库存衰减值
	 */
	private BigDecimal stockWeak;

	/**
	 * 库存上限
	 */
	private BigDecimal stockAtten;

	/**
	 * 当前库存
	 */
	private BigDecimal currentStock;

	/**
	 * 上庄条件
	 */
	private BigDecimal bankerCond;

	/**
	 * 坐庄局数
	 */
	private Integer bankerTime;

	/**
	 * 额外条件
	 */
	private BigDecimal addedCond;

	/**
	 * 额外局数
	 */
	private Integer addedTime;

	/**
	 * 是否可以系统上庄 1是可以,2是不可以
	 */
	private Integer sysBanker;

	/**
	 * 税收比例
	 */
	private BigDecimal chargeValue;
	/**
	 * 消息一
	 */
	private String msg1;

	/**
	 * 消息二
	 */
	private String msg2;

	/**
	 * 消息三
	 */
	private String msg3;
}