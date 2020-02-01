// Date: 2019/03/20
// Author: dylan
// Desc: 玩家流程控制器

package frame.game.flow;

import java.util.ArrayList;

public final class RoleFlowMgr {

    // 进大厅
    public static boolean enterHall(HallBehavior hall, RoleBehavior role) {
        if (hall == null || role == null) {
            return false;
        }
        if (role.getHall() != null) {
            return false;
        }
        hall.doEnter(role);
        hall.onEnter(role);
        role.doEnterHall(hall);
        role.onEnterHall();
        return true;
    }

    // 进房间
    public static boolean enterRoom(RoomBehavior room, RoleBehavior role) {
        if (room == null || role == null) {
            return false;
        }
        RoomBehavior preRoom = role.getRoom();
        if (preRoom != null) {
            preRoom.onExit(role);
            preRoom.doExit(role);
        }
        room.doEnter(role);
        room.onEnter(role);
        role.doEnterRoom(room);
        role.onEnterRoom();
        return true;
    }

    // 进桌子
    public static boolean enterTable(TableBehavior table, RoleBehavior role) {
        if (table == null || role == null) {
            return false;
        }
        TableBehavior preTable = role.getTable();
        if (preTable != null) {
            preTable.onExit(role);
            preTable.doExit(role);
        }
        table.doEnter(role);
        table.onEnter(role);
        role.doEnterTable(table);
        role.onEnterTable();
        return true;
    }

    // 退大厅
    public static boolean exitHall(RoleBehavior role) {
        if (role == null) {
            return false;
        }
        HallBehavior hall = role.getHall();
        if (hall == null) {
            return false;
        }

        hall.onExit(role);
        hall.doExit(role);
        role.onExitHall();
        role.doExitHall();
        return true;
    }

    // 退房间
    public static boolean exitRoom(RoleBehavior role) {
        if (role == null) {
            return false;
        }
        RoomBehavior room = role.getRoom();
        if (room == null) {
            return false;
        }

        room.onExit(role);
        room.doExit(role);
        role.onExitRoom();
        role.doExitRoom();
        return true;
    }

    // 退桌子
    public static boolean exitTabel(RoleBehavior role) {
        if (role == null) {
            return false;
        }
        TableBehavior table = role.getTable();
        if (table == null) {
            return false;
        }

        table.onExit(role);
        table.doExit(role);
        role.onExitTable();
        role.doExitTable();
        return true;
    }

    // 匹配玩家
    public static boolean pair(RoomBehavior room, RoleBehavior role) {
        if (room == null || role == null) {
            return false;
        }
        TableBehavior table = room.selectTabel(role);
        if (table == null) {
            return false;
        }
        return enterTable(table, role);
    }

    // 匹配玩家
    public static boolean pair(TableBehavior table, RoleBehavior role) {
        if (table == null || role == null) {
            return false;
        }
        return enterTable(table, role);
    }

    // 匹配玩家
    public static boolean pair(TableBehavior table) {
        RoomBehavior room = table.getRoom();
        if (room == null) {
            return false;
        }
        ArrayList<RoleBehavior> robots = room.genRobots();
        for (RoleBehavior robot : robots) {
            enterTable(table, robot);
        }
        return true;
    }
}