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
import store.lastdance.dto.group.GroupApplicationResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.dto.group.GroupRequestDTO;
import store.lastdance.dto.group.GroupResponseDTO;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.service.user.UserService;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupApplicationRepository groupApplicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;

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

            return createGroupResponse(savedGroup);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 저장 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_CREATION_FAILED);
        }
    }
    // GroupResponseDTO 생성 메소드
    private GroupResponseDTO createGroupResponse(Group group) {
        return new GroupResponseDTO(
                group.getGroupId(),
                group.getGroupName(),
                group.getInviteCode(),
                group.getMaxMembers(),
                group.getGroupBudget(),
                group.getOwner().getUserId(),
                createGroupMemberDTOList(group.getMembers())
        );
    }

    // GroupMemberDTO 목록 생성 메소드
    private List<GroupMemberDTO> createGroupMemberDTOList(List<GroupMember> members) {
        return members.stream()
                .map(member -> new GroupMemberDTO(
                        member.getUser().getUserId(),
                        member.getUser().getNickname(),
                        member.getUser().getProfileImageFile() != null ?
                                member.getUser().getProfileImageFile().getFilePath() : null,
                        member.getRole()
                ))
                .toList();
    }

    // 사용자 반환 메소드
    private User getUserByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateGroupRequest(GroupRequestDTO request) {
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

        // 그룹 참여 신청 여부 확인
        validateGroupApplicationForApplyGroup(userId, group);

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
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }

        // 그룹 참여 신청 로직 (예: 알림 전송 등)
        log.info("그룹 참여 신청 완료 - 그룹 ID: {}, 사용자 ID: {}", group.getGroupId(), userId);
    }

    // 입력값 유효성 검사 메소드
    private void validateInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.length() != RANDOM_CODE_LENGTH || !inviteCode.matches("[" + RANDOM_CODE_CHARACTERS + "]+")) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE);
        }
    }

    // 초대 코드로 그룹 조회 메소드
    private Group getGroupByInviteCode(String inviteCode) {
        return groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    // 그룹 참여 신청 여부 확인 메소드
    private void validateGroupApplicationForApplyGroup(UUID userId, Group group) {
        // 이미 신청을 했는지 확인
        if (groupApplicationRepository.existsByGroupAndUser(group, getUserByUserId(userId))) {
            throw new CustomException(ErrorCode.ALREADY_APPLIED_GROUP);
        }
    }

    // 그룹 참여 가능 여부 확인 메소드
    private void validateGroupJoin(Group group, UUID userId) {

        // 이미 그룹에 참여 중인지 확인
        if (group.getMembers().stream().anyMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }

        // 그룹 최대 인원 초과 여부 확인
        if (group.getMembers().size() >= group.getMaxMembers()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
    }

    @Override
    public List<GroupApplicationResponseDTO> getGroupApplications(UUID groupId, UUID userId) {
        log.info("그룹 참여 신청 목록 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 소유자 확인
        validateGroupOwner(group, userId);

        // 그룹 참여 신청 목록 조회
        List<GroupApplication> applications = groupApplicationRepository.findByGroup(group);

        if (applications.isEmpty()) {
            log.info("그룹 {}의 참여 신청 목록이 비어 있습니다.", groupId);
            return List.of(); // 빈 리스트 반환
        }

        log.info("그룹 {}의 참여 신청 목록 조회 완료 - 신청 수: {}", groupId, applications.size());
        return applications.stream()
                .map(app -> new GroupApplicationResponseDTO(
                        app.getUser().getUserId(),
                        app.getGroup().getGroupId(),
                        app.getUser().getNickname(),
                        app.getUser().getEmail(),
                        app.getUser().getProfileImageFile() != null ?
                                app.getUser().getProfileImageFile().getFilePath() : null,
                        app.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public GroupResponseDTO acceptGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("그룹 참여 신청 수락 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        userService.validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User user = getUserByUserId(userId);

        // 대상 사용자가 그룹 참여 신청을 했는지 확인
        validateGroupApplicationForAccept(group, user);

        // 그룹 참여 가능 여부 확인
        validateGroupJoin(group, userId);

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

            return createGroupResponse(group);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 참여 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    // 대상 사용자가 그룹 참여 신청을 했는지 확인 메소드
    private void validateGroupApplicationForAccept(Group group, User user) {
        if (!groupApplicationRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.INVALID_GROUP_REQUEST);
        }
    }

    @Override
    @Transactional
    public void rejectGroupApplication(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("그룹 참여 신청 거절 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        userService.validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User user = getUserByUserId(userId);

        // 대상 사용자가 그룹 참여 신청을 했는지 확인
        validateGroupApplicationForAccept(group, user);

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

        if (groups != null && !groups.isEmpty()) {
            log.info("사용자 {}의 그룹 조회 완료 - 그룹 수: {}", userId, groups.size());
            return groups.stream()
                    .map(this::createGroupResponse)
                    .toList();
        }

        log.info("사용자 {}의 그룹 조회 완료 - 그룹 수: 0", userId);
        return List.of(); // 빈 리스트 반환
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

        return createGroupResponse(group);
    }

    // 그룹 조회 메서드
    @Override
    public Group getGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    // 그룹 멤버 여부 확인 메서드
    @Override
    public void isUserMemberOfGroup(UUID userId, Group group) {
        if (group.getMembers().stream().noneMatch(member -> member.getUser().getUserId().equals(userId))) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    @Override
    public GroupResponseDTO updateGroup(UUID groupId, GroupRequestDTO groupRequestDTO, UUID userId) {

        log.info("그룹 수정 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 소유자 확인
        validateGroupOwner(group, userId);

        // 입력값 검증
        validateGroupRequest(groupRequestDTO);

        // 그룹 정보 업데이트
        group.updateGroupDetails(groupRequestDTO.groupName(), groupRequestDTO.maxMembers(), groupRequestDTO.groupBudget());

        try {
            Group updatedGroup = groupRepository.save(group);
            log.info("그룹 수정 완료 - ID: {}, 이름: {}", updatedGroup.getGroupId(), updatedGroup.getGroupName());

            return createGroupResponse(updatedGroup);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 수정 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    // 그룹 소유자 확인 메소드
    private void validateGroupOwner(Group group, UUID userId) {

        if (!group.getOwner().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    @Override
    public void deleteGroup(UUID groupId, UUID userId) {

        log.info("그룹 삭제 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 소유자 확인
        validateGroupOwner(group, userId);

        // 그룹 삭제
        groupRepository.delete(group);
        log.info("그룹 삭제 완료 - ID: {}", groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        log.info("그룹 탈퇴 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 멤버 여부 확인
        isUserMemberOfGroup(userId, group);

        // 그룹 소유자일 경우 예외처리
        validateGroupOwnerForLeave(group, userId);

        // 그룹에서 멤버 제거
        groupMemberRepository.deleteByGroupAndUser(group, userRepository.findById(userId).orElseThrow());

        try {
            groupRepository.save(group);
            log.info("그룹 탈퇴 완료 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("그룹 탈퇴 중 데이터 무결성 오류", e);
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    // 그룹 소유자 일 경우 예외처리 메서드
    private void validateGroupOwnerForLeave(Group group, UUID userId) {
        if (group.getOwner().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
        }
    }

    @Override
    public List<GroupMemberDTO> getGroupMembers(UUID groupId, UUID userId) {

        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 사용자 ID: {}", groupId, userId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 사용자 존재 확인
        userService.validateUserExists(userId);

        // 그룹 멤버 여부 확인
        isUserMemberOfGroup(userId, group);

        // 그룹 멤버 목록 반환
        return createGroupMemberDTOList(group.getMembers());
    }

    @Override
    public void promoteMemberToOwner(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("멤버를 소유자로 승격 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        userService.validateUserExists(currentUserId);

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
            throw new CustomException(ErrorCode.GROUP_OPERATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID userId, UUID currentUserId) {

        log.info("멤버 제거 요청 - 그룹 ID: {}, 사용자 ID: {}, 현재 사용자 ID: {}", groupId, userId, currentUserId);

        // 그룹 조회
        Group group = getGroupById(groupId);

        // 현재 사용자 존재 확인
        userService.validateUserExists(currentUserId);

        // 그룹 소유자 확인
        validateGroupOwner(group, currentUserId);

        // 대상 사용자 존재 확인
        User targetUser = getUserByUserId(userId);

        // 대상 사용자가 그룹 멤버인지 확인
        isUserMemberOfGroup(userId, group);

        // 그룹 소유자일 경우 예외처리
        validateGroupOwnerForLeave(group, userId);

        // 그룹에서 멤버 제거
        groupMemberRepository.deleteByGroupAndUser(group, targetUser);

        log.info("멤버 제거 완료 - 그룹 ID: {}, 제거된 사용자 ID: {}", groupId, userId);
    }
}
