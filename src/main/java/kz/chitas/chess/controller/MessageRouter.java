package kz.chitas.chess.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kz.chitas.chess.model.logic.MessageType;
import kz.chitas.chess.model.logic.MoveRequest;
import kz.chitas.chess.model.logic.MoveResponse;
import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.model.logic.SessionState;
import kz.chitas.chess.model.matchmaking.JoinQueueRequest;
import kz.chitas.chess.model.matchmaking.QueueEntry;
import kz.chitas.chess.service.logic.ChessService;
import kz.chitas.chess.service.matchmaking.MMservice;
import kz.chitas.chess.utils.UriIdExtractor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class MessageRouter {

    private final ChessService chessService;
    private final ObjectMapper objectMapper;
    private final MMservice matchmaker;

    private HashMap<String, WebSocketSession> sessions = new HashMap<>();

    // for games, after the game is found
    private final Map<UUID, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public MessageRouter(ChessService chessService, ObjectMapper objectMapper, MMservice matchmaker) {
        this.chessService = chessService;
        this.objectMapper = objectMapper;
        this.matchmaker = matchmaker;
    }

    public void handleMessage(WebSocketSession session, TextMessage message) throws IOException {
        String username = (String) session.getAttributes().get("username");

        log.info("User: {} | Message received", username);

        JsonNode node = objectMapper.readTree(message.getPayload());
        String rawType = node.has("type") ? node.get("type").asText() : null;
        JsonNode payload = node.has("payload") ? node.get("payload") : null;

        if (rawType == null) {
            log.warn("Message missing 'type' field");
            return;
        }

        try {
            MessageType type = MessageType.valueOf(rawType.toUpperCase());
            if (!allowActionOnState(type, getState(session))) {
                log.info("Impossible action by : {}", username);
                return;
            }
            switch (type) {
                case MOVE:
                    handleMove(session, payload);
                    break;
                case RESIGN:
                    handleResign(session);
                    break;
                case DRAW:
                    handleDraw(session);
                    break;
                case UPDATE:
                    handleUpdate(session);
                    break;
                case QUEUE:
                    handleQueue(session, payload);
                    break;
                case QUEUE_STATE:
                    handleQueueState(session);
                    break;
                case RECONNECT:
                    handleReconnect(session);
                    break;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown message type: {}", rawType);
        }
    }

    private void handleReconnect(WebSocketSession session) throws IOException {
        String username = UriIdExtractor.getUsername(session);
        if (username == null) {
            log.warn("Invalid queue request: missing required data");
            return;
        }

        Set<String> roomids = chessService.getRoomsByUsername(username);
        if (roomids.isEmpty()) {
            log.info("Reconnect FAILED by : {} no active games");
            return;
        }
        UUID roomId = UUID.fromString(roomids.iterator().next());
        WebSocketSession ws = sessions.get(username);
        ws.getAttributes().put("roomid", roomId);
        setState(ws, SessionState.INGAME);
        addSession(roomId, ws);
        handleUpdate(session);
        log.info("Reconnect SUCCESSFUL by {}", username);
    }

    private void handleQueue(WebSocketSession session, JsonNode payload) throws IOException {
        String username = UriIdExtractor.getUsername(session);
        if (username == null || payload == null) {
            log.warn("Invalid queue request: missing required data");
            return;
        }

        if (isPlaying(username)) {
            log.info("Only one active game per user : {}", username);
            return;
        }
        JoinQueueRequest jqr = objectMapper.treeToValue(payload, JoinQueueRequest.class);

        if (jqr.getGameType() == null || jqr.getPreferences() == null) {
            log.warn("Invalid queue request: missing required data");
            return;
        }
        QueueEntry qe = matchmaker.enterQueue(jqr, username);
        setState(session, SessionState.QUEUE);
        log.info("User : {} in QUEUE", username);
        sendObject(username, qe);
        RoomState roomState = matchmaker.match(username);
        if (roomState != null) {
            UUID roomId = roomState.getId();

            for (String player : List.of(roomState.getWhite(), roomState.getBlack())) {
                WebSocketSession ws = sessions.get(player);
                ws.getAttributes().put("roomid", roomId);
                setState(ws, SessionState.INGAME);
                addSession(roomId, ws);
                handleUpdate(ws);
            }

        }
    }

    private void handleMove(WebSocketSession session, JsonNode payload) throws IOException {
        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = UriIdExtractor.getUsername(session);

        if (gameId == null || username == null || payload == null) {
            log.warn("Invalid move request: missing required data");
            return;
        }

        MoveRequest move = objectMapper.treeToValue(payload, MoveRequest.class);

        if (move == null || move.getFrom() == null || move.getTo() == null) {
            log.warn("Invalid move: missing from/to fields");
            return;
        }

        boolean valid = chessService.doMove(gameId, move.getFrom(), move.getTo(), move.getPromotion(), username);
        sendResponseAndCheckEnd(gameId, valid);
    }

    private void handleResign(WebSocketSession session) throws IOException {
        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = (String) session.getAttributes().get("username");

        if (gameId == null || username == null) {
            log.warn("Invalid resign request: missing required data");
            return;
        }

        boolean valid = chessService.resign(gameId, username);
        sendResponseAndCheckEnd(gameId, valid);
    }

    private void handleDraw(WebSocketSession session) throws IOException {
        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = (String) session.getAttributes().get("username");
        RoomState state = chessService.getRoomState(gameId);

        if (gameId == null || username == null || state == null) {
            log.warn("Invalid draw request: missing required data");
            return;
        }

        // Try to accept draw first, if that fails, offer a draw
        boolean valid = chessService.acceptDraw(gameId, username);
        if (!valid) {
            valid = chessService.offerDraw(gameId, username);
        }

        sendResponseAndCheckEnd(gameId, valid);
    }

    private void handleQueueState(WebSocketSession session) throws IOException {
        String username = UriIdExtractor.getUsername(session);
        if (username == null) {
            log.warn("Invalid draw request: missing required data");
            return;
        }
        QueueEntry qe = matchmaker.getQueueEntry(username);
        String json = objectMapper.writeValueAsString(qe);
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }

    private void handleUpdate(WebSocketSession session) throws IOException {
        UUID gameId = UriIdExtractor.extractGameId(session);

        if (gameId == null) {
            log.warn("Invalid update request: missing gameId");
            return;
        }

        RoomState state = chessService.getRoomState(gameId);
        MoveResponse response = new MoveResponse(true, state);
        String json = objectMapper.writeValueAsString(response);

        // Send only to requesting session
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }

    public void addSession(UUID gameId, WebSocketSession session) {
        rooms.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(UUID gameId, WebSocketSession session) {
        Set<WebSocketSession> roomSessions = rooms.get(gameId);
        if (roomSessions != null) {
            roomSessions.remove(session);
            if (roomSessions.isEmpty()) {
                rooms.remove(gameId);
            }
        }
    }

    public void sendCurrentState(UUID gameId) throws IOException {
        RoomState state = chessService.getRoomState(gameId);
        MoveResponse response = new MoveResponse(true, state);
        String json = objectMapper.writeValueAsString(response);
        broadcast(gameId, json);
    }

    private void sendResponseAndCheckEnd(UUID gameId, boolean valid) throws IOException {
        RoomState state = chessService.getRoomState(gameId);
        MoveResponse response = new MoveResponse(valid, state);
        String json = objectMapper.writeValueAsString(response);

        broadcast(gameId, json);

        // Close room if game is finished
        if (chessService.checkAndPersist(state)) {
            Set<WebSocketSession> set = rooms.get(state.getId());
            for (WebSocketSession wss : set) {
                setState(wss, SessionState.CONNECTED);
            }
            closeRoom(gameId);
        }
    }

    private void broadcast(UUID gameId, String message) throws IOException {
        Set<WebSocketSession> roomSessions = rooms.getOrDefault(gameId, Collections.emptySet());
        TextMessage textMessage = new TextMessage(message);

        for (WebSocketSession session : roomSessions) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    private void closeRoom(UUID gameId) throws IOException {
        Set<WebSocketSession> roomSessions = rooms.getOrDefault(gameId, Collections.emptySet());

        for (WebSocketSession session : roomSessions) {
            if (session.isOpen()) {
                session.close();
            }
        }

        rooms.remove(gameId);
        log.info("Room {} closed", gameId);
    }

    // Theoretically this allows only one websocket per user
    public void put(WebSocketSession wss) throws IOException {
        String username = (String) wss.getAttributes().get("username");
        if (!sessions.containsKey(username)) {
            sessions.put(username, wss);
            log.info("Put into sessions : {}", username);
            return;
        } else {
            wss.close();
            log.info("Only one connection per user : {}", username);
        }
    }

    public void sendObject(String username, Object obj) throws IOException {
        String json = objectMapper.writeValueAsString(obj);
        sendMessage(username, json);
    }

    public void sendMessage(String username, String message) throws IOException {
        if (sessions.containsKey(username)) {
            WebSocketSession ses = sessions.get(username);
            TextMessage tm = new TextMessage(message);
            if (ses.isOpen())
                ses.sendMessage(tm);
        }
    }

    public void afterConnectionClosed(WebSocketSession session) throws IOException {
        String username = UriIdExtractor.getUsername(session);
        if (sessions.containsKey(username)) {
            // session.getAttributes().put("state", SessionState.DISCONNECT);
            QueueEntry qe = matchmaker.leaveQueue(username);
            sendObject(username, qe);
            sessions.remove(username);
            log.info("Connection closed : {}", username);

            if (isIngame(session)) {
                UUID roomid = UriIdExtractor.extractGameId(session);
                Set<WebSocketSession> set = rooms.get(roomid);
                for (WebSocketSession wss : set) {
                    if (UriIdExtractor.getUsername(wss).equals(username)) {
                        set.remove(wss);
                        log.info("Removed : {} from rooms", username);
                    }
                }
            }
        } else {
            log.info("Connection failed to close, no session with such username");
        }
    }

    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String username = UriIdExtractor.getUsername(session);
        setState(session, SessionState.CONNECTED);
        log.info("Connection established for : {}", username);
        put(session);
    }

    private boolean allowActionOnState(MessageType type, SessionState state) {
        switch (type) {
            case MOVE, UPDATE, RESIGN, DRAW:
                if (state == SessionState.INGAME)
                    return true;
                else
                    return false;
            case QUEUE, QUEUE_STATE, RECONNECT:
                if (state == SessionState.CONNECTED)
                    return true;
                else
                    return false;
            default:
                return false;
        }
    }

    private boolean isPlaying(String username) {
        if (chessService.getRoomsByUsername(username).isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private SessionState getState(WebSocketSession session) {
        return (SessionState) session.getAttributes().get("state");
    }

    private void setState(WebSocketSession session, SessionState state) {
        session.getAttributes().put("state", state);
    }

    private boolean isIngame(WebSocketSession session) {
        return getState(session) == SessionState.INGAME;
    }

    // private boolean isConnected(WebSocketSession session) {
    // return getState(session) == SessionState.CONNECTED;
    // }

    // private boolean isQueued(WebSocketSession session) {
    // return getState(session) == SessionState.QUEUE;
    // }

}