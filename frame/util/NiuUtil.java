package frame.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NiuUtil {
    public static class NiuResout {
        public int cow;
        public int[] indexs;
    }

    public static NiuResout getNiu(List<Integer> cards, int N) {
        List<Integer> data = new ArrayList<>();
        for (int v : cards) {
            if (pukeUtil.getValue(v) > 10) {
                data.add(10);
            } else {
                data.add(pukeUtil.getValue(v));
            }
        }
        NiuResout nn = new NiuResout();
        nn.indexs = new int[3];
        nn.cow = 0;
        // List<NiuResout> result=new ArrayList<>();
        // if(data.size()==4){
        // int td=0;
        // for(int x=0;x<3;x++){
        // for(int y=x+1;y<4;y++){
        // if((data.get(x)+data.get(y))%N==0){
        // result.add(N);
        // }else{
        // td=getMax(data);
        // }
        // }
        // }
        // if(td<10){
        // result.add(td);
        // }
        // }else
        if (data.size() == 5) {
            int temp = 0;
            int r = 0;
            for (int i = 0; i < 3; ++i) {
                for (int j = i + 1; j < 4; ++j) {
                    for (int k = j + 1; k < 5; ++k) {
                        if ((data.get(i) + data.get(j) + data.get(k)) % N == 0) {
                            for (int p = 0; p < data.size(); ++p) {
                                if (p != i && p != j && p != k) {
                                    temp += data.get(p);
                                }
                            }
                            if (temp % N == 0) {
                                r = N;
                            } else {
                                r = temp % N;
                            }
                            nn.cow = r;
                            nn.indexs[0] = i;
                            nn.indexs[1] = j;
                            nn.indexs[2] = k;
                            return nn;
                        }
                    }
                }
            }
            if (r == 0) {
                nn.cow = r;
                return nn;
            }
        } else {
            return null;
        }
        return nn;
    }

    public static NiuResout getNiu(List<Integer> data) {
        return getNiu(data, 10);
    }

    public static NiuResout getNiu(Integer[] data) {
        return getNiu(new ArrayList<Integer>(Arrays.asList(data)), 10);
    }

    // private static int getMax(List<Integer> tempData){
    // int temp=(tempData.get(0)+tempData.get(1))%10;
    // for(int i=0;i<tempData.size()-1;++i){
    // for(int j =i+1;j<tempData.size();++j){
    // if(((tempData.get(i)+tempData.get(j))%10)>temp){
    // temp=(tempData.get(i)+tempData.get(j))%10;
    // }
    // }
    // }
    // return temp;
    // }

    // 返回牛牛五花一下
    public static int getNiuType(int[] cards) {
        int type = 12;
        for (int j = 0; j < cards.length; ++j) {
            if (pukeUtil.getValue(cards[j]) < 10) {
                type = 10;
                break;
            } else if (pukeUtil.getValue(cards[j]) < 11) {
                if (type == 12) {
                    type = 11;
                } else {
                    type = 10;
                    break;
                }
            }
        }
        return type;
    }

    public static int getCow(int[] cards) {
        int cow = 0;
        if (pukeUtil.getBigNum(cards, 5) == 0) {
            int count = 0;
            for (int i = 0; i < cards.length; ++i) {
                count += pukeUtil.getValue(cards[i]);
            }
            if (count <= 10) {
                return 14;
            }
        }
        if (pukeUtil.checkBoom(cards, 4).size() > 0) {
            return 13;
        }
        cow = getNiu(Arrays.stream(cards).boxed().collect(Collectors.toList())).cow;
        if (cow == 10) {
            cow = getNiuType(cards);
        }
        return cow;
    }

}