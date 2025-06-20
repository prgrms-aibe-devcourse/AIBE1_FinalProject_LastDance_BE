package store.lastdance.dto.group;

import store.lastdance.domain.group.GroupMember;

import java.util.List;
import java.util.UUID;

public record GroupResponseDTO(
    UUID groupId,
    String groupName,
    String inviteCode,
    Integer maxMembers,
    Integer groupBudget,
    UUID ownerId,
    List<GroupMember> members
) {
}