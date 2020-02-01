//Date: 2019/03/01
//Author: dylan
//Desc: 开奖接口

package frame.storageLogic;

import java.util.ArrayList;

import frame.game.Player;

public interface LotteryProtocol {

    /**
     * 获取所有的开奖模型 说明: 开奖结果为开盘口类游戏(如:奔驰宝马,飞禽走兽),找出所有满足需求的盘口数据
     * 开奖结果为发牌类游戏:随机发牌,尝试互相换牌,组合出牌的结果 开奖结果为摇骰子:组合出所有骰子结果
     * 
     * @param systemControlPlayerList 被系统单控的玩家
     */
    public ArrayList<LotteryModel> getAllLotteryModel(ArrayList<Player> systemControlPlayerList);

}
