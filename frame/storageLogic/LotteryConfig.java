//Date: 2019/03/01
//Author: dylan
//Desc: 开奖配置

package frame.storageLogic;

public final class LotteryConfig {
    public enum LotteryType {
        //系统必赢
        SYSTEM_WIN, 
        //系统必输
        SYSTEM_LOSE,
        //不控制
        NO_CONTROL,
        //单控必赢
        SINGLE_WIN,
        //单控必输
        SINGLE_LOSE,
    }
}
