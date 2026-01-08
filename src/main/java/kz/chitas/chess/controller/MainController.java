package kz.chitas.chess.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        RoomState state = chess.getGame(UUID.fromString(gameId));
        if (!state.equals(null)) {
            return ResponseEntity.ok(state);
        }
        return ResponseEntity.notFound().build();
    }

}
