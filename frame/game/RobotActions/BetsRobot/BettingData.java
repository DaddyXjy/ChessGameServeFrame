package frame.game.RobotActions.BetsRobot;

import java.util.ArrayList;

import frame.util.RandomUtil;

public class BettingData {
    public int doorIndex;
    public ArrayList<Long> chipsIndex = new ArrayList<Long>();
    private int bettingCount = 0;

    public BettingData(int index, ArrayList<Long> tChipsIndex) {
        doorIndex = index;
        chipsIndex.addAll(tChipsIndex);
    }

    // 发送一次下注 会清理一次筹码列表
    public BettingData SendBetting(int willBettingCount) {
        if (chipsIndex == null) {
            return null;
        }
        if (chipsIndex.size() <= 0) {
            return null;
        }
        // 如果是最后一次下注 全部发送
        if (willBettingCount <= bettingCount) {
            return this;
        }
        int index = RandomUtil.ramdom(0, chipsIndex.size() - 1);
        ArrayList<Long> chips = new ArrayList<Long>();
        if (index == 0) {
            chips.add(chipsIndex.get(0));
            chipsIndex.remove(0);
        } else {
            try {
                for (int i = 0; i < index; i++) {
                    chips.add(chipsIndex.get(0));
                    chipsIndex.remove(0);
                }
            } catch (Exception ex) {
                return null;
            }
        }

        bettingCount++;
        return new BettingData(doorIndex, chips);
    }

}