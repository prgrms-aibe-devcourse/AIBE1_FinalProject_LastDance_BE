package store.lastdance.dto.game;

import store.lastdance.domain.game.GameType;

public record GameResultRequestDTO(
        GameType gameType,
        String participants,
        String result
) {
}
