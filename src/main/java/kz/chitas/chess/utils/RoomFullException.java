package kz.chitas.chess.utils;

import kz.chitas.chess.model.logic.RoomState;

public class RoomFullException extends RuntimeException {
    public RoomFullException(RoomState room) {
        super("Room is full: " + room);
    }
}