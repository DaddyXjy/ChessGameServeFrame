package frame.game;

import lombok.Getter;

/**
 * @author KAZER
 * @version 创建时间：2019年7月13日 下午6:02:07 类说明
 */
@Getter
public class Banker {
	/**
	 * 系统
	 */
	public static final int SYSTEM_BANKER = 1;
	/**
	 * 机器人
	 */
	public static final int ROBOT_BANKER = 2;
	/**
	 * 玩家
	 */
	public static final int PLAYER_BANKER = 3;
	/**
	 * 庄家
	 */
	private Role banker;
	/**
	 * 庄家身份 (1:系统 2:机器人 3：玩家)
	 */
	private int identity;
	/**
	 * 庄家输赢
	 */
	private long winLoseMoney;
	/**
	 * 最大当庄数
	 */
	private int bankerMaxNum;
	/**
	 * 当庄次数
	 */
	private int bankerNum;

	public Banker() {
		createSystemBanker();
	}

	/**
	 * 新建系统当庄
	 */
	protected void createSystemBanker() {
		this.identity = SYSTEM_BANKER;
		this.banker = SystemBanker.instance();
		this.bankerNum = 0;
	}

	/**
	 * 更新庄家(玩家)
	 * 
	 * @param role
	 * @param bankerMaxNum
	 */
	protected void updataBanker(Role role, int bankerMaxNum) {
		if (role instanceof Robot) {
			this.identity = ROBOT_BANKER;
		} else if (role instanceof Player) {
			this.identity = PLAYER_BANKER;
		} else {
			this.identity = SYSTEM_BANKER;
		}
		this.bankerMaxNum = bankerMaxNum;
		this.bankerNum = 0;
		this.banker = role;
		this.winLoseMoney = 0;
	}

	/**
	 * 添加一次当庄次数
	 */
	protected void updataMasterNum() {
		this.bankerNum += 1;
		this.winLoseMoney = 0;
		if(this.bankerNum >= 99999) {
			this.bankerNum = 0;
		}
	}
	
	/**
	 * 保存庄家输赢
	 * @param money
	 */
	protected void saveWinLose(long money) {
		this.winLoseMoney = money;
	}
	
	/**
	 * 
	 * @param num
	 */
	protected void addBankerMaxNum(int num) {
		this.bankerMaxNum += num;
	}
	

}
