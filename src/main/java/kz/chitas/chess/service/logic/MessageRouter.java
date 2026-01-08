package kz.chitas.chess.service.logic;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kz.chitas.chess.model.logic.MoveRequest;
import kz.chitas.chess.model.logic.MoveResponse;
import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.utils.UriIdExtractor;

@Service
public class MessageRouter {

    private final ChessService chessService;
    private final ObjectMapper objectMapper;

    public MessageRouter(ChessService chessService, ObjectMapper objectMapper) {
        this.chessService = chessService;
        this.objectMapper = objectMapper;
    }

    public void handleMove(WebSocketSession session, JsonNode payload, Map<UUID, Set<WebSocketSession>> rooms)
            throws IOException {
        MoveRequest move = objectMapper.treeToValue(payload, MoveRequest.class);

        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = (String) session.getAttributes().get("username");

        if (move == null || gameId.equals(null) || username.equals(null)) {
            return;
        }

        boolean valid = chessService.doMove(gameId, move.getFrom(), move.getTo(), move.getPromotion(), username);
        checkSendPersistClose(rooms, gameId, valid);
    }

    public void handleResign(WebSocketSession session, Map<UUID, Set<WebSocketSession>> rooms) throws IOException {

        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = (String) session.getAttributes().get("username");
        if (gameId.equals(null) || username.equals(null)) {
            return;
        }

        boolean valid = chessService.resign(gameId, username);
        checkSendPersistClose(rooms, gameId, valid);

    }

    public void handleDraw(WebSocketSession session, Map<UUID, Set<WebSocketSession>> rooms) throws IOException {
        UUID gameId = UriIdExtractor.extractGameId(session);
        String username = (String) session.getAttributes().get("username");
        RoomState state = chessService.getRoomState(gameId);
        if (gameId.equals(null) || username.equals(null) || state == null) {
            return;
        }

        boolean valid = chessService.acceptDraw(gameId, username);

        if (!valid) {
            valid = chessService.offerDraw(gameId, username);
        }
        checkSendPersistClose(rooms, gameId, valid);
    }

    public void sendMoveResponse(Map<UUID, Set<WebSocketSession>> rooms, UUID gameId, boolean valid)
            throws IOException {
        RoomState state = chessService.getRoomState(gameId);
        MoveResponse response = new MoveResponse(valid, state);
        String json = objectMapper.writeValueAsString(response);
        sendMessages(rooms, gameId, json);
    }

    public void sendMessages(Map<UUID, Set<WebSocketSession>> rooms, UUID gameId, String messsage)
            throws IOException {
        Set<WebSocketSession> roomSessions = rooms.getOrDefault(gameId, Set.of());
        TextMessage responseMessage = new TextMessage(messsage);
        for (WebSocketSession s : roomSessions) {
            if (s.isOpen()) {
                s.sendMessage(responseMessage);
                ;
            }
        }
    }

    private void closeSessions(Map<UUID, Set<WebSocketSession>> rooms, UUID gameId) throws IOException {
        Set<WebSocketSession> roomSessions = rooms.getOrDefault(gameId, Collections.emptySet());
        for (WebSocketSession s : roomSessions) {
            if (s.isOpen()) {
                s.close();
            }
        }
        rooms.remove(gameId); // drop references
    }

    private void checkSendPersistClose(Map<UUID, Set<WebSocketSession>> rooms, UUID gameId, boolean valid)
            throws IOException {
        RoomState state = chessService.getRoomState(gameId);
        sendMoveResponse(rooms, gameId, valid);
        if (chessService.checkAndPersist(state)) {
            closeSessions(rooms, gameId);
        }
    }
}
