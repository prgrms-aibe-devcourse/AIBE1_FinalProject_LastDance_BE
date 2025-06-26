package store.lastdance.service.group;

import store.lastdance.domain.group.Group;
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;

import java.util.List;
import java.util.UUID;

public interface GroupService {

    GroupResponseDTO createGroup(GroupRequestDTO groupRequestDTO, UUID userId);

    void applyGroup(String inviteCode, UUID userId);

    List<GroupApplicationResponseDTO> getGroupApplications(UUID groupId, UUID userId);

    GroupResponseDTO acceptGroupApplication(UUID groupId, UUID userId, UUID currentUserId);

    void rejectGroupApplication(UUID groupId, UUID userId, UUID currentUserId);

    List<GroupResponseDTO> getGroupsByUserId(UUID userId);

    GroupResponseDTO getGroupById(UUID groupId, UUID userId);

    // 그룹 조회 메서드
    Group getGroupById(UUID groupId);

    // 그룹 멤버 여부 확인 메서드
    void isUserMemberOfGroup(UUID userId, Group group);

    GroupResponseDTO updateGroup(UUID groupId, GroupRequestDTO groupRequestDTO, UUID userId);

    // 사용자 존재 확인 메소드
    void validateUserExists(UUID userId);

    void deleteGroup(UUID groupId, UUID userId);

    void leaveGroup(UUID groupId, UUID userId);

    List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId);

    void promoteMemberToOwner(UUID groupId, UUID userId, UUID currentUserId);

    void removeMember(UUID groupId, UUID userId, UUID currentUserId);
}
