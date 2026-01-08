package kz.chitas.chess.service.logic;

import java.util.UUID;

import org.springframework.stereotype.Service;

import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.repo.RstateRepo;

@Service
public class PostgresService {
    private final RstateRepo repo;

    public PostgresService(RstateRepo repo) {
        this.repo = repo;
    }

    public RoomState save(RoomState state) {
        return repo.save(state);
    }

    public RoomState get(UUID gameId) {
        return repo.findById(gameId).orElseThrow();
    }

    public boolean has(UUID id) {
        return repo.existsById(id);
    }

}
