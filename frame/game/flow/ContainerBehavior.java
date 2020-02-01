// Date: 2019/03/20
// Author: dylan
// Desc: 容器行为接口

package frame.game.flow;

public interface ContainerBehavior {
    // 执行进容器
    public void doEnter(RoleBehavior role);

    // 响应进容器
    public void onEnter(RoleBehavior role);

    // 执行退容器
    public void doExit(RoleBehavior role);

    // 响应退容器
    public void onExit(RoleBehavior role);
}