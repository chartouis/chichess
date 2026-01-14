package kz.chitas.chess.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kz.chitas.chess.model.logic.RoomState;

public interface RstateRepo extends JpaRepository<RoomState, UUID> {
    @Query("""
                SELECT r FROM RoomState r
                WHERE r.white = :username OR r.black = :username
                ORDER BY r.createdAt DESC
            """)
    Page<RoomState> findByUsername(
            @Param("username") String username,
            Pageable pageable);

}
