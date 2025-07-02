package store.lastdance.controller.checklist;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.checklist.ChecklistRequestDTO;
import store.lastdance.dto.checklist.ChecklistResponseDTO;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.checklist.ChecklistService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Checklist", description = "할일 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @Operation(
            summary = "그룹 체크리스트 생성",
            description = "그룹에 새로운 체크리스트를 생성합니다. 그룹 멤버만 생성할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "체크리스트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> createGroupChecklist(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "체크리스트 생성 요청 데이터", required = true)
            @RequestBody ChecklistRequestDTO checklistRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("할일 생성 요청 - 그룹 ID: {}, 요청 데이터: {}, 사용자 ID: {}", groupId, checklistRequestDTO, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.createChecklist(checklistRequestDTO, userId, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(checklistResponseDTO, "체크리스트 생성 성공"));
    }

    @Operation(
            summary = "개인 체크리스트 생성",
            description = "개인 체크리스트를 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "체크리스트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> createPersonalChecklist(
            @Parameter(description = "체크리스트 생성 요청 데이터", required = true)
            @RequestBody ChecklistRequestDTO checklistRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("개인 할일 생성 요청 - 요청 데이터: {}, 사용자 ID: {}", checklistRequestDTO, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.createChecklist(checklistRequestDTO, userId, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(checklistResponseDTO, "개인 체크리스트 생성 성공")
        );
    }

    @Operation(
            summary = "그룹 체크리스트 조회",
            description = "그룹의 체크리스트를 조회합니다. 그룹 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<List<ChecklistResponseDTO>>> getGroupChecklist(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {
        UUID userId = user.getUserId();
        log.info("그룹 할일 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        List<ChecklistResponseDTO> checklistResponseDTOs = checklistService.getGroupChecklist(groupId, userId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTOs, "그룹 체크리스트 조회 성공"));
    }

    @Operation(
            summary = "개인 체크리스트 조회",
            description = "개인 체크리스트를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ChecklistResponseDTO>>> getPersonalChecklist(@AuthenticationPrincipal CustomOAuth2User user) {
        UUID userId = user.getUserId();
        log.info("개인 할일 조회 요청 - 사용자 ID: {}", userId);

        List<ChecklistResponseDTO> checklistResponseDTOs = checklistService.getPersonalChecklist(userId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTOs, "개인 체크리스트 조회 성공"));
    }

    @Operation(
            summary = "그룹 체크리스트 수정",
            description = "그룹 체크리스트를 수정합니다. 그룹 멤버만 수정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 수정 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "그룹 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트 또는 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/groups/{groupId}/{checklistId}")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> updateGroupChecklist(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "체크리스트 ID", required = true)
            @PathVariable Long checklistId,
            @Parameter(description = "체크리스트 수정 요청 데이터", required = true)
            @RequestBody ChecklistRequestDTO checklistRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 할일 수정 요청 - 그룹 ID: {}, 할일 ID: {}, 요청 데이터: {}, 사용자 ID: {}", groupId, checklistId, checklistRequestDTO, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.updateGroupChecklist(checklistId, checklistRequestDTO, userId, groupId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTO, "그룹 체크리스트 수정 성공"));
    }

    @Operation(
            summary = "개인 체크리스트 수정",
            description = "개인 체크리스트를 수정합니다. 작성자만 수정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 수정 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "수정 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/me/{checklistId}")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> updatePersonalChecklist(
            @Parameter(description = "체크리스트 ID", required = true)
            @PathVariable Long checklistId,
            @Parameter(description = "체크리스트 수정 요청 데이터", required = true)
            @RequestBody ChecklistRequestDTO checklistRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("개인 할일 수정 요청 - 할일 ID: {}, 요청 데이터: {}, 사용자 ID: {}", checklistId, checklistRequestDTO, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.updatePersonalChecklist(checklistId, checklistRequestDTO, userId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTO, "개인 체크리스트 수정 성공"));
    }

    @Operation(
            summary = "체크리스트 삭제",
            description = "체크리스트를 삭제합니다. 작성자만 삭제할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "체크리스트 삭제 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @DeleteMapping("/{checklistId}")
    public ResponseEntity<Void> deleteChecklist(
            @Parameter(description = "체크리스트 ID", required = true)
            @PathVariable Long checklistId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("할일 삭제 요청 - 할일 ID: {}, 사용자 ID: {}", checklistId, userId);

        checklistService.deleteChecklist(checklistId, userId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "체크리스트 완료 처리",
            description = "체크리스트를 완료 상태로 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 완료 처리 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/{checklistId}/complete")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> completeChecklist(
            @Parameter(description = "체크리스트 ID", required = true)
            @PathVariable Long checklistId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("할일 완료 요청 - 할일 ID: {}, 사용자 ID: {}", checklistId, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.completeChecklist(checklistId, userId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTO, "체크리스트 완료 처리 성공"));
    }

    @Operation(
            summary = "체크리스트 완료 취소",
            description = "완료된 체크리스트를 미완료 상태로 되돌립니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체크리스트 완료 취소 성공",
                    content = @Content(schema = @Schema(implementation = ChecklistResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/{checklistId}/undo")
    public ResponseEntity<ApiResponse<ChecklistResponseDTO>> undoChecklist(
            @Parameter(description = "체크리스트 ID", required = true)
            @PathVariable Long checklistId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("할일 완료 취소 요청 - 할일 ID: {}, 사용자 ID: {}", checklistId, userId);

        ChecklistResponseDTO checklistResponseDTO = checklistService.undoChecklist(checklistId, userId);

        return ResponseEntity.ok(ApiResponse.success(checklistResponseDTO, "체크리스트 완료 취소 성공"));
    }
}