package store.lastdance.service.group;

import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupResponseDTO;

import java.util.List;
import java.util.UUID;

public interface GroupV2QueryService {

    List<GroupApplicationResponseDTO> getGroupApplications(UUID groupId, UUID userId);

    List<GroupResponseDTO> getGroupsByUserId(UUID userId);

    GroupResponseDTO getGroupResponseDTOById(UUID groupId, UUID userId);

    Group getGroupWithValidation(UUID groupId, UUID userId);

    List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId);

    Group getGroupById(UUID groupId);

    Group getGroupByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<Group> getGroupsByUser(User user);

}
