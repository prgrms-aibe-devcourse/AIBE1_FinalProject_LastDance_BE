package store.lastdance.service.group;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupApplicationRepository groupApplicationRepository;

    private static final String RANDOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_CODE_LENGTH = 6;

    @Override
    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO groupRequestDTO, UUID userId) {
        log.info("그룹 생성 요청 - 사용자: {}, 그룹명: {}", userId, groupRequestDTO.groupName());

        // 입력값 검증
        validateGroupRequest(groupRequestDTO);

        // 사용자 조회
        User owner = getUserByUserId(userId);

        // 그룹 생성
        Group group = Group.builder()
                .groupName(groupRequestDTO.groupName())
                .inviteCode(generateUniqueInviteCode())
                .owner(owner)
                .maxMembers(groupRequestDTO.maxMembers())
                .groupBudget(groupRequestDTO.groupBudget())
                .build();

        // 소유자를 멤버로 추가
        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.OWNER)
                .build();

        group.addMember(ownerMember);

        try {
            Group savedGroup = groupRepository.save(group);
            log.info("그룹 생성 완료 - ID: {}, 이름: {}", savedGroup.getGroupId(), savedGroup.getGroupName());

            return new GroupResponseDTO(
                    savedGroup.getGroupId(),
                    savedGroup.getGroupName(),
                    savedGroup.getInviteCode(),
                    savedGroup.getMaxMembers(),
                    savedGroup.getGroupBudget(),
                    savedGroup.getOwner().getUserId(),
                    savedGroup.getMembers()
            );
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 저장 중 데이터 무결성 오류", e);
            throw new IllegalStateException("그룹 생성 중 오류가 발생했습니다.");
        }
    }

    // 사용자 반환 메소드
    private User getUserByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));
    }

    private void validateGroupRequest(GroupRequestDTO request) {
        if (request.groupName() == null || request.groupName().trim().isEmpty()) {
            throw new IllegalArgumentException("그룹명은 필수입니다.");
        }
        if (request.groupName().length() > 100) {
            throw new IllegalArgumentException("그룹명은 100자를 초과할 수 없습니다.");
        }
        if (request.maxMembers() != null && (request.maxMembers() < 1 || request.maxMembers() > 100)) {
            throw new IllegalArgumentException("최대 인원은 1명 이상 100명 이하여야 합니다.");
        }
        if (request.groupBudget() != null && request.groupBudget() < 0) {
            throw new IllegalArgumentException("그룹 예산은 0 이상이어야 합니다.");
        }
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

    @Override
    public void applyGroup(String inviteCode, UUID userId) {

        log.info("그룹 참여 신청 요청 - 초대 코드: {}, 사용자: {}", inviteCode, userId);

        // 입력값 유효성 검사
        validateInviteCode(inviteCode);

        // 초대 코드로 그룹 조회
        Group group = getGroupByInviteCode(inviteCode);

        // 사용자 조회
        User user = getUserByUserId(userId);

        // 그룹 참여 가능 여부 확인
        validateGroupJoin(group, userId);

        // 그룹 참여 신청 처리
        GroupApplication application = GroupApplication.builder()
                .group(group)
                .user(user)
                .build();

        try {
            // 그룹 참여 신청 저장
            groupApplicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 참여 신청 중 데이터 무결성 오류", e);
            throw new IllegalStateException("그룹 참여 신청 중 오류가 발생했습니다.");
        }

        // 그룹 참여 신청 로직 (예: 알림 전송 등)
        log.info("그룹 참여 신청 완료 - 그룹 ID: {}, 사용자 ID: {}", group.getGroupId(), userId);
    }

    // 입력값 유효성 검사 메소드
    private void validateInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.length() != RANDOM_CODE_LENGTH || !inviteCode.matches("[" + RANDOM_CODE_CHARACTERS + "]+")) {
            throw new IllegalArgumentException("유효하지 않은 초대 코드입니다.");
        }
    }

    // 초대 코드로 그룹 조회 메소드
    private Group getGroupByInviteCode(String inviteCode) {
        return groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NoSuchElementException("해당 초대 코드를 가진 그룹을 찾을 수 없습니다: " + inviteCode));
    }

    // 그룹 참여 가능 여부 확인 메소드
    private void validateGroupJoin(Group group, UUID userId) {
        // 이미 그룹에 참여 중인지 확인
        if (group.getMembers().stream().anyMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new IllegalArgumentException("이미 해당 그룹에 참여 중입니다.");
        }

        // 그룹 최대 인원 초과 여부 확인
        if (group.getMembers().size() >= group.getMaxMembers()) {
            throw new IllegalArgumentException("그룹의 최대 인원 수를 초과했습니다.");
        }
    }

    @Override
    public GroupResponseDTO acceptGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("그룹 참여 신청 수락 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User user = getUserByUserId(userId);

        // 대상 사용자가 그룹 참여 신청을 했는지 확인
        validateGroupApplication(group, user);

        // 새로운 멤버 생성
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupRole.MEMBER) // 기본 역할은 MEMBER로 설정
                .build();

        // 그룹에 멤버 추가
        group.addMember(newMember);

        try {
            groupRepository.save(group);
            log.info("그룹 참여 완료 - 그룹 ID: {}, 사용자 ID: {}", group.getGroupId(), userId);

            // 그룹 참여 신청 삭제
            groupApplicationRepository.deleteByGroupAndUser(group, user);

            return new GroupResponseDTO(
                    group.getGroupId(),
                    group.getGroupName(),
                    group.getInviteCode(),
                    group.getMaxMembers(),
                    group.getGroupBudget(),
                    group.getOwner().getUserId(),
                    group.getMembers()
            );
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 참여 중 데이터 무결성 오류", e);
            throw new IllegalStateException("그룹 참여 중 오류가 발생했습니다.");
        }
    }

    // 대상 사용자가 그룹 참여 신청을 했는지 확인 메서드
    private void validateGroupApplication(Group group, User user) {
        if (!groupApplicationRepository.existsByGroupAndUser(group, user)) {
            throw new IllegalArgumentException("해당 사용자가 그룹 참여 신청을 하지 않았습니다.");
        }
    }

    @Override
    public void rejectGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("그룹 참여 신청 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User user = getUserByUserId(userId);

        // 대상 사용자가 그룹 참여 신청을 했는지 확인
        validateGroupApplication(group, user);

        // 그룹 참여 신청 삭제
        groupApplicationRepository.deleteByGroupAndUser(group, user);
        log.info("그룹 참여 신청 거절 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
    }

    @Override
    public List<GroupResponseDTO> getGroupsByUserId(UUID userId) {

        log.info("사용자 그룹 조회 요청 - 사용자 ID: {}", userId);

        // 사용자 조회
        User user = getUserByUserId(userId);

        // 사용자 그룹 조회
        List<Group> groups = groupRepository.findByMembers_User(user);
        return List.of();
    }

    @Override
    public GroupResponseDTO getGroupById(UUID groupId, UUID userId) {

        log.info("그룹 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 조회
        User user = getUserByUserId(userId);

        // 그룹 멤버 여부 확인
        isUserMemberOfGroup(userId, group);

        return new GroupResponseDTO(
                group.getGroupId(),
                group.getGroupName(),
                group.getInviteCode(),
                group.getMaxMembers(),
                group.getGroupBudget(),
                group.getOwner().getUserId(),
                group.getMembers()
        );
    }

    // 그룹 조회 메서드
    private Group getGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다: " + groupId));
    }

    // 그룹 멤버 여부 확인 메서드
    private void isUserMemberOfGroup(UUID userId, Group group) {
        if (group.getMembers().stream().noneMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new IllegalArgumentException("해당 그룹에 참여하지 않은 사용자입니다.");
        }
    }

    @Override
    public GroupResponseDTO updateGroup(UUID groupId, GroupRequestDTO groupRequestDTO, UUID userId) {

        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        validateUserExists(userId);

        // 그룹 소유자 확인
        validateGroupOwner(group, userId);

        // 입력값 검증
        validateGroupRequest(groupRequestDTO);

        // 그룹 정보 업데이트
        group.updateGroupDetails(groupRequestDTO.groupName(), groupRequestDTO.maxMembers(), groupRequestDTO.groupBudget());

        try {
            Group updatedGroup = groupRepository.save(group);
            log.info("그룹 수정 완료 - ID: {}, 이름: {}", updatedGroup.getGroupId(), updatedGroup.getGroupName());

            return new GroupResponseDTO(
                    updatedGroup.getGroupId(),
                    updatedGroup.getGroupName(),
                    updatedGroup.getInviteCode(),
                    updatedGroup.getMaxMembers(),
                    updatedGroup.getGroupBudget(),
                    updatedGroup.getOwner().getUserId(),
                    updatedGroup.getMembers()
            );
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 수정 중 데이터 무결성 오류", e);
            throw new IllegalStateException("그룹 수정 중 오류가 발생했습니다.");
        }
    }

    // 사용자 존재 확인 메소드
    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId);
        }
    }

    // 그룹 소유자 확인 메소드
    private void validateGroupOwner(Group group, UUID userId) {

        if (!group.getOwner().getUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹 소유자만 그룹을 수정할 수 있습니다.");
        }
    }

    @Override
    public void deleteGroup(UUID groupId, UUID userId) {

        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        validateUserExists(userId);

        // 그룹 소유자 확인
        validateGroupOwner(group, userId);

        // 그룹 삭제
        groupRepository.delete(group);
        log.info("그룹 삭제 완료 - ID: {}", groupId);
    }

    @Override
    public void leaveGroup(UUID groupId, UUID userId) {
        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        validateUserExists(userId);

        // 그룹 멤버 여부 확인
        isUserMemberOfGroup(userId, group);

        // 그룹에서 멤버 제거
        group.getMembers().removeIf(member -> member.getUser().getUserId().equals(userId));

        try {
            groupRepository.save(group);
            log.info("그룹 탈퇴 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 탈퇴 중 데이터 무결성 오류", e);
            throw new IllegalStateException("그룹 탈퇴 중 오류가 발생했습니다.");
        }
    }

    @Override
    public List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId) {

        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        validateUserExists(userId);

        // 그룹 멤버 여부 확인
        isUserMemberOfGroup(userId, group);

        // 그룹 멤버 목록 반환
        return group.getMembers().stream()
                .map(member -> new GroupMemberDTO(
                        member.getUser().getUserId(),
                        member.getUser().getNickname(),
                        member.getUser().getProfileImageFile(),
                        member.getRole()
                ))
                .toList();
    }

    @Override
    public void promoteMemberToOwner(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("멤버를 소유자로 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User targetUser = getUserByUserId(userId);

        // 대상 사용자가 그룹 멤버인지 확인
        isUserMemberOfGroup(userId, group);

        // 현재 소유자 역할을 MEMBER로 변경
        group.updateGroupRole(userId, GroupRole.MEMBER);

        // 대상 사용자를 새로운 소유자로 설정
        group.updateGroupOwner(targetUser);

        try {
            groupRepository.save(group);
            log.info("멤버 승격 완료 - 그룹 ID: {}, 새 소유자 ID: {}", groupId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("멤버 승격 중 데이터 무결성 오류", e);
            throw new IllegalStateException("멤버 승격 중 오류가 발생했습니다.");
        }
    }

    @Override
    public void removeMember(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("멤버 제거 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User targetUser = getUserByUserId(userId);

        // 대상 사용자가 그룹 멤버인지 확인
        isUserMemberOfGroup(userId, group);

        // 그룹에서 멤버 제거
        group.getMembers().removeIf(member -> member.getUser().equals(targetUser));

        try {
            groupRepository.save(group);
            log.info("멤버 제거 완료 - 그룹 ID: {}, 제거된 사용자 ID: {}", groupId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("멤버 제거 중 데이터 무결성 오류", e);
            throw new IllegalStateException("멤버 제거 중 오류가 발생했습니다.");
        }
    }
}
