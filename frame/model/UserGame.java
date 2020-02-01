package frame.model;

import lombok.Data;

/**
 * 用户游戏状态，在线状态redis更新
 */
@Data
public class UserGame {

	/**
	 * account
	 */
	private String account;

	/**
	 * 厅主id
	 */
	private Long siteId;


	/**
	 *  *当前在哪个游戏中  取值范围： com.micro.common.constant.game.GameConstants
	 */
	private Integer gameStatus;

	/**
	 * *在线状态 1 在线 2 离线
	 */
	private Integer onlineStatus;

	public UserGame(String account, Long siteId, Integer gameStatus, Integer onlineStatus) {
		this.account = account;
		this.siteId = siteId;
		this.gameStatus = gameStatus;
		this.onlineStatus = onlineStatus;
	}
}
