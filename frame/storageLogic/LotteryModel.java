//Date: 2019/03/01
//Author: dylan
//Desc: 通用开奖模型

package frame.storageLogic;

import java.util.HashMap;

import frame.game.Player;

public final class LotteryModel {
    // 开奖权重(百分值)
    public int lotteryWeight = 1;
    // 系统赢钱
    public long systemWin = 0;
    // 单控玩家赢钱
    public HashMap<Player, Long> controlPlayerWins = new HashMap<Player, Long>();
    // 开奖结果
    public Object lotteryResult;
}
