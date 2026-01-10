package kz.chitas.chess.controller;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {

    private final MessageRouter router;

    public ChessWebSocketHandler(MessageRouter router) {
        this.router = router;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            router.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            router.handleMessage(session, message);
        } catch (IOException e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            router.afterConnectionClosed(session);
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }
}