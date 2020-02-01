package frame.game;

import frame.game.proto.Game.updataTurnConfig;
import lombok.Getter;
import lombok.Setter;

/**
* @author KAZER
* @version 创建时间：2019年6月9日 下午4:04:48
* 类说明
*/
@Setter
@Getter
public class ActivityConfig {
	//转换积分的下注百分比
	private int betRate;
	// 状态1启用，0禁用
	private int status;
	  // 活动开始时间
	private long activityBegin;
	  // 活动结束时间
	private long activityEnd;
	
	
	public ActivityConfig() {
		
	}
	
	public ActivityConfig(updataTurnConfig turnConfig) {
		this.betRate = (int) turnConfig.getPercentage();
		this.status = turnConfig.getStatus();
		this.activityBegin = turnConfig.getStartime();
		this.activityEnd = turnConfig.getEndtime();
	}
}
