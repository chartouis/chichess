package kz.chitas.chess.service.matchmaking;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import kz.chitas.chess.model.matchmaking.JoinQueueRequest;
import kz.chitas.chess.model.matchmaking.QueueEntry;
import kz.chitas.chess.model.matchmaking.QueueStatus;

@Service
public class MMservice {
    HashMap<String, QueueEntry> queues = new HashMap<>();

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
                .status(QueueStatus.AWAITING)
                .queueStart(start)
                .build();
        queues.put(username, entry);
        return entry;
    }

    public QueueEntry getQueueEntry(String username) {
        if (queues.containsKey(username))
            return queues.get(username);
        return null;
    }

}
