package kz.chitas.chess.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import kz.chitas.chess.service.logic.RedisService;
import kz.chitas.chess.utils.AnnoyingConstants;
import kz.chitas.chess.utils.UriIdExtractor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class CHandshakeInterceptor implements HandshakeInterceptor {

    private final AnnoyingConstants acost;
    private final RedisService redisService;

    public CHandshakeInterceptor(AnnoyingConstants acost, RedisService redisService) {
        this.acost = acost;
        this.redisService = redisService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (acost.getCurrentUsername().equals(null)) {
            log.trace("No username. Which means no auth or jwt");
            return false;
        }
        UUID roomId = UriIdExtractor.extractGameId(request.getURI().getPath());
        if (!redisService.hasRoomId(roomId)) {
            log.trace("Attempt to connect to Non existing roomId : {}", roomId);
            return false;
        }
        String username = acost.getCurrentUsername();

        attributes.put("username", username);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {

    }

}
