package store.lastdance.controller.group;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.auth.CustomOAuth2User;
import store.lastdance.dto.group.*;
import store.lastdance.service.group.GroupService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Tag(name = "Group", description = "그룹 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;


    @Operation(
            summary = "그룹 생성",
            description = "새로운 그룹을 생성합니다. 그룹 이름, 최대 인원수, 예산을 설정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "그룹 생성 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<GroupResponseDTO> createGroup(
            @Parameter(description = "그룹 생성 요청 데이터", required = true)
            @RequestBody GroupRequestDTO groupRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 생성 요청 - 사용자 ID: {}", userId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.createGroup(groupRequestDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("사용자 조회 실패 - ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 생성 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 참여 신청",
            description = "초대 코드를 통해 그룹 참여를 신청합니다. 신청 후 그룹 오너의 승인을 기다려야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 참여 신청 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 유효하지 않은 초대 코드",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 초대 코드를 가진 그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PostMapping("/applications")
    public ResponseEntity<Map<String, String>> applyGroup(
            @Parameter(description = "초대 코드 요청 데이터", required = true)
            @RequestBody InviteCodeRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 참여 신청 요청 - 사용자 ID: {}, 초대 코드: {}", userId, request.getInviteCode());

        try {
            groupService.applyGroup(request.getInviteCode(), userId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "그룹 참여 신청이 완료되었습니다."));
        } catch (NoSuchElementException e) {
            log.warn("그룹 조회 실패 - 초대 코드: {}", request.getInviteCode(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", "해당 초대 코드를 가진 그룹을 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 참여 신청 실패 - 사용자 ID: {}, 초대 코드: {}", userId, request.getInviteCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 참여 신청에 실패했습니다."));
        }
    }

    @Operation(
            summary = "그룹 참여 신청 승인",
            description = "그룹 참여 신청을 승인합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 참여 승인 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 사용자를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PatchMapping("/applications/accept")
    public ResponseEntity<GroupResponseDTO> acceptGroupApplication(
            @Parameter(description = "그룹 참여 신청 승인 요청 데이터", required = true)
            @RequestBody GroupApplicationRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 참여 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.getGroupId(), request.getUserId(), currentUserId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.acceptGroupApplication(request.getGroupId(), request.getUserId(), currentUserId);
            return ResponseEntity.status(HttpStatus.OK).body(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("그룹 참여 실패 - 그룹 ID: {}, 사용자 ID: {}", request.getGroupId(), request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 참여 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.getGroupId(), request.getUserId(), currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 참여 신청 거부",
            description = "그룹 참여 신청을 거부합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "그룹 참여 거부 성공",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 사용자를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PatchMapping("/applications/reject")
    public ResponseEntity<Void> rejectGroupApplication(
            @Parameter(description = "그룹 참여 신청 거부 요청 데이터", required = true)
            @RequestBody GroupApplicationRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 참여 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.getGroupId(), request.getUserId(), currentUserId);

        try {
            groupService.rejectGroupApplication(request.getGroupId(), request.getUserId(), currentUserId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 참여 거절 실패 - 그룹 ID: {}, 사용자 ID: {}", request.getGroupId(), request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 참여 거절 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", request.getGroupId(), request.getUserId(), currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "내 그룹 목록 조회",
            description = "현재 사용자가 참여한 모든 그룹의 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ResponseEntity<List<GroupResponseDTO>> getMyGroup(@AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("사용자 그룹 조회 요청 - 사용자 ID: {}", userId);

        try {
            List<GroupResponseDTO> groups = groupService.getGroupsByUserId(userId);
            return ResponseEntity.ok(groups);
        } catch (NoSuchElementException e) {
            log.warn("사용자 그룹 조회 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("사용자 그룹 조회 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 상세 조회",
            description = "특정 그룹의 상세 정보를 조회합니다. 해당 그룹의 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroup(
            @Parameter(description = "조회할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.getGroupById(groupId, userId);
            return ResponseEntity.ok(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("그룹 조회 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 조회 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 정보 수정",
            description = "그룹의 정보를 수정합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 수정 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> updateGroup(
            @Parameter(description = "수정할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "그룹 수정 요청 데이터", required = true)
            @RequestBody GroupRequestDTO groupRequestDTO,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            GroupResponseDTO updatedGroup = groupService.updateGroup(groupId, groupRequestDTO, userId);
            return ResponseEntity.ok(updatedGroup);
        } catch (NoSuchElementException e) {
            log.warn("그룹 수정 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 수정 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 삭제",
            description = "그룹을 삭제합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "그룹 삭제 성공",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "삭제할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            groupService.deleteGroup(groupId, userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 삭제 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 삭제 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 탈퇴",
            description = "현재 사용자가 그룹에서 탈퇴합니다. 그룹 오너는 탈퇴할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "그룹 탈퇴 성공",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 그룹 오너는 탈퇴 불가",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<Void> leaveGroup(
            @Parameter(description = "탈퇴할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            groupService.leaveGroup(groupId, userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 탈퇴 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 탈퇴 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 멤버 목록 조회",
            description = "그룹의 모든 멤버 목록을 조회합니다. 해당 그룹의 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "그룹 멤버 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupMemberDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDTO>> getGroupMembers(
            @Parameter(description = "멤버 목록을 조회할 그룹의 ID", required = true)
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            List<GroupMemberDTO> members = groupService.getGroupMembers(groupId, userId);
            return ResponseEntity.ok(members);
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 조회 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 멤버 조회 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "멤버를 그룹 오너로 승격",
            description = "그룹 멤버를 그룹 오너로 승격시킵니다. 현재 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "멤버 승격 성공",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 멤버를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
            )
    })
    @PatchMapping("/{groupId}/members/{userId}/promote")
    public ResponseEntity<Map<String, String>> promoteMemberToOwner(
            @Parameter(description = "그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(description = "승격할 멤버의 사용자 ID", required = true)
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 멤버 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        try {
            groupService.promoteMemberToOwner(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "멤버가 그룹 오너로 승격되었습니다."));
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 승격 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 멤버 승격 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "그룹 멤버 제거",
            description = "그룹에서 멤버를 제거합니다. 그룹 오너만 실행할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "멤버 제거 성공",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터 또는 권한 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹 또는 멤버를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content
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

        try {
            groupService.removeMember(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 제거 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("그룹 멤버 제거 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}