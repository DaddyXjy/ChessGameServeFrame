package frame.game;

import frame.Callback;
import frame.Timer;
import frame.UtilsMgr;
import frame.game.RobotActions.PkRobot.RobotPkLogic;
import frame.game.proto.GameBase;
import frame.game.proto.GameBase.EmotionReq;
import frame.log;
import frame.socket.Response;
import frame.util.RandomUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 机器人魔法表情策略
 * @author terry
 */
@Data
public class RobotExpression {
	// 所属的机器人
	Robot robot;
	/** 表情攻击初始触发概率*/
	private int initProb = 20; // 默认是30
	/** 表情攻击触发概率 衰减步长*/
	private int desStep = 10;  // 默认是8
	/** 系统随机出的表情攻击最大次数*/
	private int attackNumberMax = 0;  // 默认是0
	/** 发送表情攻击次数*/
	private int attackNumber = 0;
	// 机器人发送表情攻击的状态 默认不攻击的状态, 用于判定冷却时间的判断
	// true 标识机器人正在冷却
	private boolean coolFlag = false;
	// 冷却时间戳
	public long coolTimeMillisecond = 0;
	/**表情冷却时间*/
	private int cdTime = 7;  // 默认5
	/** 表情攻击最长等待时间 */
	private int waitTime = 15;
	/** 表情反击触发的初始概率*/
	private int initStrikeBackProb = 30;  // 默认是50
	/** 表情反击触发概率衰减步长*/
	private int desStrikeBackStep = 10;
	/** 被攻击的次数 也可以作为是否被攻击的标志*/
	public int beAttackedCount = 0;
	// 把攻击机器人的玩家 保存起来, 机器人发起攻击是, 首先反击, 再执行随机的攻击任务
	// key: player.uniqueId 玩家的唯一ID value:被攻击的表情的类型
	private Map<Long, Integer> attactFromRole = new HashMap<>();
	// 表情攻击最大次数
	private static final int ATTACKNUMBERMAX = 2; // 默认是2
	// 魔法表情类型数量 目前就 4 种
	private static final int EMOTIONTYPES = 3;
	// 概率随机数 按百分比来计算 只要是需要随机的地方都用0 -- 100 之类的随机数
	private static final int PROBABILITY = 100;
	// 通过服务器配置表情的种类
	// 0 鸡 1 花 2 啤酒 3 拖鞋
	private final static Integer[] negEmo = {0, 3};
	private final static Integer[] posEmo = {1, 2};

	public RobotExpression(Robot robot){
		this.robot = robot;
	}


	public void robotTask(int maxWaitTime){

		// 判断机器人是否在攻击冷却时间之内
		// 冷却时间之内机器人不能发动表情攻击
		if( UtilsMgr.getMillisecond() - this.coolTimeMillisecond < cdTime * 1000){
			//log.info("当前时间:{}, 机器人攻击冷却时间:{}, 机器人处于冷却时间中!!!", UtilsMgr.getMillisecond(), coolTimeMillisecond);
			return;
		}

		// 处于休眠的状态
		if(prob(60)){
			freeTimeTask();
		}

		if(this.attactFromRole.size() > 0){
			// 如果机器人被攻击则优先执行反击逻辑
			strikeBack(maxWaitTime);
		}else {
			// 机器人执行攻击逻辑
			randAttackTask(maxWaitTime);
		}
	}

	// 百分之 initProb 输出 tru
	private boolean prob(int initProb){
		int ranWaitTime = RandomUtil.ramdom(100);
		return ranWaitTime <= initProb;
	}

	void freeTimeTask(){
		int freeTime = RandomUtil.ramdom( 5 );
		log.info("机器人:{} 空闲 {}秒", robot.uniqueId, freeTime);
		this.coolTimeMillisecond = UtilsMgr.getMillisecond() + freeTime * 1000;
	}

	// 机器人魔法表情反击任务
	private void strikeBack(int maxWaitTime){

		// 机器人反击的概率
		int ranStrikeBack = RandomUtil.ramdom(PROBABILITY);
		if(ranStrikeBack > this.initStrikeBackProb) {
			return;
		}

		// 在存储攻击信息的队列中取一个反击的对象出来
		Long toPlayerId = getPlayerId();
		if(toPlayerId == null){
			log.error("没有取到被反击的机器人id");
			return;
		}
		log.info("机器人反击!!!attactFromRole:{}", this.attactFromRole.toString());
		// 被攻击的表情
		int backEmoIndes = this.attactFromRole.get(toPlayerId);
		// 从map中删掉要反击的玩家的信息, 只反击一次
		this.attactFromRole.remove(toPlayerId);

		GameBase.EmotionRes.Builder emoTionRes = GameBase.EmotionRes.newBuilder();
		emoTionRes.setFromPlayer(this.robot.uniqueId);
		emoTionRes.setToPlayer(toPlayerId);

		// 反击回是相同类型的表情
		int strikeEmoIdx = sameTypeEmo(backEmoIndes);
		emoTionRes.setEmotionIndex(strikeEmoIdx);
		byte[] val = emoTionRes.build().toByteArray();
		Response resp = new Response(GameMsg.PLAYER_PK_PHIZ_BORADCAST_RES, val);

		// 随机的反击的时间是
		// 因为有可能机器人攻击机器人, 机器人会在发送表情的的过程中就反击
		// 随机的可能是0, 就是马上反击,这个时候客户端表情动画还没发到用户的身上,
		// 所以这个地方加一个3 秒的等待时间
		long randTime = 4 + RandomUtil.ramdom( maxWaitTime - cdTime );
		// 更新机器人冷却的时间戳
		this.coolTimeMillisecond = UtilsMgr.getMillisecond() + randTime * 1000;
		Robot tmpRobot = this.robot;
		Timer t = UtilsMgr.getTaskMgr().createTimer(randTime, new Callback() {
			@Override
			public void func() {
				if(tmpRobot == null || tmpRobot.getTable() == null){
					return;
				}
				tmpRobot.getTable().broadcast(resp);
			}
		});
		// 把机器人表情的定时器加到map中, 用于清除定时器时用
		robot.getTable().getRobotExpressTimer().put(robot.uniqueId, t);
		log.info("机器人还击:fromPlayer:{}, toPlayer:{}, emoTion:{}", this.robot.uniqueId, toPlayerId,  strikeEmoIdx, coolTimeMillisecond);
		// 如果反击的对象是机器人的话, 把反击的信息存储到对方的被攻击的信息当中
		Role toRobot = this.robot.getTable().getRoles().get(toPlayerId);
		if( toRobot instanceof Robot){
			int tmpCount = ((Robot) toRobot).getExpression().getBeAttackedCount() + 1;
			((Robot)toRobot).getExpression().setBeAttackedCount( tmpCount );
			((Robot)toRobot).getExpression().getAttactFromRole().put(robot.uniqueId, backEmoIndes);
		}

		// 机器人表情触发后, 表情触发概率衰减
		this.initStrikeBackProb -= this.desStrikeBackStep;
		log.info("机器人:{}, 表情反击触发的概率衰减至:{}", robot.uniqueId, initStrikeBackProb);
	}

	/**
	 * 机器人随机攻击任务
	 */
	private void randAttackTask(int maxWaitTime) {

		// 检查发送表情的概率 每次发送表情后都衰减
		int attactProb = RandomUtil.ramdom( PROBABILITY );
		if(attactProb > this.initProb) {
			return;
		}
		// 检查是否超过攻击的最大次数
		if(this.attackNumber >= this.attackNumberMax) {
			return ;
		}
		log.info("机器人:{}, 随机概率:{}, 攻击概率: {}", this.robot.uniqueId, attactProb, initProb);
		//检测剩余攻击表情任务
		log.info("判定本局是否达到最大的攻击次数atttackNumber:{}, attackNumberMax:{}", this.attackNumber, this.attackNumberMax);
		activeSendExpression(maxWaitTime);
	}

	/**
	 * 主动发送表情
	 */
	private void activeSendExpression(int maxWaitTime) {

		// 随机攻击表情
		int ranExpression = RandomUtil.ramdom(EMOTIONTYPES);

		// 随机攻击目标
		Long toPlayer = getRandTarget();
		if(null == toPlayer){
			log.info("随机攻击目标为空!!!");
			return;
		}

		GameBase.EmotionRes.Builder emoTionRes = GameBase.EmotionRes.newBuilder();
		emoTionRes.setFromPlayer(this.robot.uniqueId);
		emoTionRes.setToPlayer(toPlayer);
    	emoTionRes.setEmotionIndex(ranExpression);
		byte[] val = emoTionRes.build().toByteArray();
		Response resp = new Response(GameMsg.PLAYER_PK_PHIZ_BORADCAST_RES, val);

		// 随机发送表情的时间
		long randTime = RandomUtil.ramdom( maxWaitTime - cdTime);

		// 更新机器人攻击冷却的的时间戳
		this.coolTimeMillisecond = UtilsMgr.getMillisecond() + randTime * 1000;

		// 服务器向客户端广播表情攻击
		log.info("发送表情攻击:fromPlayer:{}, toPlayer:{}, ranExpression:{}", this.robot.uniqueId, toPlayer, ranExpression);

		Timer t = UtilsMgr.getTaskMgr().createTimer(randTime, new Callback() {
			@Override
			public void func() {
				if(robot == null || robot.table == null){
					return;
				}
				robot.table.broadcast(resp);
			}
		});
		// 把机器人表情的定时器加到map中, 用于清除定时器时用
		robot.getTable().getRobotExpressTimer().put(robot.uniqueId, t);
		log.info("机器人攻击:fromPlayer:{}, toPlayer:{}, emoTion:{}", this.robot.uniqueId, toPlayer,  ranExpression, coolTimeMillisecond);
		// 检查发送表情的概率 每次发送表情后都衰减
		this.initProb -= this.desStep;
		// 发送表情攻击的次数
		this.attackNumber += 1;

		// 如果攻击的对象是机器人的话, 把攻击的信息存储到对方的被攻击的信息当中
		if( robot.getTable().getRoles().get(toPlayer) instanceof Robot){
			Robot toRobot = (Robot) robot.getTable().getRoles().get(toPlayer);
			int tmpCount = toRobot.getExpression().getBeAttackedCount() + 1;
			toRobot.getExpression().setBeAttackedCount( tmpCount );
			log.info("被攻击的机器人将发动攻击的机器人ID和表情的编号存储到记录存储被攻击信息的map中");
			toRobot.getExpression().getAttactFromRole().put(robot.uniqueId, ranExpression);
		}
	}

	// 从同桌的玩家中随机获取一名除自己的玩家uniqueId
	private Long getRandTarget(){
		// 将其放入到一个数组中
		ArrayList<Long> listRoles;
		listRoles = new ArrayList<>(this.robot.getTable().getRoles().keySet());
		// list中存放被机器人攻击的目标, 不能攻击自己, 所以叫机器人从中删掉
		listRoles.remove(this.robot.uniqueId);
		if(listRoles.size() < 1) {
			return null;
		}
		// 随机数组下标
		int pos = RandomUtil.ramdom(PROBABILITY) % (listRoles.size());
		Long toPlayer = listRoles.get(pos);
		log.info("随机攻击,被攻击的玩家:uniqueId: {}", toPlayer);
		return  toPlayer;
	}

	/**
	 * 机器人接收到表情后处理
	 * 	2. 机器人受到多个人同时攻击时，如果执行还击时，在一个还击 cdTime 内（两次攻击的间隔时间），只会执行一次。
	 * 	3. 直接攻击或者还击的时间是不固定的，在公共 cdTime -10秒内进行随机。
	 * @param emoTionReq 接收到的表情的信息
	 */
	public void receiveExpressionHandler(EmotionReq emoTionReq) {
		// 只有对战类的机器人有反击的行为
		if(!(this.robot instanceof RobotPkLogic)) {
			return;
		}
		// 被攻击次数 +1
		this.beAttackedCount++;
		// 把被攻击的信息存储到map中
		this.attactFromRole.put(emoTionReq.getFromPlayer(), emoTionReq.getEmotionIndex());
		log.info("机器人:{} 被玩家:{}攻击", this.robot.uniqueId, emoTionReq.getFromPlayer());
	}

	// 返回相同类型的表情值:
	// 现在的表情的类型是四种 0 1 2 3
	// 0-- 鸡  1-- 花  2-- 啤酒 3--拖鞋
	// 0 3 是一种类型 1 2 是一种类型
	private int sameTypeEmo(int index){
		int tmpIndex;
		if(Arrays.asList(posEmo).contains(index)){
			int i = RandomUtil.ramdom(PROBABILITY) % (posEmo.length);
			tmpIndex = posEmo[i];
			log.info("随机的积极的表情是:{}", tmpIndex);
		}else {
			int i = RandomUtil.ramdom(PROBABILITY) % (negEmo.length);
			tmpIndex = negEmo[i];
			log.info("随机的消极的表情是:{}", tmpIndex);
		}
		log.info("初始表情编号:{}, 随机的同类型的表情编号是: {}", index, tmpIndex);
		return tmpIndex;
	}

	// 给随机出来的机器人 分配表情攻击任务
	public void receiveTask(){
		// 随机出本局最大的攻击次数, 取值范围，默认1 - 3
		this.attackNumberMax = 1 + RandomUtil.ramdom( 1000 )%(ATTACKNUMBERMAX + 1); // 最少有一次攻击
		log.info("机器人:{}, 随机本局最大攻击次数:{}",this.robot.uniqueId, attackNumberMax);
	}

	// 在存储被攻击信息的map中找出一个玩家并反击回去
	private Long getPlayerId(){
		Long tmp = null;
		for (Long i: attactFromRole.keySet()) {
			tmp = i;
			break;
		}
		return tmp;
	}
}
