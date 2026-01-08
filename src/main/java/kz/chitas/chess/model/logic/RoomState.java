package kz.chitas.chess.model.logic;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "room_state")
public class RoomState {
    @Id
    private UUID id;
    private String creator;
    private String black;
    private String white;
    private String position; // FEN
    private String history; // SAN moves
    private String timestamps; // string of move timestamps in ms
    private String drawOfferedBy;
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    private String winner;
    private String gameType;

    // Timer fields in milliseconds
    private long remainingWhite;
    private long remainingBlack;
    private long lastMoveEpoch;

    // Creation timestamp in UTC
    private Instant createdAt = Instant.now();

    // NEW FIELD — time when the game actually started
    private Instant gameStartedAt;

    public boolean hasPlayer(String username) {
        return (black.equals(username) || white.equals(username));
    }

    public boolean checkTimerRunout() {
        if (getRemainingWhite() <= 0) {
            setStatus(GameStatus.TIMEOUT);
            setWinner(getBlack());
            return true;
        }
        if (getRemainingBlack() <= 0) {
            setStatus(GameStatus.TIMEOUT);
            setWinner(getWhite());
            return true;
        }
        return false;
    }

    public void updateTimer(long incrementWhite, long incrementBlack) {
        long currentTimeInMS = System.currentTimeMillis();

        if (lastMoveEpoch == 0) {
            setLastMoveEpoch(currentTimeInMS);
        }
        if (whiteToMove()) {
            setRemainingWhite(getRemainingWhite() - (currentTimeInMS - getLastMoveEpoch()) + incrementWhite);
        } else {
            setRemainingBlack(getRemainingBlack() - (currentTimeInMS - getLastMoveEpoch()) + incrementBlack);
        }
        setLastMoveEpoch(currentTimeInMS);
    }

    public boolean isAfkTimeout() {
        long now = System.currentTimeMillis();
        if (isFirstMoveForWhite()) {
            return now - gameStartedAt.toEpochMilli() >= 60000;
        }
        if (isFirstMoveForBlack()) {
            long lastWhiteMove = Long.parseLong(timestamps);
            return now - (lastWhiteMove + gameStartedAt.toEpochMilli()) >= 60000;
        }
        return false;
    }

    public boolean whiteToMove() {
        return getPosition().split(" ")[1].equals("w");
    }

    public boolean blackToMove() {
        return getPosition().split(" ")[1].equals("b");
    }

    public boolean isFirstMoveForWhite() {
        return whiteToMove() && getFullmoveNumber() == 1;
    }

    public boolean isFirstMoveForBlack() {
        return blackToMove() && getFullmoveNumber() == 1;
    }

    public boolean isFirstMoveForBoth() {
        return getFullmoveNumber() == 1;
    }

    private int getFullmoveNumber() {
        return Integer.parseInt(getPosition().split(" ")[5]);
    }

    public int getHalfmoveClock() {
        return Integer.parseInt(getPosition().split(" ")[4]);
    }

    public void addTimestamp() {
        long now = System.currentTimeMillis();
        long sinceStart = now - getGameStartedAt().toEpochMilli();
        String current = getTimestamps();
        setTimestamps(current.isEmpty() ? String.valueOf(sinceStart) : current + " " + sinceStart);
    }

    private RoomState(Builder builder) {
        this.id = builder.id;
        this.creator = builder.creator;
        this.black = builder.black;
        this.white = builder.white;
        this.position = builder.position;
        this.history = builder.history;
        this.timestamps = builder.timestamps;
        this.status = builder.status;
        this.winner = builder.winner;
        this.drawOfferedBy = builder.drawOfferedBy;
        this.gameType = builder.gameType;
        this.remainingWhite = builder.remainingWhite;
        this.remainingBlack = builder.remainingBlack;
        this.lastMoveEpoch = builder.lastMoveEpoch;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.gameStartedAt = builder.gameStartedAt;
    }

    public static class Builder {
        private UUID id;
        private String creator;
        private String black;
        private String white;
        private String position;
        private String history;
        private String timestamps;
        private String drawOfferedBy;
        private GameStatus status;
        private String winner;
        private String gameType;
        private long remainingWhite;
        private long remainingBlack;
        private long lastMoveEpoch;
        private Instant createdAt;
        private Instant gameStartedAt;

        public Builder id(String id) {
            this.id = UUID.fromString(id);
            return this;
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder black(String black) {
            this.black = black;
            return this;
        }

        public Builder white(String white) {
            this.white = white;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public Builder history(String history) {
            this.history = history;
            return this;
        }

        public Builder timestamps(String timestamps) {
            this.timestamps = timestamps;
            return this;
        }

        public Builder status(GameStatus status) {
            this.status = status;
            return this;
        }

        public Builder winner(String winner) {
            this.winner = winner;
            return this;
        }

        public Builder drawOfferedBy(String drawOfferedBy) {
            this.drawOfferedBy = drawOfferedBy;
            return this;
        }

        public Builder gameType(String gameType) {
            this.gameType = gameType;
            return this;
        }

        public Builder remainingWhite(long remainingWhite) {
            this.remainingWhite = remainingWhite;
            return this;
        }

        public Builder remainingBlack(long remainingBlack) {
            this.remainingBlack = remainingBlack;
            return this;
        }

        public Builder lastMoveEpoch(long lastMoveEpoch) {
            this.lastMoveEpoch = lastMoveEpoch;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder gameStartedAt(Instant gameStartedAt) {
            this.gameStartedAt = gameStartedAt;
            return this;
        }

        public RoomState build() {
            return new RoomState(this);
        }
    }
}
