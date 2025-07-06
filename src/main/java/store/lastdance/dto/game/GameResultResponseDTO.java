package store.lastdance.dto.game;

import store.lastdance.domain.game.GameType;

import java.time.LocalDateTime;
import java.util.List;

public record GameResultResponseDTO(
        GameType gameType,
        List<String> participants,
        String result,
        LocalDateTime createdAt
) {
}
