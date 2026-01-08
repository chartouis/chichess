package kz.chitas.chess.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kz.chitas.chess.model.logic.RoomState;

public interface RstateRepo extends JpaRepository<RoomState, UUID> {

}
