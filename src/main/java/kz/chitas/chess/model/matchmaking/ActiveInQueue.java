package kz.chitas.chess.model.matchmaking;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveInQueue {
    int casual_blitz;
    int casual_bullet;
    int casual_classical;
    int casual_rapid;
    int rated_blitz;
    int rated_bullet;
    int rated_classical;
    int rated_rapid;

    public ActiveInQueue() {
        this.casual_blitz = 0;
        this.casual_bullet = 0;
        this.casual_classical = 0;
        this.casual_rapid = 0;
        this.rated_blitz = 0;
        this.rated_bullet = 0;
        this.rated_classical = 0;
        this.rated_rapid = 0;
    }
}
