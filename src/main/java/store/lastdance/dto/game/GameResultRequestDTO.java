package store.lastdance.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import store.lastdance.domain.game.GameType;

import java.util.List;

public record GameResultRequestDTO(

        @NotNull(message = "게임 타입은 필수 항목입니다.")
        GameType gameType,

        @NotEmpty(message = "참가자는 최소 1명 이상이어야 합니다.")
        List<@NotBlank(message = "참가자 이름은 공백일 수 없습니다.") String> participants,

        @NotBlank(message = "게임 결과는 필수 항목입니다.")
        String result,

        @NotBlank(message = "게임 설명은 필수 항목입니다.")
        String penalty
) {
}
