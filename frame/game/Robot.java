package frame.game;

import frame.Callback;
import frame.Config;
import frame.game.proto.GameBase.EmotionReq;
import frame.log;
import frame.socket.BaseResponse;
import frame.socket.Request;
import frame.socket.common.proto.Type.Server_Type;
import frame.util.RandomNameUtil;
import frame.util.RandomUtil;
import lombok.Getter;

import java.util.HashMap;

public abstract class Robot extends Role {
	public enum Type {
		Bold, Nomal, Timid
	};

	public Type type;

	private static int index = 0;
	private static int userIDIndex = -1;
	public long maxTableLifeTime = 0;
	// private static int genNameIndex = 0;

	// 机器人魔法表情类
	private @Getter RobotExpression expression;

	public void init() {
		int random = (int) (Math.random() * 100);
		int inc = 0;
		for (HashMap.Entry<Type, Config.RobotConfig> entry : Config.ROBOTTYPE.entrySet()) {
			inc += entry.getValue().bornRate;
			if (inc >= random) {
				type = entry.getKey();
				break;
			}
		}
		uniqueId = ++index;
		userId = userIDIndex--;
		super.init();
		log.debug("生成一个机器人:{}", index);
	}

	@Override
	public Config.Error exitHall() {
		super.exitHall();
		return exitRoom();
	}

	// 是否在桌子中生命周期结束
	public boolean isTableLifeTimeOver() {
		return getTableLifeTime() > maxTableLifeTime;
	}

	// 是否可以被踢出
	public boolean isCanKickOut() {
		return true;
	}

	public void onMsg(Request req) {

	}

	public void send(BaseResponse response) {

	}

	public void send2Route(BaseResponse response, Server_Type targetServerType) {
	}

	public void send2Route(BaseResponse response, Server_Type targetServerType, Callback callback) {
	}

	@Override
	public void reply2Route(Request receivedRequest, BaseResponse response) {
	};

	@Override
	public boolean isPairOnGaming() {
		// test by zy 2019.3.24
		return GameMain.getInstance().getGameMgr().getRobotConfig().GetIsRobotPairOnGameing();
		// return
		// GameMain.getInstance().getGameMgr().getRobotPairType().isRobotPairOnGameing;
	};

	@Override
	protected void onEnterRoom() {
		gender = RandomUtil.ramdom(1);
		nickName = RandomNameUtil.randomName(gender);
		portrait = String.valueOf(RandomUtil.ramdom(0, 5));
		int minMoney = 100000;
		int maxMoney = 999999;
		money = RandomUtil.ramdom(minMoney, maxMoney);
	}

	@Override
	protected void initRobotExpression() {
		expression = new RobotExpression(this);
		// 随机选择一名玩家发送表情
		// expression.isDealExpression(this);
	}

	public void receiveExpression(EmotionReq emoTionReq) {
		expression.receiveExpressionHandler(emoTionReq);
	}
}