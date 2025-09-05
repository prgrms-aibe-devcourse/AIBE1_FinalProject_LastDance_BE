package store.lastdance.controller.group;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.dto.group.*;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.group.GroupV2QueryService;
import store.lastdance.service.group.GroupV2CommandService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Group", description = "그룹 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v2/groups")
@RequiredArgsConstructor
public class GroupV2Controller {

    private final GroupV2CommandService groupV2CommandService;
    private final GroupV2QueryService groupV2QueryService;


    @Operation(
            summary = "그룹 생성",
            description = "새로운 그룹을 생성합니다. 그룹 이름, 최대 인원수, 예산을 설정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "그룹 생성 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
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
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponseDTO>> createGroup(
            @Parameter(description = "그룹 생성 요청 데이터", required = true)
            @Valid @RequestBody GroupRequestDTO groupRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);

        }
        
        UUID userId = user.getUserId();
        log.info("그룹 생성 요청 - 사용자 ID: {}", userId);

        GroupResponseDTO groupResponseDTO = groupV2CommandService.createGroup(groupRequestDTO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(groupResponseDTO, "그룹 생성 성공"));
    }

    @Operation(
            summary = "그룹 참여 신청",
            description = "초대 코드를 통해 그룹 참여를 신청합니다. 신청 후 그룹 오너의 승인을 기다려야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 참여 신청 성공",
                    content = @Content(schema = @Schema(implementation = Void.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 유효하지 않은 초대 코드",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 초대 코드를 가진 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<Void>> applyGroup(
            @Parameter(description = "초대 코드 요청 데이터", required = true)
            @Valid @RequestBody InviteCodeRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 참여 신청 요청 - 사용자 ID: {}, 초대 코드: {}", userId, request.inviteCode());

        groupV2CommandService.applyGroup(request.inviteCode(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "그룹 참여 신청이 완료되었습니다."));
    }

    @GetMapping("/{groupId}/applications")
    @Operation(
            summary = "그룹 참여 신청 목록 조회",
            description = "그룹 오너가 그룹 참여 신청 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 참여 신청 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
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
    public ResponseEntity<ApiResponse<List<GroupApplicationResponseDTO>>> getGroupApplications(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 참여 신청 목록 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        List<GroupApplicationResponseDTO> applications = groupV2QueryService.getGroupApplications(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(applications, "그룹 참여 신청 목록 조회 성공"));
    }

    @Operation(
            summary = "그룹 참여 신청 승인",
            description = "그룹 참여 신청을 승인합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 참여 승인 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/applications/accept")
    public ResponseEntity<ApiResponse<GroupResponseDTO>> acceptGroupApplication(
            @Parameter(description = "그룹 참여 신청 승인 요청 데이터", required = true)
            @Valid @RequestBody GroupApplicationRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();
        log.info("그룹 참여 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.groupId(), request.userId(), currentUserId);

        GroupResponseDTO groupResponseDTO = groupV2CommandService.acceptGroupApplication(request.groupId(), request.userId(), currentUserId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(groupResponseDTO, "그룹 참여 승인 성공"));
    }

    @Operation(
            summary = "그룹 참여 신청 거부",
            description = "그룹 참여 신청을 거부합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "그룹 참여 거부 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/applications/reject")
    public ResponseEntity<ApiResponse<Void>> rejectGroupApplication(
            @Parameter(description = "그룹 참여 신청 거부 요청 데이터", required = true)
            @Valid @RequestBody GroupApplicationRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();
        log.info("그룹 참여 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.groupId(), request.userId(), currentUserId);

        groupV2CommandService.rejectGroupApplication(request.groupId(), request.userId(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "그룹 참여 신청이 거절되었습니다."));
    }

    @Operation(
            summary = "내 그룹 목록 조회",
            description = "현재 사용자가 참여한 모든 그룹의 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
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
    public ResponseEntity<ApiResponse<List<GroupResponseDTO>>> getMyGroup(@AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("사용자 그룹 조회 요청 - 사용자 ID: {}", userId);

        List<GroupResponseDTO> groups = groupV2QueryService.getGroupsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(groups, "그룹 목록 조회 성공"));
    }

    @Operation(
            summary = "그룹 상세 조회",
            description = "특정 그룹의 상세 정보를 조회합니다. 해당 그룹의 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
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
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponseDTO>> getGroup(
            @Parameter(description = "조회할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        GroupResponseDTO groupResponseDTO = groupV2QueryService.getGroupResponseDTOById(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(groupResponseDTO, "그룹 조회 성공"));
    }

    @Operation(
            summary = "그룹 정보 수정",
            description = "그룹의 정보를 수정합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 수정 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
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
    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponseDTO>> updateGroup(
            @Parameter(description = "수정할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "그룹 수정 요청 데이터", required = true)
            @Valid @RequestBody GroupRequestDTO groupRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        GroupResponseDTO updatedGroup = groupV2CommandService.updateGroup(groupId, groupRequestDTO, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedGroup, "그룹 수정 성공"));
    }

    @Operation(
            summary = "그룹 삭제",
            description = "그룹을 삭제합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "그룹 삭제 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
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
    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @Parameter(description = "삭제할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        groupV2CommandService.deleteGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "그룹이 삭제되었습니다."));
    }

    @Operation(
            summary = "그룹 탈퇴",
            description = "현재 사용자가 그룹에서 탈퇴합니다. 그룹 오너는 탈퇴할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "그룹 탈퇴 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 그룹 오너는 탈퇴 불가",
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
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<Void> leaveGroup(
            @Parameter(description = "탈퇴할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        groupV2CommandService.leaveGroup(groupId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "그룹 멤버 목록 조회",
            description = "그룹의 모든 멤버 목록을 조회합니다. 해당 그룹의 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 멤버 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupMemberDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
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
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberDTO>>> getGroupMembers(
            @Parameter(description = "멤버 목록을 조회할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();
        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        List<GroupMemberDTO> members = groupV2QueryService.getGroupMembers(groupId, userId);
        return ResponseEntity.ok(ApiResponse.success(members, "그룹 멤버 목록 조회 성공"));
    }

    @Operation(
            summary = "멤버를 그룹 오너로 승격",
            description = "그룹 멤버를 그룹 오너로 승격시킵니다. 현재 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "멤버 승격 성공",
                    content = @Content(schema = @Schema(implementation = Void.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 멤버를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/{groupId}/members/{userId}/promote")
    public ResponseEntity<ApiResponse<Void>> promoteMemberToOwner(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "승격할 멤버의 사용자 ID", required = true)
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();
        log.info("그룹 멤버 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        groupV2CommandService.promoteMemberToOwner(groupId, userId, currentUserId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "멤버가 그룹 오너로 승격되었습니다."));
    }

    @Operation(
            summary = "그룹 멤버 제거",
            description = "그룹에서 멤버를 제거합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "멤버 제거 성공",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 멤버를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "제거할 멤버의 사용자 ID", required = true)
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();
        log.info("그룹 멤버 제거 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        groupV2CommandService.removeMember(groupId, userId, currentUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}