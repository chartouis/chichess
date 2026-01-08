package kz.chitas.chess.model.auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginInput {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String fingerprint;
}
