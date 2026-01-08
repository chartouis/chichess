package kz.chitas.chess.model.matchmaking;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueEntry {
    private UUID id;
    private String username;
    private int rating;
    private int minRating;
    private int maxRating;
    private boolean rated;
    private long queueStart; // miliseconds
    private Long queueEnd; // nullable
    private String gameType;
    private QueueStatus status;
    private String[] preferences; // List of random bools, prolly
    private long timeoutAt; // What even is timeoutAt? i kinda forgot
}
