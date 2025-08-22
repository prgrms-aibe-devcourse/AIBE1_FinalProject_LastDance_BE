package store.lastdance.service.group;

import store.lastdance.domain.group.Group;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;

import java.util.UUID;

public interface GroupV2Service {

    /**
     * 새로운 그룹을 생성합니다.
     */
    GroupResponseDTO createGroup(GroupRequestDTO groupRequestDTO, UUID userId);

    /**
     * 초대 코드를 통해 그룹 참여를 신청합니다.
     */
    void applyGroup(String inviteCode, UUID userId);

    /**
     * 그룹 참여 신청을 승인합니다.
     */
    GroupResponseDTO acceptGroupApplication(UUID groupId, UUID userId, UUID currentUserId);

    /**
     * 그룹 참여 신청을 거절합니다.
     */
    void rejectGroupApplication(UUID groupId, UUID userId, UUID currentUserId);

    /**
     * 그룹 정보를 수정합니다.
     */
    GroupResponseDTO updateGroup(UUID groupId, GroupRequestDTO groupRequestDTO, UUID userId);

    /**
     * 그룹을 삭제합니다.
     */
    void deleteGroup(UUID groupId, UUID userId);

    /**
     * 그룹에서 탈퇴합니다.
     */
    void leaveGroup(UUID groupId, UUID userId);

    /**
     * 멤버를 그룹 소유자로 승격시킵니다.
     */
    void promoteMemberToOwner(UUID groupId, UUID userId, UUID currentUserId);

    /**
     * 그룹에서 멤버를 제거합니다.
     */
    void removeMember(UUID groupId, UUID userId, UUID currentUserId);

    /**
     * 그룹에서 사용자를 제거합니다. (내부 사용)
     */
    Group removeUserFromGroup(UUID groupId, UUID userId);

}
