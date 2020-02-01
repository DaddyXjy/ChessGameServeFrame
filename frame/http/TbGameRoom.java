package frame.http;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TbGameRoom extends BaseModel implements Serializable {

	private static final long serialVersionUID = 2011789298643005448L;

	/**
	 * 下注限红1
	 */
	private BigDecimal bottomRed1 = BigDecimal.ZERO;
	/**
	 * 下注限红2
	 */
	private BigDecimal bottomRed2 = BigDecimal.ZERO;

	/**
	 * 厅主id
	 */
	private Long siteId;

	/**
	 * 游戏标识
	 */
	private String gameId;

	/**
	 * 游戏名称
	 */
	private String gameName;

	/**
	 * 游戏类型
	 */
	private String gameType;

	/**
	 * 房间状态 1：正常 2：关闭
	 */
	private Integer roomStatus;

	private String createBy;

	private String createDate;

	private String updateBy;

	private String updateDate;

	/**
	 * 房间标识
	 */
	private String roomNumber;

	/**
	 * 房间名称
	 */
	private String roomName;

	/**
	 * 房间类型 1：普通房间 2：密码房间
	 */
	private Integer roomType;

	/**
	 * 是否密码房间 1：是 2：不是
	 */
	private Integer whetherPassword;

	/**
	 * 房间密码
	 */
	private String roomPassword;

	/**
	 * 桌子数目
	 */
	private Integer tableNum;

	/**
	 * 房间人数
	 */
	private Integer roomPersons;

	/**
	 * 桌子人数
	 */
	private Integer tablePersons;

	/**
	 * 税收比例
	 */
	private BigDecimal taxRatio;

	/**
	 * 房间最低金额
	 */
	private BigDecimal minMoney;

	/**
	 * 收费设置 1,收取服务费 2,税收比例收费
	 */
	private Integer chargeType;

	/**
	 * 收费值
	 */
	private BigDecimal chargeValue;

	/**
	 * 游戏底分
	 */
	private BigDecimal roomField;

	/**
	 * 货币类型
	 */
	private int moneyType;
}