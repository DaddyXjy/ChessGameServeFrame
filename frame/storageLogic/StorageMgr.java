//Date: 2019/03/01
//Author: dylan
//Desc: 库存管理类(下注和对战通用)

package frame.storageLogic;

import frame.game.*;
import frame.socket.common.proto.LobbySiteRoom;
import frame.socket.common.proto.Storage.StorageGrade;
import frame.socket.common.proto.Storage.StorageConfig;
import frame.storageLogic.LotteryConfig.LotteryType;
import frame.*;
import frame.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class StorageMgr {
    // 库存配置
    private @Getter StorageConfig storageConfig;
    // 当前库存值(千分值)
    public @Getter @Setter long storageCurrent;
    // 系统库存总收益
    private long systemAllStorageProfit;
    // 当前房间系统总收益
    private long systemAllProfit;

    private ArrayList<StorageGrade> sortedStorageGradeList = new ArrayList<StorageGrade>();

    // 所属房间
    private Room room;

    // 是否开启库存档位控制
    private boolean storageLimitControl = true;

    // 是否开启房间概率控制
    private boolean roomControl = false;

    // 库存总衰减
    private @Setter @Getter long totalStorageReduce = 0;

    public StorageMgr(Room room) {
        this.systemAllStorageProfit = 0;
        this.systemAllProfit = 0;
        this.room = room;
        this.storageConfig = StorageConfig.newBuilder().build();
    }

    public void updateCurrentStorage(long currentStorage) {
        storageCurrent = currentStorage;
        log.info("更新 {} 库存当前值成功,库存当前值:{}", room.getRoomTextInfo(), currentStorage);
    }

    public void updateStorageReduce(int reduce) {
        storageConfig = storageConfig.toBuilder().setStorageReduce(reduce).build();
        log.info("更新 {} 库存衰减值成功,库存当前衰减值:{}", room.getRoomTextInfo(), reduce);
    }

    public void updateStorageConfig(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
        sortedStorageGradeList.clear();
        for (StorageGrade storageGrade : this.storageConfig.getStorageGradeList()) {
            sortedStorageGradeList.add(storageGrade);
        }

        // 档位排序(从高到低)
        Collections.sort(this.sortedStorageGradeList, new Comparator<StorageGrade>() {
            @Override
            public int compare(StorageGrade lhs, StorageGrade rhs) {
                if (lhs.getStorageMax() > rhs.getStorageMax()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        if (this.storageConfig.getSystemWinProb() == 0) {
            this.roomControl = false;
        } else {
            this.roomControl = true;
        }
        this.storageLimitControl = true;
        log.info("更新库存配置成功");
        log.info(getStorageTextInfo());
    }

    /**
     * 库存是否足够
     * 
     * @systemWinMoney:系统赢钱
     */
    private boolean isStorageEnough(long systemWinMoney) {
        if (systemWinMoney >= 0) {
            return true;
        } else {
            return this.storageCurrent + systemWinMoney >= this.storageConfig.getStorageStart();
        }
    }

    /**
     * 是否开启库存档位控制
     */

    /**
     * 库存衰减
     */
    private long reduceStorage(long systemWinMoney) {
        // long reduce = this.storageCurrent * this.storageConfig.getStorageReduce() /
        // 100;
        // 调整衰减为只衰减入库金钱:
        long reduce = systemWinMoney * this.storageConfig.getStorageReduce() / 100;
        reduce = reduce < 0 ? 0 : reduce;
        // 系统不输不赢,不衰减
        if (systemWinMoney == 0) {
            reduce = 0;
        }
        this.storageCurrent -= reduce;
        this.totalStorageReduce += reduce;
        this.systemAllStorageProfit += reduce;
        return reduce;
    }

    /**
     * 库存档位控制: 系统必输,如果现在系统大于库存档位值,那就让玩家赢钱
     * 
     */
    private boolean judgeStorageUpLimit(Table table) {
        boolean bSystemLose = false;
        int curPersonNum = table.getBetPlayer() + (table.checkBankerIsPlayer() ? 1 : 0);
        if (room.isBetGame()) {// 只有下注类游戏才加人数控制
            if (curPersonNum <= 0) {
                return false;
            }
            int minPersonNum = room.getHall().getBetTriggerStorage().getMinPersonNum();
            int maxPersonNum = room.getHall().getBetTriggerStorage().getMaxPersonNum();
            int prob = room.getHall().getBetTriggerStorage().getProb();
            int randomPersonNum = RandomUtil.ramdom(minPersonNum, maxPersonNum);
            log.info("下注类游戏人数限定功能开启  配置最小人数:{}，配置最大人数:{}，配置概率:{}%，当前随机出的人数:{}，当前房间中的实际人数:{}", minPersonNum,
                    maxPersonNum, prob, randomPersonNum, curPersonNum);
            boolean bPersonControl = true;
            if (curPersonNum <= randomPersonNum) {
                bPersonControl = RandomUtil.ramdom(1, 100) <= prob;
                if (!bPersonControl) {
                    log.info("人数限定触发，进行档位判定:失败");
                    return bPersonControl;
                }
            }
            log.info("人数限定触发，进行档位判定:成功");
        }

        for (StorageGrade storageGrade : this.sortedStorageGradeList) {
            if (this.storageCurrent > storageGrade.getStorageMax()) {
                log.info("库存当前值:{} , 达到档位值:{} , 档位概率:{}, 系统尝试放水", this.storageCurrent / Config.MONEY_RATIO_FLOAT,
                        storageGrade.getStorageMax() / Config.MONEY_RATIO, storageGrade.getProb());
                bSystemLose = RandomUtil.ramdom(1, 100) <= storageGrade.getProb();
                log.info("档位触发,系统放水:{}", bSystemLose ? "成功" : "失败");
                break;
            }
        }
        return bSystemLose;
    }

    /**
     * 房间概率控制: roll出来房间的输赢
     */
    private boolean judgeRoomProb() {
        return RandomUtil.ramdom(1, 100) <= this.storageConfig.getSystemWinProb();
    }

    /**
     * 玩家单控触发: roll出来是否单控
     */
    private boolean judgePlayerControlProb(Player player) {
        return RandomUtil.ramdom(1, 100) <= player.getControlBetRate();
    }

    /**
     * 改变库存
     * 
     * @systemWinMoney:系统赢钱
     */
    private void changeStorage(Table table, long systemWinMoney) {
        long storageBegin = this.storageCurrent;
        this.storageCurrent += systemWinMoney;
        long reduce = reduceStorage(systemWinMoney);
        this.systemAllProfit += systemWinMoney;
        if (systemWinMoney != 0) {
            log.info("{} 库存起始值:{} , 该局游戏开始库存值:{} , 当局库存衰减值:{} , 当局系统输赢:{} , 该局游戏结束剩余库存:{}", getLotteryTextInfo(table),
                    storageConfig.getStorageStart(), storageBegin / Config.MONEY_RATIO_FLOAT,
                    reduce / Config.MONEY_RATIO_FLOAT, systemWinMoney / Config.MONEY_RATIO_FLOAT,
                    this.storageCurrent / Config.MONEY_RATIO_FLOAT);
        }
    }

    /**
     * 是否单控模式
     * 
     * @lotteryType:开奖类型
     */
    private boolean isPlayerControlMode(LotteryType lotteryType) {
        if (lotteryType == LotteryType.SINGLE_LOSE || lotteryType == LotteryType.SINGLE_WIN) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 计算开奖类型
     * 
     */
    private LotteryConfig.LotteryType judgeLotteryType(ArrayList<LotteryModel> lotteryModelList, Table table) {
        LotteryConfig.LotteryType lotteryType = LotteryConfig.LotteryType.NO_CONTROL;
        do {
            Player player = table.getSystemControlPlayer();
            if (!table.getRoom().isFreeRoom() && player != null) {
                log.info("{} 玩家尝试单控 : {}", getLotteryTextInfo(table), player.getPlayerControlInfo());
                boolean controlPlayerJoinGame = isControlPlayerJoinGame(lotteryModelList, player);
                boolean judgeControlProb = judgePlayerControlProb(player);
                boolean canPlayerControl = controlPlayerJoinGame && judgeControlProb;
                // 单控需要一定几率触发
                if (canPlayerControl) {
                    if (player.getControlMoney() > 0) {
                        ArrayList<LotteryModel> filteredModel = filterLotteryModel(lotteryModelList,
                                LotteryConfig.LotteryType.SINGLE_WIN, player);
                        if (filteredModel.size() > 0) {
                            lotteryType = LotteryConfig.LotteryType.SINGLE_WIN;
                            log.info("{} 单控触发成功, 单控必赢 {}", getLotteryTextInfo(table), player.getPlayerControlInfo());
                            break;
                        } else {
                            log.info("{} 玩家单控必赢触发失败 : 失败原因,结果没有必赢");
                        }
                    } else if (player.getControlMoney() < 0) {
                        ArrayList<LotteryModel> filteredModel = filterLotteryModel(lotteryModelList,
                                LotteryConfig.LotteryType.SINGLE_LOSE, player);
                        if (filteredModel.size() > 0) {
                            lotteryType = LotteryConfig.LotteryType.SINGLE_LOSE;
                            log.info("{} 单控触发成功, 单控必输 {}", getLotteryTextInfo(table), player.getPlayerControlInfo());
                            break;
                        } else {
                            log.info("{} 玩家单控必输触发失败 : 失败原因,结果没有必输", getLotteryTextInfo(table));
                        }
                    }
                } else {
                    if (!controlPlayerJoinGame) {
                        log.info("{} 单控玩家:{} 没有参与游戏,单控失败", getLotteryTextInfo(table), player.getPlayerControlInfo());
                    }
                    if (!judgeControlProb) {
                        log.info("{} 玩家单控触发失败 : 失败原因,没有roll到单控概率", getLotteryTextInfo(table));
                    }
                }
            }
            if (!table.getRoom().isFreeRoom() && this.storageLimitControl && judgeStorageUpLimit(table)) {
                log.info("{} 房间库存档位触发成功,开始启动放水,系统该局必输", getLotteryTextInfo(table));
                lotteryType = LotteryConfig.LotteryType.SYSTEM_LOSE;
                break;
            }

            // 体验房概率写死
            if (table.getRoom().isFreeRoom()) {
                boolean freeRoomProb = RandomUtil.ramdom(1, 100) <= 35;
                if (freeRoomProb) {
                    lotteryType = LotteryConfig.LotteryType.SYSTEM_WIN;
                    log.info("{} 体验房概率达到, 系统赢", getLotteryTextInfo(table));
                } else {
                    lotteryType = LotteryConfig.LotteryType.SYSTEM_LOSE;
                    log.info("{} 体验房概率达到, 系统输", getLotteryTextInfo(table));
                }
                break;
            }
            if (this.roomControl) {
                if (judgeRoomProb()) {
                    lotteryType = LotteryConfig.LotteryType.SYSTEM_WIN;
                    if (!table.isBetPlayerNotJoining()) {
                        log.info("{} 房间概率触发成功, 系统赢", getLotteryTextInfo(table));
                    }
                } else {
                    lotteryType = LotteryConfig.LotteryType.SYSTEM_LOSE;
                    if (!table.isBetPlayerNotJoining()) {
                        log.info("{} 房间概率触发成功, 系统输", getLotteryTextInfo(table));
                    }
                }
                break;
            }
        } while (false);
        return lotteryType;
    }

    /**
     * 开奖
     * 
     * @tabel: 要开奖的桌子
     * 
     * @return: 开奖结果
     */
    public LotteryModel lottery(Table table) {
        return lottery(table, true);
    }

    /**
     * 开奖
     * 
     * @tabel: 要开奖的桌子
     * @isUpdateStorage 是否更新库存
     * @return: 开奖结果
     */
    public LotteryModel lottery(Table table, boolean isUpdateStorage) {
        if (table == null) {
            log.error("lottery 传入的table 为 null");
            return null;
        }
        if (!table.isBetPlayerNotJoining()) {
            log.info("===================== {} 开始走开奖逻辑 =====================", getLotteryTextInfo(table));
        }
        ArrayList<LotteryModel> lotteryModelList = table.getAllLotteryModel(table.getSystemControlPlayerList());
        if (!table.isBetPlayerNotJoining()) {
            analyzeLotteryModelList(lotteryModelList, table);
        }
        LotteryConfig.LotteryType lotteryType = judgeLotteryType(lotteryModelList, table);
        // 打乱下顺序,防止每次下层传过来都是同样的顺序
        Collections.shuffle(lotteryModelList);
        ArrayList<LotteryModel> filteredModel = filterLotteryModel(lotteryModelList, lotteryType,
                table.getSystemControlPlayer());
        ArrayList<LotteryModel> excludeLotteryModelList = new ArrayList<LotteryModel>();
        int lotteryMaxCount = filteredModel.size();
        LotteryModel openLottery = null;
        int lotteryCount = 1;
        if (!table.isBetPlayerNotJoining()) {
            log.info(" {} 库存当前值:{} , 库存起始值:{},当局开奖类型:{}", getLotteryTextInfo(table),
                    this.getStorageCurrent() / Config.MONEY_RATIO_FLOAT,
                    this.storageConfig.getStorageStart() / Config.MONEY_RATIO_FLOAT, lotteryType.toString());
        }
        do {
            openLottery = randomLotteryModel(filteredModel, excludeLotteryModelList);
            // 单控不考虑库存,这里有很高的风险性(目前商量的结果是让厅主自己承担权衡单控的风险)
            if (isPlayerControlMode(lotteryType)) {
                break;
            }
            if (openLottery == null || lotteryCount > lotteryMaxCount) {
                if (openLottery == null) {
                    log.warn("{} 开奖类型 {}, 开不出奖 ,原因:没有一个结果满足开奖类型", getLotteryTextInfo(table), lotteryType.toString());
                } else {
                    log.warn("{} 开奖类型 {}, 开不出奖 ,原因:所有开奖结果库存都不够赔", getLotteryTextInfo(table), lotteryType.toString());
                }
                boolean bSystemLose = lotteryType == LotteryType.SYSTEM_LOSE ? true : false;
                openLottery = doSystemMustLottery(table, lotteryModelList, bSystemLose);
                break;
            }
            // 体验房不走库存
            if (table.getRoom().isFreeRoom()) {
                break;
            }
            if (isStorageEnough(openLottery.systemWin)) {
                break;
            }
            lotteryCount++;
        } while (true);
        // 非体验房才有库存和单控
        if (!table.getRoom().isFreeRoom() && isUpdateStorage) {
            updateControlSystemWin(table, openLottery.systemWin);
            updateControlPlayerWins(table, openLottery.controlPlayerWins);
        }
        if (!table.isBetPlayerNotJoining()) {
            log.info("===================== {} 开奖逻辑结束 =====================", getLotteryTextInfo(table));
        }
        return openLottery;
    }

    /**
     * 更新库存 (炸金花,21点等一开始无法预知结果的游戏,需要游戏结束手动调此接口)
     * 
     * @param table
     * @param systemWin:系统输赢
     */
    public void updateControlSystemWin(Table table, long systemWin) {
        // 开始衰减库存
        changeStorage(table, systemWin);
    }

    /**
     * 更新单控 (炸金花,21点等一开始无法预知结果的游戏,需要游戏结束手动调此接口)
     * 
     * @param table
     * @param controlPlayerWins:单控玩家输赢(单控玩家列表通过此接口 table.getSystemControlPlayerList()获取)
     */
    public void updateControlPlayerWins(Table table, HashMap<Player, Long> controlPlayerWins) {
        // 更新剩余单控金额
        for (Map.Entry<Player, Long> cantrolPlayerMap : controlPlayerWins.entrySet()) {
            Player player = cantrolPlayerMap.getKey();
            // 被单控的玩家才更新,防止下层传入不对
            if (player.isSystemControl()) {
                long winMoney = cantrolPlayerMap.getValue();
                player.addLefeControlMoney(winMoney);
                log.info("{} 单控玩家数据更新, {}", getLotteryTextInfo(table), player.getPlayerControlInfo());
            } else {
                log.error("{} 非法数据,updateControlData 传入的{} 不是单控玩家", getLotteryTextInfo(table),
                        player.getPlayerControlInfo());
            }
        }
    }

    /**
     * 分析下层传过来的开奖结果信息
     */
    public void analyzeLotteryModelList(ArrayList<LotteryModel> lotteryModelList, Table table) {
        StringBuilder lotteryInfoBuilder = new StringBuilder();
        lotteryInfoBuilder.append(getLotteryTextInfo(table));
        lotteryInfoBuilder.append(" 当前下层传过来的开奖模型:\n");
        int index = 0;
        boolean isAllSystemWin = true;
        boolean isAllSystemLose = true;
        for (LotteryModel lotteryModel : lotteryModelList) {
            if (lotteryModel.systemWin >= 0) {
                isAllSystemLose = false;
            }
            if (lotteryModel.systemWin <= 0) {
                isAllSystemWin = false;
            }
            lotteryInfoBuilder.append("[");
            lotteryInfoBuilder.append("系统输赢:");
            lotteryInfoBuilder.append(lotteryModel.systemWin / Config.MONEY_RATIO_FLOAT);
            lotteryInfoBuilder.append(",单控玩家:");
            for (Map.Entry<Player, Long> cantrolMap : lotteryModel.controlPlayerWins.entrySet()) {
                lotteryInfoBuilder.append("{");
                lotteryInfoBuilder.append(cantrolMap.getKey().getPlayerInfo());
                lotteryInfoBuilder.append(",");
                lotteryInfoBuilder.append("输赢:");
                lotteryInfoBuilder.append(cantrolMap.getValue() / Config.MONEY_RATIO_FLOAT);
                lotteryInfoBuilder.append("}");
            }
            lotteryInfoBuilder.append("]  ");
            if (index % 5 == 4) {
                lotteryInfoBuilder.append("\n");
            }
            index++;
        }
        if (room.getCurPlayer() > 0) {
            log.info(lotteryInfoBuilder.toString());
            if (isAllSystemWin) {
                log.warn("警告,下层数据传过来的开奖模型系统全为赢:{}", lotteryInfoBuilder.toString());
            }
            if (isAllSystemLose) {
                log.warn("警告,下层数据传过来的开奖模型系统全为输");
            }
        }
    }

    /**
     * 随机开奖结果
     * 
     * @param lotteryModelList 开奖列表 param excludeLotteryModelList 不包含的开奖列表(已经尝试开过奖了)
     * @return 开奖结果
     */
    private LotteryModel randomLotteryModel(ArrayList<LotteryModel> lotteryModelList,
            ArrayList<LotteryModel> excludeLotteryModelList) {
        ArrayList<LotteryModel> includeLotteryModelList = new ArrayList<LotteryModel>();
        for (LotteryModel lotteryModel : lotteryModelList) {
            if (!excludeLotteryModelList.contains(lotteryModel)) {
                includeLotteryModelList.add(lotteryModel);
            }
        }
        LotteryModel openLottery = randomLotteryModel(includeLotteryModelList);
        excludeLotteryModelList.add(openLottery);
        return openLottery;
    }

    /**
     * 随机开奖结果
     * 
     * @param lotteryModelList 开奖列表
     * @return 开奖结果
     */
    private LotteryModel randomLotteryModel(ArrayList<LotteryModel> lotteryModelList) {
        int lotteryAllWeight = 0;
        for (LotteryModel lotteryModel : lotteryModelList) {
            lotteryAllWeight += lotteryModel.lotteryWeight;
        }
        lotteryAllWeight = lotteryAllWeight > 1 ? lotteryAllWeight : 1;
        int randomWeight = RandomUtil.ramdom(1, lotteryAllWeight);
        int weight = 0;
        LotteryModel openLottery = null;
        for (LotteryModel lotteryModel : lotteryModelList) {
            weight += lotteryModel.lotteryWeight;
            if (randomWeight <= weight) {
                openLottery = lotteryModel;
                break;
            }
        }
        return openLottery;
    }

    /**
     * 
     * 获取最小赢钱结果
     */
    private LotteryModel getSystemMinWinResult(ArrayList<LotteryModel> lotteryModelList) {
        LotteryModel minWinModel = null;
        long minWin = Long.MAX_VALUE;
        for (LotteryModel lotteryModel : lotteryModelList) {
            if (lotteryModel.systemWin >= 0) {
                if (minWin > lotteryModel.systemWin) {
                    minWinModel = lotteryModel;
                    minWin = lotteryModel.systemWin;
                }
            }
        }
        // 都是输钱,那就输最小的
        if (minWinModel == null) {
            minWinModel = getSystemMinLoseResult(lotteryModelList);
        }
        return minWinModel;
    }

    /**
     * 
     * 获取输钱最少的结果
     */
    private LotteryModel getSystemMinLoseResult(ArrayList<LotteryModel> lotteryModelList) {
        LotteryModel minLoseModel = null;
        long minWin = Long.MIN_VALUE;
        for (LotteryModel lotteryModel : lotteryModelList) {
            if (lotteryModel.systemWin <= 0) {
                if (minWin < lotteryModel.systemWin) {
                    minLoseModel = lotteryModel;
                    minWin = lotteryModel.systemWin;
                }
            }
        }
        // 都是赢钱,那就赢最小的
        if (minLoseModel == null) {
            minLoseModel = getSystemMinWinResult(lotteryModelList);
        }
        return minLoseModel;
    }

    /**
     * 
     * 系统强制开奖: 开不出奖的时候,强制开奖
     */
    private LotteryModel doSystemMustLottery(Table table, ArrayList<LotteryModel> lotteryModelList,
            boolean bSystemLose) {
        if (bSystemLose) {
            log.warn("{} 该局开奖类型为系统输,但是开不出奖,强制开奖让系统赢", getLotteryTextInfo(table));
            return getSystemMinWinResult(lotteryModelList);
        } else {
            log.warn("{} 该局开奖类型为系统赢,但是开不出奖,强制开奖让系统输", getLotteryTextInfo(table));
            return getSystemMinLoseResult(lotteryModelList);
        }
    }

    /**
     * 
     * 过滤开奖数据:
     * 
     * @lotteryModelList: 所有开奖集合
     * @lotteryType: 开奖类型
     */
    private ArrayList<LotteryModel> filterLotteryModel(ArrayList<LotteryModel> lotteryModelList,
            LotteryType lotteryType, Player controllPlayer) {
        boolean filter = lotteryType != LotteryType.NO_CONTROL;
        ArrayList<LotteryModel> result = new ArrayList<LotteryModel>();
        for (LotteryModel lotteryModel : lotteryModelList) {
            if (!filter) {
                result.add(lotteryModel);
            } else {
                if (lotteryType == LotteryType.SYSTEM_WIN || lotteryType == LotteryType.SYSTEM_LOSE) {
                    boolean bSystemWin = lotteryType == LotteryType.SYSTEM_WIN;
                    if (bSystemWin && lotteryModel.systemWin >= 0) {
                        result.add(lotteryModel);
                    } else if (!bSystemWin && lotteryModel.systemWin <= 0) {
                        result.add(lotteryModel);
                    }
                } else if (lotteryType == LotteryType.SINGLE_LOSE || lotteryType == LotteryType.SINGLE_WIN) {
                    boolean bPlayerWin = lotteryType == LotteryType.SINGLE_WIN;
                    Long playerWin = 0l;
                    for (Map.Entry<Player, Long> cantrolMap : lotteryModel.controlPlayerWins.entrySet()) {
                        if (cantrolMap.getKey().uniqueId == controllPlayer.uniqueId) {
                            playerWin = cantrolMap.getValue();
                        }
                    }
                    if (bPlayerWin && playerWin != null && playerWin > 0) {
                        result.add(lotteryModel);
                    } else if (!bPlayerWin && playerWin != null && playerWin < 0) {
                        result.add(lotteryModel);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 是否单控玩家参与游戏
     */

    public boolean isControlPlayerJoinGame(ArrayList<LotteryModel> lotteryModelList, Player controlPlayer) {
        boolean isJoinGame = false;
        for (LotteryModel lotteryModel : lotteryModelList) {
            HashMap<Player, Long> controlPlayerWins = lotteryModel.controlPlayerWins;
            for (Map.Entry<Player, Long> cantrolMap : controlPlayerWins.entrySet()) {
                if (cantrolMap.getKey().uniqueId == controlPlayer.uniqueId) {
                    long winGold = cantrolMap.getValue();
                    if (winGold != 0) {
                        isJoinGame = true;
                        break;
                    }
                }
            }
        }
        return isJoinGame;
    }

    /**
     * 房间库存信息
     */

    public String getLotteryTextInfo(Table table) {
        StringBuilder storageBuilder = new StringBuilder();
        storageBuilder.append(table.getRoom().getRoomTextInfo());
        storageBuilder.append(", gameCode:").append(table.getGameUUID()).append(" ");
        return storageBuilder.toString();
    }

    /**
     * 房间库存信息
     */

    public String getStorageTextInfo() {
        StringBuilder storageBuilder = new StringBuilder();
        storageBuilder.append(room.getRoomTextInfo());
        storageBuilder.append(" 当前库存信息:");
        storageBuilder.append("\n\t\t房间概率:");
        storageBuilder.append(this.storageConfig.getSystemWinProb());
        storageBuilder.append("\n\t\t库存起始值:");
        storageBuilder.append(this.storageConfig.getStorageStart() / Config.MONEY_RATIO_FLOAT);
        storageBuilder.append("\n\t\t库存衰减值:");
        storageBuilder.append(this.storageConfig.getStorageReduce());
        storageBuilder.append("\n\t\t库存档位:");
        int index = 1;
        for (StorageGrade storageGrade : this.storageConfig.getStorageGradeList()) {
            storageBuilder.append("\n\t\t\t档位");
            storageBuilder.append(index);
            storageBuilder.append(": 档位值:");
            storageBuilder.append(storageGrade.getStorageMax() / Config.MONEY_RATIO_FLOAT);
            storageBuilder.append(" 档位概率:");
            storageBuilder.append(storageGrade.getProb());
            index++;
        }
        return storageBuilder.toString();
    }

}