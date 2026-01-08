package kz.chitas.chess.model.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JWT {
    private String token;

    public JWT(String token) {
        this.token = token;
    }
}
