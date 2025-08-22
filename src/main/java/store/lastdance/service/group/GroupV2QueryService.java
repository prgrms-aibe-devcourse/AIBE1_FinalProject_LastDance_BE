package store.lastdance.service.group;

import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupResponseDTO;

import java.util.List;
import java.util.UUID;

public interface GroupV2QueryService {

    /**
     * 그룹의 참여 신청 목록을 조회합니다.
     */
    List<GroupApplicationResponseDTO> getGroupApplications(UUID groupId, UUID userId);

    /**
     * 사용자가 속한 그룹 목록을 조회합니다.
     */
    List<GroupResponseDTO> getGroupsByUserId(UUID userId);

    /**
     * 그룹 정보를 조회합니다.
     */
    GroupResponseDTO getGroupResponseDTOById(UUID groupId, UUID userId);

    /**
     * 그룹을 조회하고 사용자의 멤버십을 검증합니다.
     */
    Group getGroupWithValidation(UUID groupId, UUID userId);

    /**
     * 그룹의 멤버 목록을 조회합니다.
     */
    List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId);

    /**
     * 그룹을 ID로 조회합니다. (검증 없음)
     */
    Group getGroupById(UUID groupId);

    /**
     * 초대 코드로 그룹을 조회합니다.
     */
    Group getGroupByInviteCode(String inviteCode);

    /**
     * 초대 코드가 존재하는지 확인합니다.
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * 사용자가 속한 그룹들을 조회합니다.
     */
    List<Group> getGroupsByUser(User user);

    /**
     * 그룹 참여 신청이 존재하는지 확인합니다.
     */
    boolean isGroupApplicationExists(Group group, User user);

    /**
     * 사용자가 그룹 멤버인지 확인합니다.
     */
    boolean isUserGroupMember(UUID userId, Group group);

    /**
     * 사용자가 그룹 소유자인지 확인합니다.
     */
    boolean isGroupOwner(Group group, UUID userId);

}
