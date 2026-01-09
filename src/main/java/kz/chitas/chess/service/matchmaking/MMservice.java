package kz.chitas.chess.service.matchmaking;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.model.matchmaking.JoinQueueRequest;
import kz.chitas.chess.model.matchmaking.QueueEntry;
import kz.chitas.chess.model.matchmaking.QueueState;
import kz.chitas.chess.service.logic.ChessService;
import kz.chitas.chess.utils.RoomFullException;

@Service
public class MMservice {
    ChessService chessService;
    Random random = new Random();
    LinkedHashMap<String, QueueEntry> queues = new LinkedHashMap<>();

    public MMservice(ChessService chessService) {
        this.chessService = chessService;
    }

    public QueueEntry leaveQueue(String username) {
        if (queues.containsKey(username)) {
            QueueEntry entry = queues.get(username);
            queues.remove(username);
            return entry;
        }
        return null;
    }

    public QueueEntry enterQueue(JoinQueueRequest req, String username) {
        long start = Instant.now().toEpochMilli();
        UUID uuid = UUID.randomUUID();
        QueueEntry entry = QueueEntry.builder()
                .id(uuid)
                .username(username)
                .gameType(req.getGameType())
                .preferences(req.getPreferences())
                .maxRating(req.getMaxRating())
                .minRating(req.getMinRating())
                .rated(req.isRated())
                .status(QueueState.AWAITING)
                .queueStart(start)
                .build();
        queues.put(username, entry);
        return entry;
    }

    public RoomState match(String username) {
        QueueEntry qe = queues.get(username);
        String gametype = qe.getGameType();
        for (String matcher : queues.keySet()) {
            QueueEntry matchqe = queues.get(matcher);
            if (!gametype.equals(matchqe.getGameType()))
                continue;
            else if (qe.isRated() != matchqe.isRated())
                continue;

            if (random.nextBoolean())
                return chessService.createRoom("server", matcher, username, gametype);
            else
                return chessService.createRoom("server", username, matcher, gametype);

            // add preferences check in future
        }
        return null;
    }

    public QueueEntry getQueueEntry(String username) {
        if (queues.containsKey(username))
            return queues.get(username);
        return null;
    }

}
