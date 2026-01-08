package kz.chitas.chess.model.auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCodeInput {
    @NotBlank
    private String code;
    @NotBlank
    private String fingerprint;
}
