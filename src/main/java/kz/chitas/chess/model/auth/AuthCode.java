package kz.chitas.chess.model.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthCode {
    @NotBlank
    private String code;

}