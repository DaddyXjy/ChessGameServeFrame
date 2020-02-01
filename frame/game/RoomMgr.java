package frame.game;

import frame.*;
import frame.socket.common.proto.LobbySiteRoom.BetRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.FishRoomCfg;
import frame.socket.common.proto.LobbySiteRoom.PkRoomCfg;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class RoomMgr {
    private @Getter HashMap<Integer, Room> rooms = new HashMap<>();
    private HashSet<Room> roomsToBeClosed = new HashSet<>();
    private @Setter GameHall hall;

    Room baseCreate(Integer roomID) {
        Room room = new Room();
        room.setHall(hall);
        room.setId(roomID);
        room.init();
        log.info("站点:{} , 创建房间:{} 成功 ", hall.getHallId(), roomID);
        return room;
    }

    Room createRoom(Integer roomID, BetRoomCfg cfg) {
        Room room = baseCreate(roomID);
        room.updateCfg(cfg);
        rooms.put(roomID, room);
        return room;
    }

    Room createRoom(Integer roomID, FishRoomCfg cfg) {
        Room room = baseCreate(roomID);
        room.updateCfg(cfg);
        rooms.put(roomID, room);
        return room;
    }

    Room createRoom(Integer roomID, PkRoomCfg cfg) {
        Room room = baseCreate(roomID);
        room.updateCfg(cfg);
        rooms.put(roomID, room);
        return room;
    }

    public void update() {
        for (Room room : rooms.values()) {
            room.update();
        }
        HashSet<Room> roomClosed = new HashSet<Room>();
        for (Room room : roomsToBeClosed) {
            room.update();
            if (room.canTerminate()) {
                room.doTerminate();
                roomClosed.add(room);
            }
        }
        for (Room room : roomClosed) {
            roomsToBeClosed.remove(room);
        }
    }

    public Room getRoom(Integer roomID) {
        return rooms.get(roomID);
    }

    void closeRoom(int roomId) {
        Room room = rooms.remove(roomId);
        if (room != null) {
            roomsToBeClosed.add(room);
            room.setClosing(true);
            log.info("站点:{} , 正在关闭房间:{} ", hall.getHallId(), roomId);
        } else {
            log.error("站点:{} , 关闭房间:{} 失败: 没有找到该房间", hall.getHallId(), roomId);
        }
    }

    Room forbidRoom(int roomId) {
        Room room = rooms.get(roomId);
        if (room != null && !room.isForbid()) {
            room.forbid(2);
            log.info("站点:{} , 已禁用房间:{} ", hall.getHallId(), roomId);
        } else {
            log.error("站点:{} , 禁用房间:{} 失败: 没有找到该房间", hall.getHallId(), roomId);
        }
        return room;
    }

    Room reopenRoom(int roomId) {
        Room room = rooms.get(roomId);
        if (room != null && room.isForbid()) {
            room.forbid(1);
            log.info("站点:{} , 已开启被禁用的房间:{} ", hall.getHallId(), roomId);
        } else {
            log.error("站点:{} , 开启被禁用的房间:{} 失败: 没有找到该房间", hall.getHallId(), roomId);
        }
        return room;
    }

    void removeRoom(Room room) {
        if (rooms.get(room.id) != null) {
            closeRoom(room.id);
        } else if (roomsToBeClosed.contains(room)) {
            room.doDestroy();
            roomsToBeClosed.remove(room);
        }
    }

    boolean canTerminate() {
        for (Room room : rooms.values()) {
            if (!room.canTerminate()) {
                return false;
            }
        }

        for (Room room : roomsToBeClosed) {
            if (!room.canTerminate()) {
                return false;
            }
        }

        return true;
    }

    void doStop() {
        for (Room room : rooms.values()) {
            room.doStop();
        }

        for (Room room : roomsToBeClosed) {
            room.doStop();
        }
    }

    void doTerminate() {
        for (Room room : rooms.values()) {
            room.doTerminate();
        }
        for (Room room : roomsToBeClosed) {
            room.doTerminate();
        }
    }

    void doDestroy() {
        for (Room room : rooms.values()) {
            room.doDestroy();
        }
        for (Room room : roomsToBeClosed) {
            room.doDestroy();
        }
    }

}