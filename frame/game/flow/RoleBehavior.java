// Date: 2019/03/20
// Author: dylan
// Desc: 角色行为接口

package frame.game.flow;

public interface RoleBehavior {

    /** =================== 大厅相关 =================== */

    // 获取大厅
    public HallBehavior getHall();

    // 执行进大厅
    public void doEnterHall(HallBehavior hall);

    // 响应进大厅
    public void onEnterHall();

    // 执行退大厅
    public void doExitHall();

    // 响应退大厅
    public void onExitHall();

    /** =================== 房间相关 =================== */

    // 获取房间
    public RoomBehavior getRoom();

    // 执行进房间
    public void doEnterRoom(RoomBehavior room);

    // 响应进房间
    public void onEnterRoom();

    // 执行退房间
    public void doExitRoom();

    // 响应进房间
    public void onExitRoom();

    /** =================== 桌子相关 =================== */
    // 获取房间
    public TableBehavior getTable();

    // 执行进桌子
    public void doEnterTable(TableBehavior table);

    // 响应进桌子
    public void onEnterTable();

    // 执行退桌子
    public void doExitTable();

    // 响应退桌子
    public void onExitTable();

}