package kz.chitas.chess.utils;

import java.util.Objects;
import java.util.UUID;

import org.springframework.web.socket.WebSocketSession;

public class UriIdExtractor {
    public static UUID extractGameIdFromURI(WebSocketSession session) {
        // Example path: /ws/game/abc123
        String path = Objects.requireNonNull(session.getUri()).getPath();
        return UUID.fromString(path.substring(path.lastIndexOf("/") + 1));
    }

    public static UUID extractGameId(WebSocketSession session) {
        return UUID.fromString((String) session.getAttributes().get("roomid"));
    }

    public static UUID extractGameId(String uri) {
        String path = Objects.requireNonNull(uri);
        return UUID.fromString(path.substring(path.lastIndexOf("/") + 1));
    }

    public static String getUsername(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }
}