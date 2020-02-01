// Date: 2019/03/10
// Author: dylan
// Desc: 捕鱼玩家库存接口

package frame.storageLogic;

public interface FishPlayerStorageProtocol {

    /**
     * 获取玩家叠加概率
     */
    public float getAddProb();

    /**
     * 获取玩家叠加概率
     */
    public long addPump(long deltaPump);
}