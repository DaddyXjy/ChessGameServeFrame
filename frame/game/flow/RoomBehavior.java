// Date: 2019/03/20
// Author: dylan
// Desc: 房间行为接口

package frame.game.flow;

import java.util.ArrayList;

public interface RoomBehavior extends ContainerBehavior {
    // 为玩家选桌
    public TableBehavior selectTabel(RoleBehavior role);

    // 获取大厅
    public HallBehavior getHall();

    // 生成一堆机器人
    public ArrayList<RoleBehavior> genRobots();
}