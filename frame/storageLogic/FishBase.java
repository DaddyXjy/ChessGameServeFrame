//Date: 2019/05/19
//Author: dylan
//Desc: 鱼基类

package frame.storageLogic;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

public abstract class FishBase {

    private @Getter @Setter long id;
    protected @Getter int type;

    private HashMap<Long, Integer> hitCountList = new HashMap<Long, Integer>();

    public FishBase() {

    }

    public FishBase(long fishid, int t) {
        id = fishid;
        type = t;
    }

    public void baseInit() {
        hitCountList = new HashMap<Long, Integer>();
    }

    // 是否是首击
    public boolean isFirstHit(long uniqueId) {
        return !hitCountList.containsKey(uniqueId);
    }

    // 增加击打次数
    public void addHitCount(long uniqueId) {
        if (hitCountList.get(uniqueId) == null) {
            hitCountList.put(uniqueId, 1);
        } else {
            hitCountList.put(uniqueId, hitCountList.get(uniqueId) + 1);
        }
    }

    // 获取击打次数
    public int getHitCount(long uniqueId) {
        if (hitCountList.get(uniqueId) == null) {
            return 0;
        } else {
            return hitCountList.get(uniqueId) + 1;
        }
    }

    // 获取捕鱼基础概率
    public abstract float getFishBaseProb();

    // 获取捕鱼必杀概率
    public abstract float getFishOneMustKillProb();

    // 是否是道具鱼
    public abstract boolean isMagicFish();

    // 是否是炸弹鱼
    public abstract boolean isBoomFish();

}