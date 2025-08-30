package store.lastdance.service.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.group.GroupConverter;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.aijudgment.AiJudgmentRepository;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.game.GameResultRepository;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.user.UserService;
import store.lastdance.service.user.UserV2QueryService;
import store.lastdance.validation.group.GroupValidator;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupV2ServiceImpl implements GroupV2Service {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupApplicationRepository groupApplicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final ChecklistRepository checklistRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final CalendarRepository calendarRepository;
    private final GameResultRepository gameResultRepository;
    private final AiJudgmentRepository aiJudgmentRepository;
    private final GroupConverter groupConverter;
    private final GroupV2QueryService groupQueryService;
    private final UserV2QueryService userQueryService;
    private final GroupValidator groupValidator;

    private static final String RANDOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_CODE_LENGTH = 6;

    @Override
    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO groupRequestDTO, UUID userId) {
        log.info("그룹 생성 요청 - 사용자: {}, 그룹명: {}", userId, groupRequestDTO.groupName());

        groupValidator.validateGroupRequest(groupRequestDTO);

        User owner = userQueryService.findByUserId(userId);

        Group group = Group.builder()
                .groupName(groupRequestDTO.groupName())
                .inviteCode(generateUniqueInviteCode())
                .owner(owner)
                .maxMembers(groupRequestDTO.maxMembers())
                .groupBudget(groupRequestDTO.groupBudget())
                .build();

        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.OWNER)
                .build();

        group.addMember(ownerMember);

        try {
            Group savedGroup = groupRepository.save(group);
            log.info("그룹 생성 완료 - ID: {}, 이름: {}", savedGroup.getGroupId(), savedGroup.getGroupName());

            return groupConverter.toGroupResponseDTO(savedGroup);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 저장 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_CREATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void applyGroup(String inviteCode, UUID userId) {
        log.info("그룹 참여 신청 요청 - 초대 코드: {}, 사용자: {}", inviteCode, userId);

        groupValidator.validateInviteCode(inviteCode, RANDOM_CODE_LENGTH, RANDOM_CODE_CHARACTERS);

        Group group = groupQueryService.getGroupByInviteCode(inviteCode);

        User user = userQueryService.findByUserId(userId);

        groupValidator.validateGroupApplicationForApplyGroup(userId, group);

        groupValidator.validateGroupJoin(group, userId);

        GroupApplication application = GroupApplication.builder()
                .group(group)
                .user(user)
                .build();

        try {
            groupApplicationRepository.save(application);
            log.info("그룹 참여 신청 완료 - 그룹 ID: {}, 사용자 ID: {}", group.getGroupId(), userId);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 참여 신청 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public GroupResponseDTO acceptGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {
        log.info("그룹 참여 신청 수락 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        Group group = groupQueryService.getGroupWithValidation(groupId, currentUserId);

        userService.validateUserExists(currentUserId);

        groupValidator.validateGroupOwner(group, currentUserId);

        User user = userQueryService.findByUserId(userId);

        groupValidator.validateGroupApplicationForAccept(group, user);

        groupValidator.validateGroupJoin(group, userId);


        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupRole.MEMBER)
                .build();

        group.addMember(newMember);

        try {
            groupRepository.save(group);
            log.info("그룹 참여 완료 - 그룹 ID: {}, 사용자 ID: {}", group.getGroupId(), userId);

            groupApplicationRepository.deleteByGroupAndUser(group, user);

            return groupConverter.toGroupResponseDTO(group);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 참여 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void rejectGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {
        log.info("그룹 참여 신청 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        Group group = groupQueryService.getGroupWithValidation(groupId, currentUserId);

        userService.validateUserExists(currentUserId);

        groupValidator.validateGroupOwner(group, currentUserId);

        User user = userQueryService.findByUserId(userId);

        groupValidator.validateGroupApplicationForAccept(group, user);

        groupApplicationRepository.deleteByGroupAndUser(group, user);
        log.info("그룹 참여 신청 거절 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
    }

    @Override
    @Transactional
    public GroupResponseDTO updateGroup(UUID groupId, GroupRequestDTO groupRequestDTO, UUID userId) {
        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = groupQueryService.getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        groupValidator.validateGroupOwner(group, userId);

        groupValidator.validateGroupRequest(groupRequestDTO);

        groupValidator.validateGroupRequestMaxMembers(group, groupRequestDTO);

        group.updateGroupDetails(groupRequestDTO.groupName(), groupRequestDTO.maxMembers(), groupRequestDTO.groupBudget());

        try {
            Group updatedGroup = groupRepository.save(group);
            log.info("그룹 수정 완료 - ID: {}, 이름: {}", updatedGroup.getGroupId(), updatedGroup.getGroupName());

            return groupConverter.toGroupResponseDTO(updatedGroup);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 수정 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = groupQueryService.getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        groupValidator.validateGroupOwner(group, userId);

        cascadeDeleteGroupRelatedData(group);

        groupRepository.delete(group);
        log.info("그룹 삭제 완료 - ID: {}", groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        removeUserFromGroup(groupId, userId);

        log.info("그룹 탈퇴 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
    }

    @Override
    @Transactional
    public void promoteMemberToOwner(UUID groupId, UUID userId, UUID currentUserId) {
        log.info("멤버를 소유자로 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        Group group = groupQueryService.getGroupWithValidation(groupId, currentUserId);

        userService.validateUserExists(currentUserId);

        groupValidator.validateGroupOwner(group, currentUserId);

        User targetUser = userQueryService.findByUserId(userId);

        groupValidator.validateUserMemberOfGroup(userId, group);

        updateGroupRole(currentUserId, groupId, GroupRole.MEMBER);

        updateGroupOwner(group, targetUser);

        try {
            groupRepository.save(group);
            log.info("멤버 승격 완료 - 그룹 ID: {}, 새 소유자 ID: {}", groupId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("멤버 승격 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID userId, UUID currentUserId) {
        log.info("멤버 제거 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        Group group = groupQueryService.getGroupWithValidation(groupId, currentUserId);

        userService.validateUserExists(currentUserId);

        groupValidator.validateGroupOwner(group, currentUserId);

        removeUserFromGroup(groupId, userId);

        log.info("멤버 제거 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
    }

    public Group removeUserFromGroup(UUID groupId, UUID userId) {
        log.info("그룹에서 사용자 제거 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        Group group = groupQueryService.getGroupWithValidation(groupId, userId);

        userService.validateUserExists(userId);

        groupValidator.validateUserMemberOfGroup(userId, group);

        groupValidator.validateGroupOwnerForLeave(group, userId);

        groupMemberRepository.deleteByGroupAndUser(group, userRepository.findById(userId).orElseThrow());

        checklistRepository.deleteByGroupAndAssignee(group, userRepository.findById(userId).orElseThrow());

        log.info("그룹에서 사용자 제거 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        return group;
    }

    private String generateUniqueInviteCode() {
        String code = generateRandomCode();
        while (groupRepository.existsByInviteCode(code)) {
            code = generateRandomCode();
        }
        return code;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();
        String characters = RANDOM_CODE_CHARACTERS;
        for (int i = 0; i < RANDOM_CODE_LENGTH; i++) {
            int index = (int) (Math.random() * characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }

    private void updateGroupRole(UUID userId, UUID groupId, GroupRole groupRole) {
        log.info("그룹 멤버 역할 업데이트 요청 - 사용자 ID: {}, 그룹 ID: {}, 새 역할: {}", userId, groupId, groupRole);

        Group group = groupQueryService.getGroupWithValidation(groupId, userId);
        User user = userQueryService.findByUserId(userId);

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        member.changeRole(groupRole);

        try {
            groupMemberRepository.save(member);
            log.info("그룹 멤버 역할 업데이트 완료 - 사용자 ID: {}, 새 역할: {}", userId, groupRole);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 멤버 역할 업데이트 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    private void updateGroupOwner(Group group, User targetUser) {
        group.changeOwner(targetUser);
        UUID targetUserId = targetUser.getUserId();
        updateGroupRole(targetUserId, group.getGroupId(), GroupRole.OWNER);
    }

    private void cascadeDeleteGroupRelatedData(Group group) {
        log.info("그룹 관련 데이터 cascade 삭제 시작 - 그룹 ID: {}", group.getGroupId());

        try {
            groupApplicationRepository.deleteByGroup(group);
            log.debug("그룹 신청 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            checklistRepository.deleteByGroup(group);
            log.debug("체크리스트 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            expenseSplitRepository.deleteByGroupId(group.getGroupId());
            log.debug("지출 분담 정보 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            expenseRepository.deleteByGroup(group);
            log.debug("지출 내역 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            calendarRepository.deleteByGroupId(group.getGroupId());
            log.debug("캘린더 일정 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            gameResultRepository.deleteByGroup(group);
            log.debug("게임 결과 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            aiJudgmentRepository.deleteByGroupId(group.getGroupId());
            log.debug("AI 판단 기록 삭제 완료 - 그룹 ID: {}", group.getGroupId());

            log.info("그룹 관련 데이터 cascade 삭제 완료 - 그룹 ID: {}", group.getGroupId());
        } catch (Exception e) {
            log.error("그룹 관련 데이터 삭제 중 오류 발생 - 그룹 ID: {}", group.getGroupId(), e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }
}