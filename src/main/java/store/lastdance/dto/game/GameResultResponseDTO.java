package store.lastdance.dto.game;

import store.lastdance.domain.game.GameType;

import java.time.LocalDateTime;

public record GameResultResponseDTO(
        GameType gameType,
        String participants,
        String result,
        LocalDateTime createdAt
) {
}
