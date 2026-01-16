package kz.chitas.chess.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kz.chitas.chess.model.logic.PageResponse;
import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.service.logic.ChessService;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping
public class MainController {

    private final ChessService chess;

    public MainController(ChessService chess) {
        this.chess = chess;
    }

    @GetMapping("/api/{gameId}")
    public ResponseEntity<RoomState> getGame(@PathVariable("gameId") String gameId) {
        if (gameId == null || !gameId.matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")) {
            return ResponseEntity.badRequest().build();
        }
        RoomState state = chess.getGame(UUID.fromString(gameId));
        if (!state.equals(null)) {
            return ResponseEntity.ok(state);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/history")
    public ResponseEntity<PageResponse<RoomState>> getHistory(
            @RequestParam String username,
            @RequestParam int page,
            @RequestParam int size) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (page < 0)
            page = 0;
        if (size <= 0 || size > 100)
            size = 20; // default bounds

        return ResponseEntity.ok(chess.getGameHistoryByUsername(username, page, size));

    }

    @GetMapping("/api/rating/{username}")
    public ResponseEntity<Integer> getRatingByUsername(@PathVariable("username") String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int rating = chess.getRating(username);
        if (rating == 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(rating);
    }

}
