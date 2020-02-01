package frame.util;

import java.util.ArrayList;
import java.util.List;

public class pukeUtil {
    private ArrayList<Integer> pukes = new ArrayList<>();
    private ArrayList<Integer> allPukes = new ArrayList<>();
    public int shuffleTime = 200;

    public int getPuke() {
        return pukes.remove(0);
    }

    public pukeUtil(int... tt) {
        for (Integer p : tt) {
            allPukes.add(p);
            pukes.add(p);
        }
    }

    public pukeUtil() {
        int[] temp = { 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80, 0x90, 0xa0, 0xb0, 0xc0, 0xd0, 0x11, 0x21, 0x31,
                0x11, 0x51, 0x61, 0x71, 0x81, 0x91, 0xa1, 0xb1, 0xc1, 0xd1, 0x12, 0x22, 0x32, 0x42, 0x52, 0x62, 0x72,
                0x82, 0x92, 0xa2, 0xb2, 0xc2, 0xd2, 0x13, 0x23, 0x33, 0x43, 0x53, 0x63, 0x73, 0x83, 0x93, 0xa3, 0xb3,
                0xc3, 0xd3, 0x14, 0x24 };
        for (Integer p : temp) {
            allPukes.add(p);
            pukes.add(p);
        }
    }

    public static int getValue(int p) {
        return p >> 4 & 0xf;
    }

    public static int getColor(int p) {
        return p & 0xf;
    }
    public static int getBigNum(int[] cards, int value) {
        int p = 0;
        for (int i = 0; i < cards.length; ++i) {
            if (getValue(cards[i]) > value) {
                p++;
            }
        }
        return p;
    }

    public static int[] sortCount(int[] cards) {
        int[] counts = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        for (int i = 0; i < cards.length; ++i) {
            counts[getValue(cards[i])]++;
        }
        return counts;
    }

    public static List<Integer> checkBoom(int[] cards, int BomNum) {
        int[] counts = sortCount(cards);
        List<Integer> res = new ArrayList<>();
        if (cards.length > 15) {
            for (int p = 0; p < counts.length; ++p) {
                if (counts[p] >= BomNum) {
                    res.add(p);
                }
            }
        } else {
            for (int p = 0; p < cards.length; ++p) {
                if (counts[getValue(cards[p])] >= BomNum) {
                    res.add(getValue(cards[p]));
                }
            }
        }
        return res;
    }

    public static int getMxa(int[] cards) {
        int max = cards[0];
        for (int i = 1; i < cards.length; ++i) {
            if (cards[i] > max) {
                max = cards[i];
            }
        }
        return max;
    }

    public void shuffle() {
        pukes.clear();
        pukes = new ArrayList<>(allPukes);
        for (int i = 0; i < shuffleTime; ++i) {
            int rand = RandomUtil.ramdom(0, pukes.size() - 1);
            changePukePos(0, rand);
        }
    }

    public List<Integer> getColorPuke(int color) {
        List<Integer> cards = new ArrayList<>();
        for (Integer puke : pukes) {
            if (getColor(puke) == color) {
                cards.add(puke);
            }
        }
        return cards;
    }

    public List<Integer> getValuePuke(int value) {
        List<Integer> cards = new ArrayList<>();
        for (Integer puke : pukes) {
            if (getValue(puke) == value) {
                cards.add(puke);
            }
        }
        return cards;
    }

    public void changePukePos(int pos1, int pos2) {
        Integer temp = pukes.get(pos1);
        pukes.set(pos1, pukes.get(pos2));
        pukes.set(pos2, temp);
    }

    public void add(int pos, int card) {
        pukes.add(pos, card);
    }

    public void add(int pos, int card[]) {
        for (int i = card.length - 1; i >= 0; --i) {
            pukes.add(pos, card[i]);
        }
    }
}