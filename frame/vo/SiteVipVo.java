package frame.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SiteVipVo implements Serializable {

	private static final long serialVersionUID = -1L;

	// "主键id，更新时有值")
	private Integer id;

	// "厅主id")
	private Integer siteId;

	// "状态(0禁用，1启用)")
	private Integer status;

	// "稽核-倍数（需要稽核时有值）")
	private Integer checkMultip;

	// "VIP说明文本")
	private String vipContent;

	// "更新VIP特权时间")
	private Long updateTime;

	// "更新VIP等级时间")
	private Long updateLevelTime;

	// "VIP特权等级")
	private List<SiteVipLevelVo> vipLevelList = new ArrayList<>();
}
