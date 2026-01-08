package kz.chitas.chess.model.logic;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveResponse {
    @NotBlank
    private boolean isValid;
    @NotBlank
    private RoomState state;
}
