package store.lastdance.service.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.group.GroupConverter;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.user.UserService;
import store.lastdance.service.user.UserV2QueryService;
import store.lastdance.validation.group.GroupValidator;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupV2QueryServiceImpl implements GroupV2QueryService {

    private final GroupRepository groupRepository;
    private final GroupApplicationRepository groupApplicationRepository;
    private final UserService userService;
    private final GroupConverter groupConverter;
    private final GroupValidator groupValidator;
    private final UserV2QueryService userQueryService;

    @Override
    public List<GroupApplicationResponseDTO> getGroupApplications(UUID groupId, UUID userId) {
        log.info("그룹 참여 신청 목록 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        groupValidator.validateGroupOwner(group, userId);

        List<GroupApplication> applications = groupApplicationRepository.findByGroup(group);

        if (applications.isEmpty()) {
            log.info("그룹 {}의 참여 신청 목록이 비어 있습니다.", groupId);
            return List.of();
        }

        log.info("그룹 {}의 참여 신청 목록 조회 완료 - 신청 수: {}", groupId, applications.size());
        return groupConverter.toGroupApplicationResponseDTOList(applications);
    }

    @Override
    public List<GroupResponseDTO> getGroupsByUserId(UUID userId) {
        log.info("사용자 그룹 조회 요청 - 사용자 ID: {}", userId);

        User user = userQueryService.findByUserId(userId);

        List<Group> groups = groupRepository.findByMembers_User(user);

        if (groups != null && !groups.isEmpty()) {
            log.info("사용자 {}의 그룹 조회 완료 - 그룹 수: {}", userId, groups.size());
            return groupConverter.toGroupResponseDTOList(groups);
        }

        log.info("사용자 {}의 그룹 조회 완료 - 그룹 수: 0", userId);
        return List.of();
    }

    @Override
    public GroupResponseDTO getGroupResponseDTOById(UUID groupId, UUID userId) {
        log.info("그룹 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        return groupConverter.toGroupResponseDTO(group);
    }

    @Override
    public Group getGroupWithValidation(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        groupValidator.validateUserMemberOfGroup(userId, group);

        return group;
    }

    @Override
    public List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId) {
        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        return groupConverter.toGroupMemberDTOList(group.getMembers());
    }

    @Override
    public Group getGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    @Override
    public Group getGroupByInviteCode(String inviteCode) {
        return groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return groupRepository.existsByInviteCode(inviteCode);
    }

    @Override
    public List<Group> getGroupsByUser(User user) {
        return groupRepository.findByMembers_User(user);
    }

    @Override
    public boolean isGroupApplicationExists(Group group, User user) {
        return groupApplicationRepository.existsByGroupAndUser(group, user);
    }

    @Override
    public boolean isUserGroupMember(UUID userId, Group group) {
        return group.getMembers().stream()
                .anyMatch(member -> member.getUser().getUserId().equals(userId));
    }

    @Override
    public boolean isGroupOwner(Group group, UUID userId) {
        return group.getOwner().getUserId().equals(userId);
    }

}