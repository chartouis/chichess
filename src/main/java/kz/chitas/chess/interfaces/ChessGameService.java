package kz.chitas.chess.interfaces;

import java.util.UUID;

//This interface assumes that the input that you give the service is TRUSTED
public interface ChessGameService {

    boolean doMove(UUID roomId, String from, String to, String promotion, String username);

    boolean acceptDraw(UUID roomId, String username);

    boolean offerDraw(UUID roomId, String username);

    boolean resign(UUID roomId, String username);
}
