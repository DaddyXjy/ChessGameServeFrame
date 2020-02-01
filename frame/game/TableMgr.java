package frame.game;

import frame.log;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * TableMgr
 */
public class TableMgr {
    private @Setter Room room;
    private @Getter HashMap<Integer, Table> allTables = new HashMap<>();

    private int index;

    public Table createTable() {
        Table table = GameMain.getInstance().getGameMgr().createTable();
        table.room = room;
        table.init();
        table.setId(index);
        allTables.put(index++, table);
        log.info("[房间]:{} [桌子]:{} 创建成功,剩余桌子数量:{}", room.getId(), table.getId(), allTables.size());
        // TODO
        return table;
    }

    public void update() {
        for (Table table : allTables.values()) {
            table.update();
        }
        updateRemoveTabels();
    }

    private void updateRemoveTabels() {
        ArrayList<Table> removeTables = new ArrayList<>();
        for (Table table : allTables.values()) {
            if (table.getIsDestroy()) {
                removeTables.add(table);
            }
        }
        for (Table table : removeTables) {
            allTables.remove(table.getId());
            log.info("[房间]:{} [桌子]:{} 已被销毁,剩余桌子数量:{}", room.getId(), table.getId(), allTables.size());
        }
    }

    void updateConfig() {
        for (Table table : allTables.values()) {
            table.setNeedUpdateConfig(true);
            table.onRoomConfigUpdate();
        }
    }

    boolean canTerminate() {
        for (Table table : allTables.values()) {
            if (!table.canTerminate()) {
                return false;
            }
        }

        return true;
    }

    Table getWait(Role role) {
        Table wait = null;
        for (Table table : allTables.values()) {
            if (table.getTablePair().IsWaitingRole(role)) {
                wait = table;
            }
        }
        if (wait == null) {
            wait = createTable();
        }
        return wait;
    }

    void doStop() {
        for (Table table : allTables.values()) {
            table.doStop();
        }
    }

    void doDestroy() {
        for (Table table : allTables.values()) {
            table.doDestroy();
        }
    }

    void doTerminate() {
        for (Table table : allTables.values()) {
            table.doTerminate();
        }
    }
}