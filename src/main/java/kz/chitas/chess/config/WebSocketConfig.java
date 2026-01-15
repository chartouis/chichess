package kz.chitas.chess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import kz.chitas.chess.controller.ChessWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChessWebSocketHandler chessWebSocketHandler;
    private final CHandshakeInterceptor interceptor;
    private final String FRONTEND_URL = System.getenv("FRONTEND_URL");

    public WebSocketConfig(ChessWebSocketHandler chessWebSocketHandler, CHandshakeInterceptor interceptor) {
        this.chessWebSocketHandler = chessWebSocketHandler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(chessWebSocketHandler, "/api/ws/game")
                .addInterceptors(interceptor)
                .setAllowedOrigins(FRONTEND_URL);
    }
}
