package store.lastdance.service.group;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.dto.group.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.group.GroupApplicationRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Group Service 테스트")
class GroupServiceTest {

    @InjectMocks
    private GroupServiceImpl groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupApplicationRepository groupApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChecklistRepository checklistRepository;

    private UUID userId;
    private UUID groupId;
    private UUID memberId;
    private User owner;
    private User member;
    private Group group;
    private GroupMember ownerMember;
    private GroupMember normalMember;
    private GroupRequestDTO groupRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        owner = User.builder()
                .email("owner@example.com")
                .username("owner")
                .nickname("owner")
                .provider(OAuthProvider.KAKAO)
                .providerId("owner123")
                .build();
        owner.setUserId(userId); // userId 명시적 설정

        member = User.builder()
                .email("member@example.com")
                .username("member") 
                .nickname("member")
                .provider(OAuthProvider.KAKAO)
                .providerId("member123")
                .build();
        member.setUserId(memberId); // userId 명시적 설정

        group = Group.builder()
                .groupName("Test Group")
                .inviteCode("ABC123")
                .owner(owner)
                .maxMembers(10)
                .groupBudget(1000000)
                .build();
        
        // 리플렉션을 사용하여 groupId 설정
        try {
            var field = Group.class.getDeclaredField("groupId");
            field.setAccessible(true);
            field.set(group, groupId);
        } catch (Exception e) {
            // 리플렉션 실패 시 무시
        }

        ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.OWNER)
                .build();

        normalMember = GroupMember.builder()
                .group(group)
                .user(member)
                .role(GroupRole.MEMBER)
                .build();

        groupRequest = new GroupRequestDTO(
                "New Group",
                15,
                2000000
        );
    }

    @Nested
    @DisplayName("그룹 생성")
    class GroupCreationTests {

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_Success() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(groupRepository.save(any(Group.class))).willReturn(group);

            // when
            GroupResponseDTO result = groupService.createGroup(groupRequest, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.groupName()).isEqualTo("Test Group");
            assertThat(result.ownerId()).isEqualTo(userId);
            verify(groupRepository).save(any(Group.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 그룹 생성 실패")
        void createGroup_UserNotFound() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(groupRequest, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("그룹 조회")
    class GroupRetrievalTests {

        @Test
        @DisplayName("사용자 그룹 목록 조회 성공")
        void getGroupsByUserId_Success() {
            // given
            List<Group> groups = List.of(group, group); // 2개 그룹
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(groupRepository.findByMembers_User(owner)).willReturn(groups);

            // when
            List<GroupResponseDTO> result = groupService.getGroupsByUserId(userId);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("그룹 상세 정보 조회 성공")
        void getGroupById_Success() {
            // given
            List<GroupMember> members = List.of(ownerMember, normalMember);
            
            // 그룹에 소유자 멤버와 일반 멤버 모두 추가
            group.addMember(ownerMember);
            group.addMember(normalMember);
            
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupMemberRepository.findByGroup(group)).willReturn(members);

            // when
            GroupResponseDTO result = groupService.getGroupResponseDTOById(groupId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.groupId()).isEqualTo(groupId);
            assertThat(result.members()).hasSize(2);
        }

        @Test
        @DisplayName("그룹 멤버가 아닌 사용자의 그룹 상세 조회 실패")
        void getGroupById_NotMember() {
            // given
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when & then
            assertThatThrownBy(() -> groupService.getGroupResponseDTOById(groupId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("그룹 수정")
    class GroupUpdateTests {

        @Test
        @DisplayName("그룹 정보 수정 성공")
        void updateGroup_Success() {
            // given
            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);
            
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupRepository.save(any(Group.class))).willReturn(group);
            given(groupMemberRepository.findByGroup(group)).willReturn(List.of(ownerMember, normalMember));

            // when
            GroupResponseDTO result = groupService.updateGroup(groupId, groupRequest, userId);

            // then
            assertThat(result).isNotNull();
            verify(groupRepository).save(any(Group.class));
        }

        @Test
        @DisplayName("그룹 소유자가 아닌 사용자의 수정 시도 실패")
        void updateGroup_NotOwner() {
            // given
            // 그룹에 일반 멤버 추가
            group.addMember(normalMember);
            
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, member)).willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() -> groupService.updateGroup(groupId, groupRequest, memberId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class GroupDeletionTests {

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_Success() {
            // given
            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);
            
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));

            // when
            groupService.deleteGroup(groupId, userId);

            // then
            verify(groupRepository).delete(group);
        }

        @Test
        @DisplayName("그룹 소유자가 아닌 사용자의 삭제 시도 실패")
        void deleteGroup_NotOwner() {
            // given
            // 그룹에 일반 멤버 추가
            group.addMember(normalMember);
            
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, member)).willReturn(Optional.of(normalMember));

            // when & then
            assertThatThrownBy(() -> groupService.deleteGroup(groupId, memberId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("그룹 참여 및 탈퇴")
    class GroupMembershipTests {

        @Test
        @DisplayName("그룹 탈퇴 성공")
        void leaveGroup_Success() {
            // given
            // 그룹에 멤버 추가 (탈퇴할 멤버)
            group.addMember(normalMember);
            
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when
            groupService.leaveGroup(groupId, memberId);

            // then
            verify(groupMemberRepository).deleteByGroupAndUser(group, member);
        }

        @Test
        @DisplayName("그룹 소유자의 그룹 탈퇴 시도 실패")
        void leaveGroup_OwnerCannotLeave() {
            // given
            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);
            
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when & then
            assertThatThrownBy(() -> groupService.leaveGroup(groupId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
        }
    }

    @Nested
    @DisplayName("그룹 멤버 관리")
    class GroupMemberManagementTests {

        @Test
        @DisplayName("그룹 멤버 목록 조회 성공")
        void getGroupMembers_Success() {
            // given
            // 그룹에 멤버들 추가
            group.addMember(ownerMember);
            group.addMember(normalMember);
            
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));

            // when
            List<GroupMemberDTO> result = groupService.getGroupMembers(groupId, userId);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("그룹 멤버 강제 퇴장 성공")
        void removeMember_Success() {
            // given
            // 그룹에 멤버들 추가
            group.addMember(ownerMember);
            group.addMember(normalMember);
            
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));

            // when
            groupService.removeMember(groupId, memberId, userId);

            // then
            verify(groupMemberRepository).deleteByGroupAndUser(group, member);
        }

        @Test
        @DisplayName("그룹 소유권 이양 성공")
        void promoteMemberToOwner_Success() {
            // given
            // 그룹에 멤버들 추가
            group.addMember(ownerMember);
            group.addMember(normalMember);
            
            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupMemberRepository.findByGroupAndUser(group, member)).willReturn(Optional.of(normalMember));
            given(groupRepository.save(any(Group.class))).willReturn(group);

            // when
            groupService.promoteMemberToOwner(groupId, memberId, userId);

            // then
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(ownerMember);
            verify(groupMemberRepository).save(normalMember);
        }
    }

    @Nested
    @DisplayName("그룹 신청 관리")
    class GroupApplicationTests {

        @Test
        @DisplayName("그룹 신청 목록 조회 성공")
        void getGroupApplications_Success() {
            // given
            GroupApplication application = GroupApplication.builder()
                    .group(group)
                    .user(member)
                    .build();
            List<GroupApplication> applications = List.of(application);

            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);

            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupApplicationRepository.findByGroup(group)).willReturn(applications);

            // when
            List<GroupApplicationResponseDTO> result = groupService.getGroupApplications(groupId, userId);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("그룹 신청 승인 성공")
        void acceptGroupApplication_Success() {
            // given
            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);

            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupApplicationRepository.existsByGroupAndUser(group, member)).willReturn(true);
            given(groupMemberRepository.countByGroup(group)).willReturn(5L);
            given(groupRepository.save(any(Group.class))).willReturn(group);

            // when
            GroupResponseDTO result = groupService.acceptGroupApplication(groupId, memberId, userId);

            // then
            assertThat(result).isNotNull();
            verify(groupRepository).save(any(Group.class));
            verify(groupApplicationRepository).deleteByGroupAndUser(group, member);
        }

        @Test
        @DisplayName("그룹 신청 거절 성공")
        void rejectGroupApplication_Success() {
            // given
            // 그룹에 소유자 멤버 추가
            group.addMember(ownerMember);

            given(userRepository.findById(userId)).willReturn(Optional.of(owner));
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroupAndUser(group, owner)).willReturn(Optional.of(ownerMember));
            given(groupApplicationRepository.existsByGroupAndUser(group, member)).willReturn(true);

            // when
            groupService.rejectGroupApplication(groupId, memberId, userId);

            // then
            verify(groupApplicationRepository).deleteByGroupAndUser(group, member);
        }
    }

    @Nested
    @DisplayName("그룹 신청")
    class GroupApplicationCreateTests {

        @Test
        @DisplayName("그룹 신청 생성 성공")
        void applyGroup_Success() {
            // given
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findByInviteCode("ABC123")).willReturn(Optional.of(group));
            given(groupApplicationRepository.existsByGroupAndUser(group, member)).willReturn(false);

            // when
            groupService.applyGroup("ABC123", memberId);

            // then
            verify(groupApplicationRepository).save(any(GroupApplication.class));
        }

        @Test
        @DisplayName("이미 그룹에 속한 사용자의 신청 시도 실패")
        void applyGroup_AlreadyMember() {
            // given
            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findByInviteCode("ABC123")).willReturn(Optional.of(group));
            
            // 그룹에 이미 속해있도록 설정
            group.addMember(normalMember);

            // when & then
            assertThatThrownBy(() -> groupService.applyGroup("ABC123", memberId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_GROUP_REQUEST);
        }

        @Test
        @DisplayName("이미 신청한 그룹에 재신청 시도 실패")
        void applyGroup_AlreadyApplied() {
            // given
            GroupApplication existingApplication = GroupApplication.builder()
                    .group(group)
                    .user(member)
                    .build();

            given(userRepository.findById(memberId)).willReturn(Optional.of(member));
            given(groupRepository.findByInviteCode("ABC123")).willReturn(Optional.of(group));
            given(groupApplicationRepository.existsByGroupAndUser(group, member)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> groupService.applyGroup("ABC123", memberId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED_GROUP);
        }
    }

    @Nested
    @DisplayName("데이터 검증")
    class ValidationTests {

        @Test
        @DisplayName("잘못된 그룹명으로 생성 시 예외 발생")
        void createGroup_InvalidGroupName() {
            // given
            GroupRequestDTO invalidRequest = new GroupRequestDTO(
                    "", // 빈 그룹명
                    10,
                    1000000
            );

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(invalidRequest, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_GROUP_REQUEST);
        }

        @Test
        @DisplayName("잘못된 최대 멤버 수로 생성 시 예외 발생")
        void createGroup_InvalidMaxMembers() {
            // given
            GroupRequestDTO invalidRequest = new GroupRequestDTO(
                    "Test Group",
                    0, // 잘못된 최대 멤버 수
                    1000000
            );

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(invalidRequest, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_GROUP_REQUEST);
        }

        @Test
        @DisplayName("음수 예산으로 생성 시 예외 발생")
        void createGroup_NegativeBudget() {
            // given
            GroupRequestDTO invalidRequest = new GroupRequestDTO(
                    "Test Group",
                    10,
                    -1000 // 음수 예산
            );

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(invalidRequest, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_GROUP_REQUEST);
        }
    }
}
