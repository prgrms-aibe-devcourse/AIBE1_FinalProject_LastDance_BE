package store.lastdance.service.game;

import org.springframework.stereotype.Service;
import store.lastdance.dto.game.GameResultRequestDTO;
import store.lastdance.dto.game.GameResultResponseDTO;

import java.util.List;
import java.util.UUID;

public interface GameService {
    void saveMyGameResult(GameResultRequestDTO gameResultRequestDTO, UUID userId);

    void saveGroupGameResult(GameResultRequestDTO gameResultRequestDTO, UUID userId, UUID groupId);

    List<GameResultResponseDTO> getMyGameResultList(UUID userId);

    List<GameResultResponseDTO> getGroupGameResultList(UUID userId, UUID groupId);
}
