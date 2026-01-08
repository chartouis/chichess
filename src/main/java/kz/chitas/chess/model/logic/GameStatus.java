package kz.chitas.chess.model.logic;

public enum GameStatus {
    WAITING, // lobby created
    ONGOING, // game in progress
    CHECKMATE,
    STALEMATE,
    DRAW,
    RESIGNED,
    TIMEOUT,
    ABANDONED
}
