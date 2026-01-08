package kz.chitas.chess.model.matchmaking;

import lombok.Data;

@Data
public class JoinQueueRequest {
    private boolean rated;
    private int minRating;
    private int maxRating;
    private String gameType;
    private String[] preferences;
}
