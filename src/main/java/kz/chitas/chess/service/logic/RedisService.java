package kz.chitas.chess.service.logic;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import kz.chitas.chess.model.logic.GameStatus;
import kz.chitas.chess.model.logic.RoomState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
@Log4j2
public class RedisService {

    private final JedisPool jedisPool;

    public RedisService() {
        log.info("Starting Redis-Service");
        this.jedisPool = new JedisPool("localhost", 6379);
    }

    public void saveRoomState(RoomState roomState) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "room:" + roomState.getId().toString();
            jedis.hset(key, "creator", nvl(roomState.getCreator()));
            jedis.hset(key, "white", nvl(roomState.getWhite()));
            jedis.hset(key, "black", nvl(roomState.getBlack()));
            jedis.hset(key, "position", nvl(roomState.getPosition()));
            jedis.hset(key, "history", nvl(roomState.getHistory()));
            jedis.hset(key, "status", roomState.getStatus() != null ? roomState.getStatus().name() : "");
            jedis.hset(key, "winner", nvl(roomState.getWinner()));
            jedis.hset(key, "drawOfferedBy", nvl(roomState.getDrawOfferedBy()));
            jedis.hset(key, "gameType", nvl(roomState.getGameType()));

            // Timer fields
            jedis.hset(key, "remainingWhite", String.valueOf(roomState.getRemainingWhite()));
            jedis.hset(key, "remainingBlack", String.valueOf(roomState.getRemainingBlack()));
            jedis.hset(key, "lastMoveEpoch", String.valueOf(roomState.getLastMoveEpoch()));

            // NEW FIELD — timestamps
            jedis.hset(key, "timestamps", nvl(roomState.getTimestamps()));

            // NEW FIELD — game start time
            jedis.hset(key, "gameStartedAt",
                    roomState.getGameStartedAt() != null ? roomState.getGameStartedAt().toString() : "");

            // Creation time (UTC)
            jedis.hset(key, "createdAt",
                    roomState.getCreatedAt() != null ? roomState.getCreatedAt().toString() : Instant.now().toString());

            // ---- PLAYER INDEXING ----
            if (roomState.getWhite() != null && !roomState.getWhite().isEmpty()) {
                jedis.sadd("user:" + roomState.getWhite() + ":rooms", roomState.getId().toString());
            }
            if (roomState.getBlack() != null && !roomState.getBlack().isEmpty()) {
                jedis.sadd("user:" + roomState.getBlack() + ":rooms", roomState.getId().toString());
            }
        }
    }

    public RoomState getRoomState(UUID roomId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "room:" + roomId;
            if (!jedis.exists(key)) {
                return null;
            }

            String creator = jedis.hget(key, "creator");
            String white = jedis.hget(key, "white");
            String black = jedis.hget(key, "black");
            String position = jedis.hget(key, "position");
            String history = jedis.hget(key, "history");
            String status = jedis.hget(key, "status");
            String winner = jedis.hget(key, "winner");
            String drawOfferedBy = jedis.hget(key, "drawOfferedBy");
            String gameType = jedis.hget(key, "gameType");
            String remainingWhite = jedis.hget(key, "remainingWhite");
            String remainingBlack = jedis.hget(key, "remainingBlack");
            String lastMoveEpoch = jedis.hget(key, "lastMoveEpoch");
            String timestamps = jedis.hget(key, "timestamps");
            String createdAt = jedis.hget(key, "createdAt");
            String gameStartedAt = jedis.hget(key, "gameStartedAt");

            return new RoomState.Builder()
                    .id(roomId)
                    .creator(creator)
                    .black(black)
                    .white(white)
                    .position(position)
                    .history(history)
                    .status(status != null && !status.isEmpty() ? GameStatus.valueOf(status) : GameStatus.WAITING)
                    .winner(winner)
                    .drawOfferedBy(drawOfferedBy)
                    .gameType(gameType)
                    .remainingWhite(parseLong(remainingWhite))
                    .remainingBlack(parseLong(remainingBlack))
                    .lastMoveEpoch(parseLong(lastMoveEpoch))
                    .timestamps(timestamps)
                    .createdAt(createdAt != null && !createdAt.isEmpty() ? Instant.parse(createdAt) : Instant.now())
                    .gameStartedAt(
                            gameStartedAt != null && !gameStartedAt.isEmpty() ? Instant.parse(gameStartedAt) : null)
                    .build();
        }
    }

    public List<RoomState> getAllExistingRooms() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("room:*");
            if (keys.isEmpty())
                return Collections.emptyList();

            List<RoomState> roomList = new ArrayList<>();
            for (String roomId : keys) {
                RoomState state = getRoomState(UUID.fromString(roomId.split(":")[1]));
                if (state != null)
                    roomList.add(state);
            }
            return roomList;
        }
    }

    public void deleteRoom(String roomId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "room:" + roomId;

            String white = jedis.hget(key, "white");
            String black = jedis.hget(key, "black");

            if (white != null && !white.isEmpty()) {
                jedis.srem("user:" + white + ":rooms", roomId);
            }
            if (black != null && !black.isEmpty()) {
                jedis.srem("user:" + black + ":rooms", roomId);
            }

            jedis.del(key);
        }
    }

    public void deleteRoom(UUID roomId) {
        deleteRoom(roomId.toString());
    }

    public void deleteAllRooms() {
        try (Jedis jedis = jedisPool.getResource()) {
            var keys = jedis.keys("room:*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }
        }
    }

    public boolean hasRoomId(String roomId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("room:" + roomId);
        }
    }

    public boolean hasRoomId(UUID roomId) {
        return hasRoomId(roomId.toString());
    }

    public Set<String> getRoomsByUsername(String username) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.smembers("user:" + username + ":rooms");
        }
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private long parseLong(String value) {
        if (value == null || value.isEmpty())
            return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
