package store.lastdance.dto.game;

import store.lastdance.domain.game.GameType;

import java.util.List;

public record GameResultRequestDTO(
        GameType gameType,
        List<String> participants,
        String result
) {
}
