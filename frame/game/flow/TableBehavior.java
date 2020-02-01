// Date: 2019/03/20
// Author: dylan
// Desc: 桌子行为接口

package frame.game.flow;

public interface TableBehavior extends ContainerBehavior {
    // 获取大厅
    public RoomBehavior getRoom();
}