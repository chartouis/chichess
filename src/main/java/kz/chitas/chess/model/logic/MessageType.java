package kz.chitas.chess.model.logic;

public enum MessageType {
    MOVE, // Perform a move by providing a MoveRequest
    UPDATE, // Get the current RoomState
    RESIGN,
    DRAW,
    QUEUE, // Enter Queue by providing JoinQueueRequest
    QUEUE_STATE, // Get the current QueueEntry
    RECONNECT, // Reconnect to the game after stopping the ws session
    SPECTATE, // Start spectating someone else's game by username
}
