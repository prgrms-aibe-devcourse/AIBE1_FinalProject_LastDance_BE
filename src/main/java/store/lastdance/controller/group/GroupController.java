package store.lastdance.controller.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.auth.CustomOAuth2User;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;
import store.lastdance.service.group.GroupService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(GroupRequestDTO groupRequestDTO, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 생성 요청 - 사용자 ID: {}", userId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.createGroup(groupRequestDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("사용자 조회 실패 - ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 생성 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 생성에 실패했습니다."));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyGroup(@RequestBody String inviteCode, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 참여 신청 요청 - 사용자 ID: {}, 초대 코드: {}", userId, inviteCode);

        try {
            groupService.applyGroup(inviteCode, userId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "그룹 참여 신청이 완료되었습니다."));
        } catch (NoSuchElementException e) {
            log.warn("그룹 조회 실패 - 초대 코드: {}", inviteCode, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", "해당 초대 코드를 가진 그룹을 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 참여 신청 실패 - 사용자 ID: {}, 초대 코드: {}", userId, inviteCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 참여 신청에 실패했습니다."));
        }
    }

    @PostMapping("/apply/accept")
    public ResponseEntity<?> acceptGroupApplication(@RequestBody UUID groupId, @RequestBody UUID userId, @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 참여 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.acceptGroupApplication(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.OK).body(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("그룹 참여 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_OR_USER_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 참여 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 참여에 실패했습니다."));
        }
    }

    @PostMapping("/apply/reject")
    public ResponseEntity<?> rejectGroupApplication(@RequestBody UUID groupId, @RequestBody UUID userId, @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 참여 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        try {
            groupService.rejectGroupApplication(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 참여 거절 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_OR_USER_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 참여 거절 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 참여 거절에 실패했습니다."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyGroup(@AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("사용자 그룹 조회 요청 - 사용자 ID: {}", userId);

        try {
            List<GroupResponseDTO> groups = groupService.getGroupsByUserId(userId);
            return ResponseEntity.ok(groups);
        } catch (NoSuchElementException e) {
            log.warn("사용자 그룹 조회 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("사용자 그룹 조회 실패 - 사용자 ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 조회에 실패했습니다."));
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable UUID groupId, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            GroupResponseDTO groupResponseDTO = groupService.getGroupById(groupId, userId);
            return ResponseEntity.ok(groupResponseDTO);
        } catch (NoSuchElementException e) {
            log.warn("그룹 조회 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 조회 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 조회에 실패했습니다."));
        }
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable UUID groupId, @RequestBody GroupRequestDTO groupRequestDTO, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            GroupResponseDTO updatedGroup = groupService.updateGroup(groupId, groupRequestDTO, userId);
            return ResponseEntity.ok(updatedGroup);
        } catch (NoSuchElementException e) {
            log.warn("그룹 수정 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 수정 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 수정에 실패했습니다."));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable UUID groupId, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            groupService.deleteGroup(groupId, userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 삭제 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 삭제 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 삭제에 실패했습니다."));
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable UUID groupId, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            groupService.leaveGroup(groupId, userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 탈퇴 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 탈퇴 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 탈퇴에 실패했습니다."));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable UUID groupId, @AuthenticationPrincipal CustomOAuth2User user) {

        UUID userId = user.getUserId();

        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        try {
            List<GroupMemberDTO> members = groupService.getGroupMembers(groupId, userId);
            return ResponseEntity.ok(members);
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 조회 실패 - 그룹 ID: {}", groupId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 멤버 조회 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 그룹 멤버 조회에 실패했습니다."));
        }
    }

    @PatchMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> promoteMemberToOwner(@PathVariable UUID groupId, @PathVariable UUID userId, @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 멤버 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        try {
            groupService.promoteMemberToOwner(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "멤버가 그룹 오너로 승격되었습니다."));
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 승격 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_OR_MEMBER_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 멤버 승격 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 멤버 승격에 실패했습니다."));
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable UUID groupId, @PathVariable UUID userId, @AuthenticationPrincipal CustomOAuth2User currentUser) {

        UUID currentUserId = currentUser.getUserId();

        log.info("그룹 멤버 제거 요청 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId);

        try {
            groupService.removeMember(groupId, userId, currentUserId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchElementException e) {
            log.warn("그룹 멤버 제거 실패 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "GROUP_OR_MEMBER_NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("그룹 멤버 제거 실패 - 그룹 ID: {}, 사용자 ID: {}, 요청자 ID: {}", groupId, userId, currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", "서버 오류로 멤버 제거에 실패했습니다."));
        }
    }
}
