//Date: 2019/03/05
//Author: dylan
//Desc: 捕鱼库存管理

package frame.storageLogic;

import frame.game.*;
import frame.*;
import frame.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import frame.socket.common.proto.LobbySiteRoom;
import frame.socket.common.proto.Storage.StorageConfig;

import lombok.Getter;
import lombok.Setter;

public class FishStorageMgr {

    // 库存配置
    private StorageConfig storageConfig;

    // 大鱼库存
    private @Setter @Getter long bigFishStorage = 0;
    private @Setter @Getter long smallFishStorage = 0;

    // 当前抽水总分
    private long pumpAllScore = 0;

    // 体验房增加概率
    private float freeRoomAddProb = 1.5f;

    // 机器人叠加概率
    private float robotAddProb = 0.8f;

    // 所属房间
    private Room room;
    private @Getter FishStorageConfig config;

    // 库存总衰减
    private @Setter @Getter long totalStorageReduce = 0;

    public FishStorageMgr(Room room) {
        this.config = new FishStorageConfig();
        this.room = room;
        this.storageConfig = StorageConfig.newBuilder().build();
    }

    public void loadCfg(long roomField) {
        this.config.load(room.getFishRoomCfg().getRoomField());
    }

    public void updateStorageConfig(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
        log.info("更新库存配置成功");
        log.info(getStorageTextInfo());
    }

    public void updateStorageReduce(int reduce) {
        storageConfig = storageConfig.toBuilder().setStorageReduce(reduce).build();
        log.info("更新 {} 库存衰减值成功,库存当前衰减值:{}", room.getRoomTextInfo(), reduce);
    }

    public void updateCurrentStorage(long currentSmallFishStorage, long currentBigFishStorage) {
        smallFishStorage = currentSmallFishStorage;
        bigFishStorage = currentBigFishStorage;
        log.info("更新 {} 库存当前值成功,小鱼库存当前值:{}, 大鱼库存当前值:{}", room.getRoomTextInfo(), smallFishStorage, bigFishStorage);
    }

    // 抽水值
    public float getPump() {
        return room.getFishRoomCfg().getPumpRate();
    }

    /**
     * 抓鱼
     * 
     * @param: fishPlayer  打鱼玩家
     * @param: fishType    鱼的类型
     * @param: fishScore   鱼分数
     * @param: bulletScore 打炮钱
     * 
     * @return 是否抓住
     */
    public boolean catchFish(FishPlayerStorageProtocol fishPlayer, FishBase fish, int fishScore, long bulletScore) {
        int fishType = fish.getType();
        int fishMul = (int) (fishScore / bulletScore);
        boolean bigFish = isBigFish(fish, fishMul);
        boolean catachFish = false;
        float catchProb = RandomUtil.ramdom((float) 1.0);
        float fishBaseProb = fish.getFishBaseProb();
        float fishProb = fishBaseProb;
        do {
            if (room.isFreeRoom()) {
                if (fishPlayer instanceof Player) {
                    fishProb *= freeRoomAddProb;
                }
            }
            if (fishPlayer instanceof Robot) {
                fishProb *= robotAddProb;
            }
            if (!room.isFreeRoom() && fishPlayer instanceof Player) {
                Player player = (Player) fishPlayer;
                log.info("================{} 玩家{} 打鱼开始=================", room.getRoomTextInfo(),
                        player.getPlayerInfo());
                // 单控不考虑库存
                if (this.config.isOpenStorageCtrl && !player.isSystemControl()) {
                    if (!isStorageEnough(bigFish, fishScore)) {
                        log.info("库存不够,无法打死鱼. 子弹分数:{} 鱼类型:{} , 鱼分:{}", bulletScore / Config.MONEY_RATIO_FLOAT, fishType,
                                fishScore / Config.MONEY_RATIO_FLOAT);
                        fishProb = 0;
                        catachFish = false;
                        break;
                    }
                }

                //2019年8月8日,产品richard新需求,需求任务号:#1754.
                //被单控玩家（单控金额为负）需要打不死100倍以上的鱼也打不死道具鱼
                if(player.isSystemControl() && player.getLeftControlMoney()<0){
                    if(fishMul >= 100 || fish.isMagicFish() || fish.isBoomFish()){
                        log.info("被单控玩家（单控金额为负）需要打不死100倍以上的鱼也打不死道具鱼,触发成功,抓鱼失败");
                        catachFish = false;
                        break;
                    }
                }
                if (fishPlayer.getAddProb() > 1.0f) {
                    fishProb *= fishPlayer.getAddProb();
                    log.info("玩家个人捕鱼概率增加:{}", fishPlayer.getAddProb());
                }

                // 判断首击必杀
                if (isOneMustKill(player, fish, fishScore)) {
                    catachFish = true;
                    break;
                }
                fish.addHitCount(player.uniqueId);

                // 叠加库存档位控制(乘法)
                if (this.config.isOpenTotalStorageCtrl) {
                    fishProb *= getTotalFishProb(bigFishStorage + smallFishStorage);
                } else {
                    if (this.config.isOpenStorageCtrl) {
                        if (bigFish) {
                            float bigFishProb = getBigFishProb(bigFishStorage);
                            fishProb *= bigFishProb;
                            log.info("大鱼档位概率:{}", bigFishProb);
                        } else {
                            float samllFishProb = getSmallFishProb(smallFishStorage);
                            fishProb *= samllFishProb;
                            log.info("小鱼档位概率:{}", samllFishProb);
                        }
                    }
                }
                // 赢分档位控制(减法)
                if (isAddPlayerWinControl(player, bulletScore, fishMul)) {
                    float winProb = getWinProb(player.getFishTotalWinMoney());
                    fishProb += winProb;
                    log.info("玩家当前赢分概率叠加:{}", winProb);
                }

                // 单控控制(乘法)
                if (player.isSystemControl()) {
                    log.info("玩家当前单控叠加概率:{}", player.getControlFishRate() / 100.0f);
                    fishProb = fishProb * player.getControlFishRate() / 100.0f;
                }
            }
            if (catchProb <= fishProb) {
                catachFish = true;
                break;
            } else {
                catachFish = false;
                break;
            }
        } while (false);
        if (!room.isFreeRoom() && fishPlayer instanceof Player) {
            long systemWinScore = 0;
            long pumpScore = getPumpScore(bulletScore);
            long reduceScore = getStorageReduceScore(bulletScore);
            totalStorageReduce += reduceScore;
            systemWinScore = pumpScore + reduceScore;
            pumpAllScore += systemWinScore;
            fishPlayer.addPump(pumpScore);
            addStorage(bigFish, bulletScore - systemWinScore);
            Player player = (Player) fishPlayer;
            if (catachFish) {
                addStorage(bigFish, -fishScore);
                player.addFishTotalWinMoney(fishScore - bulletScore + systemWinScore);
            } else {
                player.addFishTotalWinMoney(-bulletScore + systemWinScore);
            }
            log.info("{}, 当前捕鱼赢分:{}", player.getPlayerInfo(), player.getFishTotalWinMoney() / Config.MONEY_RATIO_FLOAT);
            if (!room.isFreeRoom()) {
                log.info("{} 炮弹分数:{} , 鱼的类型:{} , 鱼分:{} , 鱼的基础概率:{}  , 玩家概率:{} , 结果:{}", player.getPlayerInfo(),
                        bulletScore / Config.MONEY_RATIO_FLOAT, fishType, fishScore / Config.MONEY_RATIO_FLOAT,
                        fishBaseProb, fishProb, catachFish ? "命中" : "未命中");
                log.info("当前炮弹抽水值:{} , 当前炮弹衰减值:{}", pumpScore / Config.MONEY_RATIO_FLOAT,
                        reduceScore / Config.MONEY_RATIO_FLOAT);
                if (bigFish) {
                    log.info("{} 当前大鱼库存:{} , (抽水+衰减) 总分:{}", room.getRoomTextInfo(),
                            this.bigFishStorage / Config.MONEY_RATIO_FLOAT,
                            this.pumpAllScore / Config.MONEY_RATIO_FLOAT);
                } else {
                    log.info("{} 当前小鱼库存:{} , (抽水+衰减) 总分:{}", room.getRoomTextInfo(),
                            this.smallFishStorage / Config.MONEY_RATIO_FLOAT,
                            this.pumpAllScore / Config.MONEY_RATIO_FLOAT);
                }
            }
            if (player.isSystemControl()) {
                // 更新剩余单控金额
                player.addLefeControlMoney(-bulletScore);
                if (catachFish) {
                    player.addLefeControlMoney(fishScore);
                }
                // 记录单控日志
                log.info("单控玩家成功, {}", player.getPlayerControlInfo());
            }
            log.info("====================打鱼结束====================");
        }
        return catachFish;
    }

    /**
     * 判断库存是否满足
     * 
     * @param: isBigFish       是否是大鱼
     * @param: systemLoseScore 系统输钱
     */
    private boolean isStorageEnough(boolean isBigFish, long systemLoseScore) {
        long startStorage = -1000000 * 1000;
        if (this.config.isOpenTotalStorageCtrl) {
            long totalStorage = bigFishStorage + smallFishStorage;
            return totalStorage - systemLoseScore > startStorage;
        } else {
            if (isBigFish) {
                return bigFishStorage - systemLoseScore > startStorage;
            } else {
                return smallFishStorage - systemLoseScore > startStorage;
            }
        }
    }

    /**
     * 增加库存
     * 
     * @param: isBigFish 是否是大鱼
     * @param: 获得积分
     */
    private void addStorage(boolean isBigFish, long score) {
        if (!this.config.isOpenStorageCtrl) {
            return;
        }
        if (isBigFish) {
            bigFishStorage += score;
        } else {
            smallFishStorage += score;
        }
    }

    /**
     * 计算衰减分数
     * 
     * @param: bulletScore 炮钱
     */
    private long getStorageReduceScore(long bulletScore) {
        int storageReduce = this.storageConfig.getStorageReduce();
        if (storageReduce < 0 || storageReduce >= 100) {
            log.error("衰减比例为:{},超过允许范围值 0.0~1.0", storageReduce);
        }
        long reduceScore = (long) (bulletScore * (storageReduce / 100.0));
        if (storageReduce > 0 && reduceScore <= 0) {
            reduceScore = 1L;
        }
        return reduceScore;
    }

    /**
     * 计算抽水分数
     * 
     * @param: bulletScore 炮钱
     */
    private long getPumpScore(long bulletScore) {
        float pump = getPump();
        if (pump < 0 || pump >= 1) {
            log.error("抽水比例为:{},超过允许范围值 0.0~1.0", pump);
        }
        long pumpScore = (long) (bulletScore * pump);
        if (pump > 0 && pumpScore <= 0) {
            pumpScore = 1L;
        }
        return pumpScore;
    }

    /**
     * 是否是大鱼
     * 
     * @param: player   渔夫
     * @param: fishType 鱼的类型
     * @param: fishMul  鱼的倍率
     */
    private boolean isBigFish(FishBase fish, int fishMul) {
        return fishMul > 30 || fish.isBoomFish() || fish.isMagicFish();
    }

    /**
     * 获取玩家赢分档位概率
     * 
     * @param: playerTotalWinScore 玩家总赢分
     */
    private float getWinProb(long playerTotalWinScore) {
        return getProb(this.config.playerWinStorageMaxList, this.config.playerWinStorageMulList, playerTotalWinScore);
    }

    /**
     * 获取大鱼档位概率
     * 
     * @param: curBigFishStorage 大鱼当前库存值
     */
    private float getBigFishProb(long curBigFishStorage) {
        return getProb(this.config.bigFishStorageMaxList, this.config.bigFishStorageMulList, curBigFishStorage);
    }

    /**
     * 获取小鱼档位概率
     * 
     * @param: curSmallFishStorage 大鱼当前库存值
     */
    private float getSmallFishProb(long curSmallFishStorage) {
        return getProb(this.config.smallFishStorageMaxList, this.config.smallFishStorageMulList, curSmallFishStorage);
    }

    /**
     * 获取总库存档位概率
     * 
     * @param: curTotalStorage 当前总库存值
     */
    private float getTotalFishProb(long curTotalStorage) {
        return getProb(this.config.totalStorageMaxList, this.config.totalStorageMulList, curTotalStorage);
    }

    private float getProb(ArrayList<Long> maxList, ArrayList<Float> mulList, long score) {
        for (int i = 0; i < maxList.size(); i++) {
            if (maxList.get(i) > score) {
                int index = i - 1;
                index = index < 0 ? 0 : index;
                return mulList.get(index);
            }
        }
        return mulList.get(mulList.size() - 1);
    }

    /**
     * 是否首击必杀
     * 
     * @param: player    捕鱼玩家
     * @param: fish      捕获的鱼
     * @param: fishScore 鱼分
     */
    private boolean isOneMustKill(Player player, FishBase fish, long fishScore) {
        boolean firstHit = fish.isFirstHit(player.uniqueId);
        if (firstHit) {
            log.info("{} 第一次打中鱼:{} , 进入首击必杀判断", player.getPlayerInfo(), fish.getType());
            if (player.getFishTotalWinMoney() < 0 && Math.abs(player.getFishTotalWinMoney()) >= fishScore) {
                float oneMustKillProb = fish.getFishOneMustKillProb();
                float randomProb = RandomUtil.ramdom(1.0f);
                log.info("{} 首击必杀条件基本条件满足(命中收益<=(-玩家个人赢分)),开始进入概率判断,必杀概率:{} , 随机值:{}", player.getPlayerInfo(),
                        oneMustKillProb, randomProb);
                if (randomProb <= oneMustKillProb) {
                    log.info("{} 鱼:{} 首击必杀成功", player.getPlayerInfo(), fish.getType());
                    return true;
                }
            } else {
                log.info("{} 首击必杀条件基本条件不满足(命中收益<=(-玩家个人赢分)) ,必杀失败", player.getPlayerInfo());
            }
        }
        if (firstHit) {
            log.info("{} 鱼:{} 首击必杀失败", player.getPlayerInfo(), fish.getType());
        }
        return false;
    }

    /**
     * 是否叠加个人赢分概率
     * 
     * @param: player      捕鱼玩家
     * @param: bulletScore 子弹分数
     * @param: fishMul     鱼的倍率
     */
    private boolean isAddPlayerWinControl(Player player, long bulletScore, int fishMul) {
        if (this.config.isOpenPlayerWinCtrl) {
            if (player.getFishTotalWinMoney() < 0) {
                long maxAddFishMul = Math.abs(player.getFishTotalWinMoney()) / bulletScore;
                log.info("{} 当前鱼的倍数:{} , 生效鱼的倍数:{}", player.getPlayerInfo(), fishMul, maxAddFishMul);
                if (maxAddFishMul >= fishMul) {
                    log.info("玩家赢分为负,修正概率成功");
                    return true;
                } else {
                    log.info("玩家赢分为负,修正概率失败");
                }
            } else {
                if (!this.config.isOpenStorageCtrl) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 房间库存信息
     */

    public String getStorageTextInfo() {
        StringBuilder storageBuilder = new StringBuilder();
        storageBuilder.append("\n当前库存信息:");
        storageBuilder.append("\n\t\t库存起始值:");
        storageBuilder.append(this.storageConfig.getStorageStart());
        storageBuilder.append("\n\t\t库存衰减值:");
        storageBuilder.append(this.storageConfig.getStorageReduce());
        return storageBuilder.toString();
    }

}