package kz.chitas.chess.utils;

import kz.chitas.chess.model.logic.RoomState;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(RoomState room) {
        super("Room not found: " + room);
    }
}
