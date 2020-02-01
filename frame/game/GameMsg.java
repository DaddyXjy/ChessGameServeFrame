//Date: 2019/03/21
//Author: dylan
//Desc: 子游戏框架消息定义
package frame.game;

public final class GameMsg {

	// Q 代表请求 A代表回应

	// 获取所有房间数据
	public final static int GET_ALL_ROOM_DATA = 230;
	// 增加房间
	public final static int ADD_ROOM = 232;
	// 删除房间
	public final static int DEL_ROOM = 234;
	// 更新房间配置
	public final static int UPDATE_ROOM = 236;
	// 开启房间
	public final static int OPEN_ROOM = 238;
	// 禁用
	public final static int FORBID_ROOM = 240;

	// 查所有游戏房间库存配置
	public final static int GET_ALL_ROOM_STORAGE_CFG = 600703;

	// 更新游戏房间库存配置
	public final static int UPDATE_ROOM_STORAGE_CFG = 256;

	// 更新游戏房间库存衰减
	public final static int BATCH_UPDATE_STORAGE_REDUCE = 257;

	// 更新游戏房间当前库存
	public final static int UPDATE_ROOM_STORAGE_VALUE = 600711;

	// 游戏更新DB单控
	public final static int G2DB_UpdateLeftControlMoney = 600605;

	// DB更新游戏单控
	public final static int DB2G_UpdateLeftControlMoney = 600603;

	// 更新下注人数触发库存设置
	public final static int BET_NUM_STORAGE_TRIGGER = 258;

	// 游戏控制开奖请求
	public final static int GAME_LOTTERY_CONTROL_REQ = 600901;

	// 匹配游戏 语音广播
	public final static int PLAYER_PK_VOICE_BORADCAST_REQ = 600902;
	// 匹配游戏 语音广播响应
	public final static int PLAYER_PK_VOICE_BORADCAST_RES = 600903;
	// 匹配游戏 表情广播
	public final static int PLAYER_PK_PHIZ_BORADCAST_REQ = 600904;
	// 匹配游戏 表情广播响应
	public final static int PLAYER_PK_PHIZ_BORADCAST_RES = 600905;
	// DB充值通知
	public final static int DB_CHARGE_NOTIFY = 600507;
	// 游戏充值通知
	public final static int GAME_CHARGE_NOTIFY_PLAYER = 600921;
	// 玩家数据通知
	public final static int GAME_PLAYER_INFO_NOTIFY = 600931;
	// DB更新玩家数据
	public final static int DB_UPDATE_PLAYER_INFO = 600110;
	// 大厅玩家数据通知
	public final static int LOBBY_PLAYER_INFO_NOTIFY = 333;
	
}