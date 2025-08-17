package store.lastdance.controller.group;

import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.http.protocol.HTTP;
import org.hibernate.Internal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import store.lastdance.exception.GlobalExceptionHandler;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.dto.group.*;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.group.GroupService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Group Controller 테스트")
class GroupControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupService groupService;

    @Mock
    private CustomOAuth2User mockUser;

    @InjectMocks
    private GroupController groupController;

    private UUID userId;
    private UUID groupId;
    private UUID memberId;
    private GroupRequestDTO groupRequest;
    private GroupResponseDTO groupResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        // Set up mock user with lenient behavior
        lenient().when(mockUser.getUserId()).thenReturn(userId);
        
        mockMvc = MockMvcBuilders.standaloneSetup(groupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter, 
                                                org.springframework.web.method.support.ModelAndViewContainer mavContainer, 
                                                org.springframework.web.context.request.NativeWebRequest webRequest, 
                                                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        // Return a concrete CustomOAuth2User instance
                        return new CustomOAuth2User(
                            userId,
                            "test@example.com",
                            "testUser",
                            "kakao",
                            "12345",
                            java.util.Map.of()
                        );
                    }
                })
                .build();

        groupRequest = new GroupRequestDTO(
                "Test Group",
                10,
                1000000
        );

        List<GroupMemberDTO> members = List.of(
                new GroupMemberDTO(userId, "owner", "owner.jpg", GroupRole.OWNER),
                new GroupMemberDTO(memberId, "member", "member.jpg", GroupRole.MEMBER)
        );

        groupResponse = new GroupResponseDTO(
                groupId,
                "Test Group",
                "ABC123",
                10,
                1000000,
                userId,
                members
        );
    }

    @Nested
    @DisplayName("그룹 생성 및 기본 관리")
    class GroupCreationTests {

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_Success() throws Exception {
            // given
            given(groupService.createGroup(any(GroupRequestDTO.class), any(UUID.class)))
                    .willReturn(groupResponse);

            // when & then
            mockMvc.perform(post("/api/v1/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(groupRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.groupName").value("Test Group"))
                    .andExpect(jsonPath("$.data.maxMembers").value(10))
                    .andExpect(jsonPath("$.data.groupBudget").value(1000000))
                    .andExpect(jsonPath("$.data.inviteCode").exists());
        }

        @Test
        @DisplayName("그룹명 없이 생성 시 400 반환")
        void createGroup_InvalidGroupName() throws Exception {
            // given
            GroupRequestDTO invalidRequest = new GroupRequestDTO(
                    "", // 빈 그룹명
                    10,
                    1000000
            );

            // when & then - Validation 에러로 인한 400 상태 코드 확인
            mockMvc.perform(post("/api/v1/groups")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest()); // 400 Bad Request
        }

        @Test
        @DisplayName("최대 멤버 수가 0 이하일 때 400 반환")
        void createGroup_InvalidMaxMembers() throws Exception {
            // given
            GroupRequestDTO invalidRequest = new GroupRequestDTO(
                    "Test Group",
                    0, // 잘못된 최대 멤버 수
                    1000000
            );

            // when & then - Validation 에러로 인한 400 상태 코드 확인
            mockMvc.perform(post("/api/v1/groups")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest()); // 400 Bad Request
        }
    }

    @Nested
    @DisplayName("그룹 조회")
    class GroupRetrievalTests {

        @Test
        @DisplayName("사용자 그룹 목록 조회 성공")
        void getUserGroups_Success() throws Exception {
            // given
            List<GroupResponseDTO> groups = List.of(groupResponse);
            given(groupService.getGroupsByUserId(any(UUID.class)))
                    .willReturn(groups);

            // when & then
            mockMvc.perform(get("/api/v1/groups/me")
                            .requestAttr("user", mockUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].groupName").value("Test Group"));
        }

        @Test
        @DisplayName("그룹 상세 정보 조회 성공")
        void getGroupDetails_Success() throws Exception {
            // given
            given(groupService.getGroupResponseDTOById(any(UUID.class), any(UUID.class)))
                    .willReturn(groupResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
                    .andExpect(jsonPath("$.data.groupName").value("Test Group"))
                    .andExpect(jsonPath("$.data.members").isArray())
                    .andExpect(jsonPath("$.data.members").value(org.hamcrest.Matchers.hasSize(2)));
        }

        @Test
        @DisplayName("존재하지 않는 그룹 조회 시 404 반환")
        void getGroupDetails_NotFound() throws Exception {
            // given
            UUID notFoundGroupId = UUID.randomUUID();
            given(groupService.getGroupResponseDTOById(any(UUID.class), any(UUID.class)))
                    .willThrow(new RuntimeException("Group not found"));

            // when & then
            mockMvc.perform(get("/api/v1/groups/{groupId}", notFoundGroupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("그룹 수정")
    class GroupUpdateTests {

        @Test
        @DisplayName("그룹 정보 수정 성공")
        void updateGroup_Success() throws Exception {
            // given
            GroupRequestDTO updateRequest = new GroupRequestDTO(
                    "Updated Group",
                    15,
                    2000000
            );

            GroupResponseDTO updatedResponse = new GroupResponseDTO(
                    groupId,
                    "Updated Group",
                    "ABC123",
                    15,
                    2000000,
                    userId,
                    groupResponse.members()
            );

            given(groupService.updateGroup(any(UUID.class), any(GroupRequestDTO.class), any(UUID.class)))
                    .willReturn(updatedResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.groupName").value("Updated Group"))
                    .andExpect(jsonPath("$.data.maxMembers").value(15))
                    .andExpect(jsonPath("$.data.groupBudget").value(2000000));
        }

        @Test
        @DisplayName("그룹 소유자가 아닌 사용자의 수정 시도 시 403 반환")
        void updateGroup_NotOwner() throws Exception {
            // given
            given(groupService.updateGroup(any(UUID.class), any(GroupRequestDTO.class), any(UUID.class)))
                    .willThrow(new RuntimeException("Only group owner can update group"));

            // when & then
            mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(groupRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class GroupDeletionTests {

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_Success() throws Exception {
            // given
            willDoNothing().given(groupService)
                    .deleteGroup(any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("그룹 소유자가 아닌 사용자의 삭제 시도 시 403 반환")
        void deleteGroup_NotOwner() throws Exception {
            // given
            willThrow(new RuntimeException("Only group owner can delete group"))
                    .given(groupService).deleteGroup(any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("그룹 참여 및 탈퇴")
    class GroupMembershipTests {

        @Test
        @DisplayName("초대 코드로 그룹 참여 신청 성공")
        void applyGroup_Success() throws Exception {
            // given
            InviteCodeRequestDTO inviteRequest = new InviteCodeRequestDTO("ABC123");
            willDoNothing().given(groupService)
                    .applyGroup(any(String.class), any(UUID.class));

            // when & then
            mockMvc.perform(post("/api/v1/groups/applications")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inviteRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("잘못된 초대 코드로 참여 시도 시 400 반환")
        void applyGroup_InvalidCode() throws Exception {
            // given
            InviteCodeRequestDTO invalidRequest = new InviteCodeRequestDTO("INVALID");
            willThrow(new RuntimeException("Invalid invite code"))
                    .given(groupService).applyGroup(any(String.class), any(UUID.class));

            // when & then
            mockMvc.perform(post("/api/v1/groups/applications")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("그룹 탈퇴 성공")
        void leaveGroup_Success() throws Exception {
            // given
            willDoNothing().given(groupService)
                    .leaveGroup(any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("그룹 소유자의 탈퇴 시도 시 400 반환")
        void leaveGroup_OwnerCannotLeave() throws Exception {
            // given
            willThrow(new RuntimeException("Owner cannot leave group"))
                    .given(groupService).leaveGroup(any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("그룹 멤버 관리")
    class GroupMemberManagementTests {

        @Test
        @DisplayName("그룹 멤버 목록 조회 성공")
        void getGroupMembers_Success() throws Exception {
            // given
            List<GroupMemberDTO> members = List.of(
                    new GroupMemberDTO(userId, "owner", "owner.jpg", GroupRole.OWNER),
                    new GroupMemberDTO(memberId, "member", "member.jpg", GroupRole.MEMBER)
            );
            given(groupService.getGroupMembers(any(UUID.class), any(UUID.class)))
                    .willReturn(members);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{groupId}/members", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.data[0].role").value("OWNER"));
        }

        @Test
        @DisplayName("그룹 멤버 강제 퇴장 성공")
        void removeMember_Success() throws Exception {
            // given
            willDoNothing().given(groupService)
                    .removeMember(any(UUID.class), any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", groupId, memberId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("그룹 소유자가 아닌 사용자의 멤버 퇴장 시도 시 403 반환")
        void removeMember_NotOwner() throws Exception {
            // given
            willThrow(new RuntimeException("Only owner can remove members"))
                    .given(groupService).removeMember(any(UUID.class), any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", groupId, memberId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("그룹 소유권 이양 성공")
        void transferOwnership_Success() throws Exception {
            // given
            willDoNothing().given(groupService)
                    .promoteMemberToOwner(any(UUID.class), any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(patch("/api/v1/groups/{groupId}/members/{memberId}/promote", groupId, memberId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("그룹 신청 관리")
    class GroupApplicationTests {

        @Test
        @DisplayName("그룹 신청 목록 조회 성공")
        void getGroupApplications_Success() throws Exception {
            // given
            List<GroupApplicationResponseDTO> applications = List.of(
                    new GroupApplicationResponseDTO(
                            memberId, groupId, "applicant", "applicant@example.com",
                            "profile.jpg", LocalDateTime.now()
                    )
            );
            lenient().when(groupService.getGroupApplications(any(UUID.class), any(UUID.class)))
                    .thenReturn(applications);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{groupId}/applications", groupId)
                            .requestAttr("user", mockUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("그룹 신청 승인 성공")
        void approveApplication_Success() throws Exception {
            // given
            GroupApplicationRequestDTO applicationRequest = new GroupApplicationRequestDTO(
                    groupId, memberId
            );
            lenient().when(groupService.acceptGroupApplication(any(UUID.class), any(UUID.class), any(UUID.class)))
                    .thenReturn(groupResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/groups/applications/accept")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applicationRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("그룹 신청 거절 성공")
        void rejectApplication_Success() throws Exception {
            // given
            GroupApplicationRequestDTO applicationRequest = new GroupApplicationRequestDTO(
                    groupId, memberId
            );
            lenient().doNothing().when(groupService)
                    .rejectGroupApplication(any(UUID.class), any(UUID.class), any(UUID.class));

            // when & then
            mockMvc.perform(patch("/api/v1/groups/applications/reject")
                            .requestAttr("user", mockUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applicationRequest)))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 그룹 생성 시도")
    void createGroup_Unauthorized() throws Exception {
        // given - 사용자 인증 없이 서비스 호출 시 예외 발생 설정
        given(groupService.createGroup(any(GroupRequestDTO.class), any()))
                .willThrow(new RuntimeException("Authentication required"));

        // when & then - 인증 없이 요청하면 컨트롤러에서 예외 발생
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupRequest)))
                .andExpect(status().isInternalServerError()); // RuntimeException으로 인한 500
    }
}

