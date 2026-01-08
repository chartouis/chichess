package kz.chitas.chess.model.logic;

import lombok.Data;

@Data
public class GameType {
    private String name;
    private int initialWhite;
    private int initialBlack;
    private int incrementWhite;
    private int incrementBlack;
}
