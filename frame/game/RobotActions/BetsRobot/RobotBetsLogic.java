package frame.game.RobotActions.BetsRobot;

import frame.game.RobotActions.RobotFrame.RobotBaseConfig;
import frame.game.RobotActions.RobotFrame.RobotLogic;
import frame.util.RandomNameUtil;
import frame.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;

public abstract class RobotBetsLogic extends RobotLogic {

	/** 机器人类型(0下注，1上庄) */
	@Getter @Setter
	public int robotType = 0;
	
	// 性格
	public RobotBetsConfig.RobotCharacter cur_RobotCharacter;

	// 初始化配置 创建机器人的时候应该已经配置完成
	public void InitData(RobotBaseConfig config) {
		super.InitData(config);
	}

	// 更新配置
	public void UpData(RobotBaseConfig config) {
		InitData(config);
		// super.UpData(config);

		// robotConfig = (RobotBetsConfig) config;

		// Init();
	}

	@Override
	// 是否在游戏过程中匹配
	public boolean isPairOnGaming() {
		return true;
	}

	// 子游戏必须自己实现
	// 机器人下注
	public abstract boolean RobotSendBetting(BettingData betting);

	/** 机器人上庄 */
	public abstract boolean RobotSendUpBanker();

	/** 机器人退出上庄列表 */
	public abstract boolean RobotSendDownBanker();

	// 进入房间
	@Override
	protected void onEnterRoom() {
		gender = RandomUtil.ramdom(1);
		nickName = RandomNameUtil.randomName(gender);
		portrait = String.valueOf(RandomUtil.ramdom(0, 5));
		// 携带金钱
		// money = carryScore;
		// log.warn("机器人{},进入房间携带{}", nickName, money / 1000);
	}
}
