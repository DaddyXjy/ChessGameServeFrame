package frame.vo;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class SiteVipLevelVo implements Serializable {

	private static final long serialVersionUID = -1L;

	// "站点ID，前端忽略")
	private Integer siteId;

	// "VIP等级")
	private Integer vipLevel;

	// "VIP打码量")
	private Long vipBet;

	// "晋级礼金")
	private BigDecimal levelAmount;

	// "周礼金")
	private BigDecimal weekAmount;

	// "月礼金")
	private BigDecimal monthAmount;

	// "存款加速通道(0无，1有)")
	private Integer fastChannel;

	// "专属客服经理(0无，1有)")
	private Integer customerService;

}
