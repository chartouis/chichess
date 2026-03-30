package kz.chitas.chess.service.matchmaking;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.model.matchmaking.ActiveInQueue;
import kz.chitas.chess.model.matchmaking.JoinQueueRequest;
import kz.chitas.chess.model.matchmaking.QueueEntry;
import kz.chitas.chess.model.matchmaking.QueueState;
import kz.chitas.chess.service.logic.ChessService;

@Service
public class MMservice {
    ChessService chessService;
    Random random = new Random();
    private final int RATING_RANGE = Integer.parseInt(System.getenv("RATING_RANGE"));
    // Username, QueueEntry
    private final Map<String, QueueEntry> queues = Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile ActiveInQueue active = new ActiveInQueue();

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
            if (matcher.equals(username))
                continue;
            else if (!gametype.equals(matchqe.getGameType()))
                continue;
            else if (qe.isRated() != matchqe.isRated())
                continue;

            if (qe.isRated()) {
                int uRating = chessService.getRating(username);
                int mRating = chessService.getRating(matcher);
                if (!(mRating >= uRating - RATING_RANGE && mRating <= uRating + RATING_RANGE)) {
                    continue;
                }
            }

            if (random.nextBoolean()) {
                RoomState rm = chessService.createRoom("server", matcher, "", gametype, qe.isRated());
                return chessService.joinRoom(rm.getId(), username);
            } else {
                RoomState rm = chessService.createRoom("server", "", matcher, gametype, qe.isRated());
                return chessService.joinRoom(rm.getId(), username);
            }

            // add preferences check in future
        }
        return null;
    }

    public QueueEntry getQueueEntry(String username) {
        if (queues.containsKey(username))
            return queues.get(username);
        return null;
    }

    @Scheduled(fixedRate = 10000)
    private void countInQueue() {
        ActiveInQueue aiq = new ActiveInQueue();
        synchronized (queues) {
            for (QueueEntry queue : queues.values()) {
                String type = queue.getGameType();
                if (queue.isRated()) {
                    switch (type) {
                        case "blitz" -> aiq.setRated_blitz(aiq.getRated_blitz() + 1);
                        case "bullet" -> aiq.setRated_bullet(aiq.getRated_bullet() + 1);
                        case "classical" -> aiq.setRated_classical(aiq.getRated_classical() + 1);
                        case "rapid" -> aiq.setRated_rapid(aiq.getRated_rapid() + 1);
                    }
                } else {
                    switch (type) {
                        case "blitz" -> aiq.setCasual_blitz(aiq.getCasual_blitz() + 1);
                        case "bullet" -> aiq.setCasual_bullet(aiq.getCasual_bullet() + 1);
                        case "classical" -> aiq.setCasual_classical(aiq.getCasual_classical() + 1);
                        case "rapid" -> aiq.setCasual_rapid(aiq.getCasual_rapid() + 1);
                    }
                }
            }
        }

        active = aiq;
    }

    public static ActiveInQueue getActive() {
        return active;
    }

}
