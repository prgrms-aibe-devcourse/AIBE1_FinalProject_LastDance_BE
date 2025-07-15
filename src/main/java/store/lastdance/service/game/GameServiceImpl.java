package store.lastdance.service.game;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.game.GameResult;
import store.lastdance.domain.group.Group;
import store.lastdance.dto.game.GameResultRequestDTO;
import store.lastdance.dto.game.GameResultResponseDTO;
import store.lastdance.repository.game.GameResultRepository;
import store.lastdance.service.group.GroupService;
import store.lastdance.service.user.UserService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService{

    private final UserService userService;
    private final GameResultRepository gameResultRepository;
    private final GroupService groupService;

    @Override
    @Transactional
    public void saveMyGameResult(GameResultRequestDTO gameResultRequestDTO, UUID userId) {


        userService.validateUserExists(userId);

        GameResult gameResult = GameResult.builder()
                .user(userService.findByUserId(userId))
                .group(null)
                .gameType(gameResultRequestDTO.gameType())
                .participants(gameResultRequestDTO.participants())
                .result(gameResultRequestDTO.result())
                .penalty(gameResultRequestDTO.penalty())
                .build();

        gameResultRepository.save(gameResult);

    }

    @Override
    @Transactional
    public void saveGroupGameResult(GameResultRequestDTO gameResultRequestDTO, UUID userId, UUID groupId) {


        userService.validateUserExists(userId);

        GameResult gameResult = GameResult.builder()
                .user(null)
                .group(groupService.getGroupById(groupId, userId))
                .gameType(gameResultRequestDTO.gameType())
                .participants(gameResultRequestDTO.participants())
                .result(gameResultRequestDTO.result())
                .penalty(gameResultRequestDTO.penalty())
                .build();

        gameResultRepository.save(gameResult);

    }

    @Override
    public List<GameResultResponseDTO> getMyGameResultList(UUID userId) {


        userService.validateUserExists(userId);

        List<GameResult> gameResults = gameResultRepository.findByUser(userService.findByUserId(userId));

        List<GameResultResponseDTO> responseDTOs = gameResults.stream()
                .map(gameResult -> new GameResultResponseDTO(
                        gameResult.getGameType(),
                        gameResult.getParticipants(),
                        gameResult.getResult(),
                        gameResult.getPenalty(),
                        gameResult.getCreatedAt()))
                .toList();


        return responseDTOs;
    }

    @Override
    public List<GameResultResponseDTO> getGroupGameResultList(UUID userId, UUID groupId) {


        userService.validateUserExists(userId);
        Group group = groupService.getGroupById(groupId, userId);

        List<GameResult> gameResults = gameResultRepository.findByGroup(group);

        List<GameResultResponseDTO> responseDTOs = gameResults.stream()
                .map(gameResult -> new GameResultResponseDTO(
                        gameResult.getGameType(),
                        gameResult.getParticipants(),
                        gameResult.getResult(),
                        gameResult.getPenalty(),
                        gameResult.getCreatedAt()))
                .toList();


        return responseDTOs;
    }
}
