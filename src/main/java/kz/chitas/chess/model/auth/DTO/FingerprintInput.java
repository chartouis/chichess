package kz.chitas.chess.model.auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FingerprintInput {
    @NotBlank
    private String hash;
}
