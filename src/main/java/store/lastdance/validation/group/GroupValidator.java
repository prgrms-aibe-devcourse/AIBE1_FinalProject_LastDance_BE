package store.lastdance.validation.group;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.service.user.UserV2QueryService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupValidator {

    private final GroupApplicationRepository groupApplicationRepository;
    private final UserV2QueryService userQueryService;

    public void validateGroupRequest(GroupRequestDTO request) {
        if (request.groupName() == null || request.groupName().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
        if (request.groupName().length() > 100) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
        if (request.maxMembers() != null && (request.maxMembers() < 1 || request.maxMembers() > 100)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
        if (request.groupBudget() != null && request.groupBudget() < 0) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
    }

    public void validateInviteCode(String inviteCode, int RANDOM_CODE_LENGTH, String RANDOM_CODE_CHARACTERS) {
        if (inviteCode == null || inviteCode.length() != RANDOM_CODE_LENGTH || !inviteCode.matches("[" + RANDOM_CODE_CHARACTERS + "]+")) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE);
        }
    }

    public void validateGroupApplicationForApplyGroup(UUID userId, Group group) {
        if (groupApplicationRepository.existsByGroupAndUser(group, userQueryService.findByUserId(userId))) {
            throw new CustomException(ErrorCode.ALREADY_APPLIED_GROUP);
        }
    }

    public void validateGroupJoin(Group group, UUID userId) {
        if (group.getMembers().stream().anyMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }

        if (group.getMembers().size() >= group.getMaxMembers()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
    }

    public void validateGroupApplicationForAccept(Group group, User user) {
        if (!groupApplicationRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
    }

    public void validateGroupRequestMaxMembers(Group group, GroupRequestDTO groupRequestDTO) {
        if (group.getMembers().size() > groupRequestDTO.maxMembers()) {
            throw new CustomException(ErrorCode.GROUP_MAX_MEMBERS_EXCEEDED);
        }
    }

    public void validateGroupOwner(Group group, UUID userId) {
        if (!group.getOwner().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    public void validateGroupOwnerForLeave(Group group, UUID userId) {
        if (group.getOwner().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
        }
    }

    public void validateUserMemberOfGroup(UUID userId, Group group) {
        if (group.getMembers().stream().noneMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

}
