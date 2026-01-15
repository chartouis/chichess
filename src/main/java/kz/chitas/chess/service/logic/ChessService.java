package kz.chitas.chess.service.logic;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;

import jakarta.transaction.Transactional;
import kz.chitas.chess.model.logic.GameStatus;
import kz.chitas.chess.model.logic.GameType;
import kz.chitas.chess.model.logic.PageResponse;
import kz.chitas.chess.model.logic.RoomState;
import kz.chitas.chess.utils.GamePresetsLoader;
import kz.chitas.chess.utils.RoomFullException;
import kz.chitas.chess.utils.RoomNotFoundException;
import kz.chitas.chess.utils.SamePlayerException;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ChessService {

    private final RedisService redisService;
    private final PostgresService postgresService;
    private final static int K_FACTOR = Integer.parseInt(System.getenv("K_FACTOR"));

    public ChessService(RedisService redisService, GamePresetsLoader gLoader, PostgresService postgresService) {
        this.redisService = redisService;
        this.postgresService = postgresService;
        // this.gLoader = gLoader;
        settings = new HashMap<>(gLoader.loadPresets());

        loadRooms();
    }

    private HashMap<UUID, Board> roomBoards = new HashMap<>();
    private HashMap<UUID, MoveList> roomMoves = new HashMap<>();
    private HashMap<String, GameType> settings;

    public boolean doMove(UUID roomId, String from, String to, String promotion, String player) {
        from = from.toUpperCase();
        to = to.toUpperCase();
        Move move;
        if (!promotion.equals("")) {
            move = new Move(Square.fromValue(from), Square.fromValue(to), Piece.fromFenSymbol(promotion)); // possible
            // exception
            // abuse. because
            // im not
            // checking
        } else { // of the piece with that promotion exists
            move = new Move(Square.fromValue(from), Square.fromValue(to));
        }

        RoomState state = getRoomState(roomId);
        Board board = roomBoards.get(roomId);
        MoveList moveList = roomMoves.get(roomId);
        if (board == null || moveList == null || state == null) {
            return false;
        }
        Piece fromPiece = board.getPiece(move.getFrom());
        if (Piece.NONE.equals(fromPiece)) {
            return false;
        }

        if (state.getBlack().equals(null) || state.getWhite().equals(null)) {
            return false;
        }

        String playerToMove = board.getSideToMove() == Side.WHITE ? state.getWhite() : state.getBlack();

        if (player.equals(playerToMove) && isMoveLegal(board, move)
                && state.getStatus() == GameStatus.ONGOING) {

            GameType type = settings.get(state.getGameType());
            if (state.isAfkTimeout()) {
                closeRoom(state, GameStatus.ABANDONED, "");
                return true;
            }
            state.updateTimer(type.getIncrementWhite(), type.getIncrementBlack());
            if (state.checkTimerRunout()) {
                redisService.saveRoomState(state);
                return true;
            }

            if (board.doMove(move, true)) {
                moveList.add(move);

                log.info("Move : {} on room : {}", move, roomId);
                state.addTimestamp();
                state.setPosition(board.getFen());
                state.setHistory(moveList.toSan());
                GameStatus status = checkBoardStatus(board);
                state.setStatus(status);

                if (!playerToMove.equals(state.getDrawOfferedBy())) {
                    state.setDrawOfferedBy(null);
                }

                if (status == GameStatus.CHECKMATE) {
                    if (board.getSideToMove() == Side.WHITE) {
                        closeRoom(state, status, state.getBlack());
                    } else {
                        closeRoom(state, status, state.getWhite());
                    }
                    log.info("Game ended with: {}", status.name());
                    return true;
                }

                redisService.saveRoomState(state);
                return true;
            }
        }

        return false;
    }

    private GameStatus checkBoardStatus(Board board) {
        if (board.isMated()) {
            return GameStatus.CHECKMATE;
        }

        if (board.isStaleMate()) {
            return GameStatus.STALEMATE;
        }

        if (board.isDraw() || board.isRepetition() || board.isInsufficientMaterial()) {
            return GameStatus.DRAW;
        }

        return GameStatus.ONGOING;
    }

    private UUID generateRoomUUID() {
        return UUID.randomUUID();
    }

    public RoomState createRoom(String creator, String white, String black, String gameType, boolean isRated) {
        log.info("Creating {} for creator: {}", gameType, creator);
        UUID roomId = generateRoomUUID();
        GameType type = settings.get(gameType);
        log.debug("Generated roomId: {}", roomId);

        Board board = new Board();
        MoveList mList = new MoveList();

        roomBoards.put(roomId, board);
        roomMoves.put(roomId, mList);
        String history = mList.toSan();

        log.debug("Initialized board and move list for roomId: {}", roomId);

        RoomState roomState = new RoomState.Builder()
                .creator(creator)
                .white(white)
                .black(black)
                .id(roomId)
                .position(board.getFen())
                .history(history)
                .status(GameStatus.ONGOING)
                .winner("")
                .remainingWhite(type.getInitialWhite())
                .remainingBlack(type.getInitialBlack())
                .gameType(gameType)
                .isRated(isRated)
                .build();

        if (white.equals(black)) {
            throw new SamePlayerException(roomState, white);
        }
        redisService.saveRoomState(roomState);
        log.info("Room created: {}", roomId);
        return roomState;
    }

    public RoomState joinRoom(UUID roomId, String visitor) {
        RoomState rm = redisService.getRoomState(roomId);
        if (rm == null) {
            throw new RoomNotFoundException(rm);
        }
        if (rm.getCreator().equals(visitor) || rm.getWhite().equals(rm.getBlack())) {
            throw new SamePlayerException(rm, visitor);
        }
        if (rm.getWhite().equals("")) {
            rm.setWhite(visitor);
            log.info("player: {} joined room: {} as white", visitor, roomId);
        } else if (rm.getBlack().equals("")) {
            rm.setBlack(visitor);
            log.info("player: {} joined room: {} as black", visitor, roomId);
        } else {
            throw new RoomFullException(rm);
        }
        if (rm.getGameStartedAt() == null) {
            rm.setGameStartedAt(Instant.now());
        }
        redisService.saveRoomState(rm);

        return rm;
    }

    public void deleteRoom(UUID roomId) {
        redisService.deleteRoom(roomId);
        log.info("Room deleted: {}", roomId);
    }

    public RoomState getRoomState(UUID roomId) {
        log.debug("Getting a roomState of : {}", roomId);
        RoomState state = redisService.getRoomState(roomId);
        if (state == null) {
            return new RoomState.Builder().build();
        }
        return state;

    }

    public void loadRooms() {
        log.info("-------------------LOADING STARTED------------------");
        for (RoomState room : redisService.getAllExistingRooms()) {
            try {
                Board board = new Board();
                MoveList mList = new MoveList();

                board.loadFromFen(room.getPosition());
                mList.loadFromSan(room.getHistory());

                roomBoards.put(room.getId(), board);
                roomMoves.put(room.getId(), mList);
                log.info("loaded room : {}", room.getId());
            } catch (Exception e) {
                log.error("Skipping room " + room.getId() + ": corrupted data");
            }
        }
        log.info("------------------LOADING FINISHED------------------");

    }

    private boolean isMoveLegal(Board board, Move move) {
        List<Move> legalMoves = board.legalMoves();
        for (Move legalMove : legalMoves) {
            if (legalMove.equals(move)) {
                return true;
            }
        }
        return false;

    }

    // gets every existing RoomState on redis
    public List<RoomState> getAllExistingRooms() {
        return redisService.getAllExistingRooms();
    }

    // gets the id of every existing room
    public List<String> getAllRoomIds() {
        List<String> idList = redisService.getAllExistingRooms().stream()
                .map(room -> room.getId().toString())
                .collect(Collectors.toList());
        return idList;
    }

    public HashMap<UUID, Board> getBoards() {
        return roomBoards;
    }

    public boolean acceptDraw(UUID roomId, String username) {
        RoomState state = getRoomState(roomId);
        if (state == null) {
            return false;
        }
        if (!Objects.equals(state.getDrawOfferedBy(), username)
                && state.hasPlayer(username) && !Objects.equals(state.getDrawOfferedBy(), null)) {
            closeRoom(state, GameStatus.DRAW, null);
            return true;
        }
        return false;
    }

    public boolean offerDraw(UUID roomId, String username) {
        RoomState state = getRoomState(roomId);
        if (state == null) {
            return false;
        }
        if (Objects.equals(state.getDrawOfferedBy(), null)
                && state.hasPlayer(username)) {
            state.setDrawOfferedBy(username);
            redisService.saveRoomState(state);
            return true;
        }
        return false;
    }

    // The username means the player who resigns. So the winner is the opposite
    // player

    public boolean resign(UUID roomId, String username) {
        RoomState state = getRoomState(roomId);
        Board board = roomBoards.get(roomId);
        if (board == null || state == null) {
            return false;
        }
        if (state.getBlack().equals(username)) {
            closeRoom(state, GameStatus.RESIGNED, state.getWhite());
            return true;
        }
        if (state.getWhite().equals(username)) {
            closeRoom(state, GameStatus.RESIGNED, state.getBlack());
            return true;
        }
        return false;

    }

    private void closeRoom(RoomState state, GameStatus status, String winner) {
        state.setWinner(winner);
        state.setStatus(status);
        // state.setActive(false);
        roomBoards.remove(state.getId());
        redisService.saveRoomState(state);
    }

    // Deletes from redis then saves on postgres
    @Transactional
    public void persistFromRedis(RoomState state) {
        UUID id = state.getId();
        if (postgresService.has(id)) {
            postgresService.save(state);
            redisService.deleteRoom(id);
            return;
        }
        if (!redisService.hasRoomId(id)) {
            return;
        }
        try {
            postgresService.save(state);
            redisService.deleteRoom(id);
        } catch (DataIntegrityViolationException e) {
            redisService.deleteRoom(id);
        }
    }

    // if persisted true, otherwise false
    // updates rating on game end
    public boolean checkAndPersist(RoomState state) {
        GameStatus status = state.getStatus();
        if (status != GameStatus.WAITING && status != GameStatus.ONGOING) {
            updateRating(state);
            persistFromRedis(state);
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = 10000)
    public void flushRedisToPostgres() {
        getAllExistingRooms()
                .forEach(room -> postgresService.save(room));
    }

    @Scheduled(fixedRate = 10000)
    public void updateEveryRoomState() {
        getAllExistingRooms().forEach(room -> updateRoomState(room));
    }

    public void updateRoomState(RoomState state) {
        if (state.getGameStartedAt() == null) {
            return;
        }
        state.updateTimer(0, 0);
        state.checkTimerRunout();
        if (state.isAfkTimeout()) {
            closeRoom(state, GameStatus.ABANDONED, "");
            persistFromRedis(state);
            return;
        }

        if (!isActive(state)) {
            redisService.saveRoomState(state);
            persistFromRedis(state);
            return;
        }
        redisService.saveRoomState(state);

    }

    public boolean isActive(RoomState state) {
        return state.getStatus() == GameStatus.ONGOING || state.getStatus() == GameStatus.WAITING;
    }

    public RoomState getGame(UUID gameUuid) {
        return postgresService.get(gameUuid);
    }

    public Set<String> getRoomsByUsername(String username) {
        log.info("Set of rooms of : {}", username);
        return redisService.getRoomsByUsername(username);
    }

    public void updateRating(RoomState state) {
        if (!state.isRated())
            return;

        String winner = state.getWinner();
        if (winner == null || winner.isBlank())
            return;

        boolean draw = state.getStatus() == GameStatus.DRAW;
        String white = state.getWhite();
        String black = state.getBlack();

        int whiteRating = postgresService.getRating(white);
        int blackRating = postgresService.getRating(black);

        double expectedWhite = probability(blackRating, whiteRating);
        double expectedBlack = probability(whiteRating, blackRating);

        double scoreWhite;
        double scoreBlack;

        if (draw) {
            scoreWhite = 0.5;
            scoreBlack = 0.5;
        } else if (winner.equals(white)) {
            scoreWhite = 1.0;
            scoreBlack = 0.0;
        } else {
            scoreWhite = 0.0;
            scoreBlack = 1.0;
        }

        int newWhite = (int) Math.round(whiteRating + K_FACTOR * (scoreWhite - expectedWhite));
        int newBlack = (int) Math.round(blackRating + K_FACTOR * (scoreBlack - expectedBlack));
        log.info("updated elo {} : {}   ||   {} : {}", white, newWhite, black, newBlack);
        postgresService.setRating(white, newWhite);
        postgresService.setRating(black, newBlack);
    }

    // with check
    public int getRating(String username) {
        if (!postgresService.existsByUsername(username)) {
            return 0;
        }
        return postgresService.getRating(username);
    }

    // probability of winning of the player with rating 2
    private double probability(int rating1, int rating2) {
        return 1.0 / (1 + Math.pow(10, (rating1 - rating2) / 400.0));
    }

    public PageResponse<RoomState> getGameHistoryByUsername(String username, int page, int size) {
        log.info("{} Fetched history of the player : {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), username);
        return postgresService.getGameHistoryByUsername(username, page, size);
    }
}