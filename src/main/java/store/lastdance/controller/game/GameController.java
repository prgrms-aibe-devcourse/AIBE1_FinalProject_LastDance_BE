package store.lastdance.controller.game;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.dto.game.GameResultRequestDTO;
import store.lastdance.dto.game.GameResultResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.game.GameService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Game", description = "게임 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(
            summary = "내 게임 결과 저장",
            description = "내 게임 결과를 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 게임 결과 저장 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/result/me")
    public ResponseEntity<ApiResponse<Void>> saveGameResult(
            @Valid @RequestBody GameResultRequestDTO gameResultRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {
        log.info("게임 결과 저장 요청: {}", gameResultRequestDTO);

        gameService.saveMyGameResult(gameResultRequestDTO, user.getUserId());

        log.info("게임 결과 저장 완료: {}", gameResultRequestDTO);

        return ResponseEntity.ok(ApiResponse.success(null, "내 게임 결과 저장 성공"));
    }

    @Operation(
            summary = "그룹 게임 결과 저장",
            description = "그룹의 게임 결과를 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 게임 결과 저장 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹에 속해있지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/result/group/{groupId}")
    public ResponseEntity<ApiResponse<Void>> saveGameResult(
            @Parameter(description = "게임 결과를 저장할 그룹 ID", required = true) @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody GameResultRequestDTO gameResultRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        log.info("게임 결과 저장 요청: {}", gameResultRequestDTO);

        gameService.saveGroupGameResult(gameResultRequestDTO, user.getUserId(), groupId);

        log.info("게임 결과 저장 완료: {}", gameResultRequestDTO);

        return ResponseEntity.ok(ApiResponse.success(null, "그룹 게임 결과 저장 성공"));
    }

    @Operation(
            summary = "내 게임 결과 목록 조회",
            description = "내 게임 결과 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 게임 결과 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GameResultResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/result/me")
    public ResponseEntity<ApiResponse<List<GameResultResponseDTO>>> getGameResultList(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {

        log.info("내 게임 결과 조회 요청");

        List<GameResultResponseDTO> gameResults = gameService.getMyGameResultList(user.getUserId());

        log.info("내 게임 결과 조회 완료: results={}", gameResults);

        return ResponseEntity.ok(ApiResponse.success(gameResults, "내 게임 결과 목록 조회 성공"));
    }

    @Operation(
            summary = "그룹 게임 결과 목록 조회",
            description = "그룹의 게임 결과 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 게임 결과 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GameResultResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹에 속해있지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/result/group/{groupId}")
    public ResponseEntity<ApiResponse<List<GameResultResponseDTO>>> getGameResultList(
            @Parameter(description = "조회할 그룹 ID", required = true) @PathVariable("groupId") UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {

        log.info("그룹 게임 결과 조회 요청: groupId={}", groupId);

        List<GameResultResponseDTO> gameResults = gameService.getGroupGameResultList(user.getUserId(), groupId);

        log.info("그룹 게임 결과 조회 완료: results={}", gameResults);

        return ResponseEntity.ok(ApiResponse.success(gameResults, "그룹 게임 결과 목록 조회 성공"));
    }

}
