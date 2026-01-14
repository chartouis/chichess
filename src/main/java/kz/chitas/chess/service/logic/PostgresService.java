package kz.chitas.chess.service.logic;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import kz.chitas.chess.model.auth.User;
import kz.chitas.chess.model.logic.PageResponse;
import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.repo.RstateRepo;
import kz.chitas.chess.repo.UsersRepo;

@Service
public class PostgresService {
    private final RstateRepo repo;
    private final UsersRepo urepo;

    public PostgresService(RstateRepo repo, UsersRepo urepo) {
        this.repo = repo;
        this.urepo = urepo;
    }

    public RoomState save(RoomState state) {
        return repo.save(state);
    }

    public RoomState get(UUID gameId) {
        return repo.findById(gameId).orElseThrow();
    }

    public int getRating(String username) {
        return urepo.findRatingByUsername(username);
    }

    public void setRating(String username, int newRating) {
        User user = urepo.findByUsername(username).orElseThrow();
        user.setRating(newRating);
    }

    public boolean has(UUID id) {
        return repo.existsById(id);
    }

    public PageResponse<RoomState> getGameHistoryByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoomState> roomPage = repo.findByUsername(username, pageable);

        return new PageResponse<>(
                roomPage.getContent(),
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages(),
                roomPage.hasNext(),
                roomPage.hasPrevious());
    }

}
