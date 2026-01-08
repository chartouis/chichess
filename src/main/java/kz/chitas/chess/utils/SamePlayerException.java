package kz.chitas.chess.utils;

import kz.chitas.chess.model.logic.RoomState;

public class SamePlayerException extends RuntimeException {
    public SamePlayerException(RoomState room, String player) {
        super("Room has the same player: " + player + " as white and black: \n" + room);
    }
}
