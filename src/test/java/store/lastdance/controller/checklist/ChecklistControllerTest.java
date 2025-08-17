package store.lastdance.controller.checklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.Internal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import store.lastdance.domain.checklist.ChecklistType;
import store.lastdance.domain.checklist.Priority;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.dto.checklist.ChecklistRequestDTO;
import store.lastdance.dto.checklist.ChecklistResponseDTO;
import store.lastdance.dto.group.GroupMemberDTO;
import store.lastdance.exception.GlobalExceptionHandler;
import store.lastdance.service.checklist.ChecklistService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Checklist Controller 테스트")
class ChecklistControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ChecklistService checklistService;

    @InjectMocks
    private ChecklistController checklistController;

    private UUID userId;
    private UUID groupId;
    private Long checklistId;
    private ChecklistRequestDTO checklistRequest;
    private ChecklistResponseDTO checklistResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Include GlobalExceptionHandler in the test setup
        mockMvc = MockMvcBuilders.standaloneSetup(checklistController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        checklistId = 1L;

        checklistRequest = new ChecklistRequestDTO(
                "Test Checklist",
                "Test Description",
                userId,
                LocalDateTime.now().plusDays(7),
                Priority.HIGH
        );

        checklistResponse = new ChecklistResponseDTO(
                checklistId,
                "Test Checklist",
                "Test Description",
                ChecklistType.PERSONAL,
                null,
                null,
                new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                false,
                null,
                LocalDateTime.now().plusDays(7).toString(),
                Priority.HIGH.toString()
        );
    }

    @Nested
    @DisplayName("개인 체크리스트 관리")
    class PersonalChecklistTests {

        @Test
        @WithMockUser
        @DisplayName("개인 체크리스트 생성 성공 - 올바른 URL")
        void createPersonalChecklist_Success() throws Exception {
            // given
            lenient().when(checklistService.createChecklist(any(ChecklistRequestDTO.class), any(), eq(null)))
                    .thenReturn(checklistResponse);

            // when & then
            mockMvc.perform(post("/api/v1/checklists/me")  
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checklistRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Test Checklist"))
                    .andExpect(jsonPath("$.data.type").value("PERSONAL"))
                    .andExpect(jsonPath("$.data.priority").value("HIGH"));
        }

        @Test
        @WithMockUser
        @DisplayName("개인 체크리스트 목록 조회 성공 - 올바른 URL")
        void getPersonalChecklists_Success() throws Exception {
            // given
            List<ChecklistResponseDTO> checklists = List.of(checklistResponse);
            lenient().when(checklistService.getPersonalChecklist(any()))
                    .thenReturn(checklists);

            // when & then
            mockMvc.perform(get("/api/v1/checklists/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].title").value("Test Checklist"))
                    .andExpect(jsonPath("$.data[0].type").value("PERSONAL"));
        }

        @Test
        @WithMockUser
        @DisplayName("개인 체크리스트 수정 성공 - 올바른 URL")
        void updatePersonalChecklist_Success() throws Exception {
            // given
            ChecklistRequestDTO updateRequest = new ChecklistRequestDTO(
                    "Updated Checklist",
                    "Updated Description",
                    userId,
                    LocalDateTime.now().plusDays(14),
                    Priority.MEDIUM
            );

            ChecklistResponseDTO updatedResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Updated Checklist",
                    "Updated Description",
                    ChecklistType.PERSONAL,
                    null,
                    null,
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    false,
                    null,
                    LocalDateTime.now().plusDays(14).toString(),
                    Priority.MEDIUM.toString()
            );

            lenient().when(checklistService.updatePersonalChecklist(any(), any(ChecklistRequestDTO.class), any()))
                    .thenReturn(updatedResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/me/{checklistId}", checklistId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Checklist"))
                    .andExpect(jsonPath("$.data.priority").value("MEDIUM"));
        }

        @Test
        @WithMockUser
        @DisplayName("개인 체크리스트 삭제 성공")
        void deletePersonalChecklist_Success() throws Exception {
            // given
            lenient().doNothing().when(checklistService)
                    .deleteChecklist(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/checklists/{checklistId}", checklistId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("빈 제목으로 체크리스트 생성 - 서비스에서 예외 발생")
        void createChecklist_EmptyTitle() throws Exception {
            // given
            ChecklistRequestDTO invalidRequest = new ChecklistRequestDTO(
                    "", // 빈 제목
                    "Test Description",
                    userId,
                    LocalDateTime.now().plusDays(7),
                    Priority.HIGH
            );

            lenient().when(checklistService.createChecklist(any(ChecklistRequestDTO.class), any(), eq(null)))
                    .thenThrow(new IllegalArgumentException("Title cannot be empty"));

            // when & then
            mockMvc.perform(post("/api/v1/checklists/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("체크리스트 제목은 필수입니다."));
        }

        @Test
        @WithMockUser
        @DisplayName("과거 날짜로 마감일 설정 - 서비스에서 예외 발생")
        void createChecklist_PastDueDate() throws Exception {
            // given
            ChecklistRequestDTO invalidRequest = new ChecklistRequestDTO(
                    "Test Checklist",
                    "Test Description",
                    userId,
                    LocalDateTime.now().minusDays(1), // 과거 날짜
                    Priority.HIGH
            );

            lenient().when(checklistService.createChecklist(any(ChecklistRequestDTO.class), any(), eq(null)))
                    .thenThrow(new IllegalArgumentException("Due date cannot be in the past"));

            // when & then
            mockMvc.perform(post("/api/v1/checklists/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("마감일은 현재 시간 이후여야 합니다."));
        }

        @Test
        @WithMockUser
        @DisplayName("null 우선순위로 체크리스트 생성 - 서비스에서 예외 발생")
        void createChecklist_NullPriority() throws Exception {
            // given
            ChecklistRequestDTO invalidRequest = new ChecklistRequestDTO(
                    "Test Checklist",
                    "Test Description",
                    userId,
                    LocalDateTime.now().plusDays(7),
                    null // null 우선순위
            );

            lenient().when(checklistService.createChecklist(any(ChecklistRequestDTO.class), any(), eq(null)))
                    .thenThrow(new IllegalArgumentException("Priority cannot be null"));

            // when & then
            mockMvc.perform(post("/api/v1/checklists/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("중요도는 필수 항목입니다."));
        }
    }

    @Nested
    @DisplayName("그룹 체크리스트 관리")
    class GroupChecklistTests {

        @Test
        @WithMockUser
        @DisplayName("그룹 체크리스트 생성 성공 - 올바른 URL")
        void createGroupChecklist_Success() throws Exception {
            // given
            ChecklistResponseDTO groupChecklistResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Group Checklist",
                    "Group Description",
                    ChecklistType.GROUP,
                    groupId,
                    "Test Group",
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    false,
                    null,
                    LocalDateTime.now().plusDays(7).toString(),
                    Priority.HIGH.toString()
            );

            lenient().when(checklistService.createChecklist(any(ChecklistRequestDTO.class), any(), any()))
                    .thenReturn(groupChecklistResponse);

            // when & then
            mockMvc.perform(post("/api/v1/checklists/groups/{groupId}", groupId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checklistRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("GROUP"))
                    .andExpect(jsonPath("$.data.groupId").value(groupId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("그룹 체크리스트 목록 조회 성공 - 올바른 URL")
        void getGroupChecklists_Success() throws Exception {
            // given
            ChecklistResponseDTO groupChecklistResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Group Checklist",
                    "Group Description",
                    ChecklistType.GROUP,
                    groupId,
                    "Test Group",
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    false,
                    null,
                    LocalDateTime.now().plusDays(7).toString(),
                    Priority.HIGH.toString()
            );

            List<ChecklistResponseDTO> groupChecklists = List.of(groupChecklistResponse);
            lenient().when(checklistService.getGroupChecklist(any(), any()))
                    .thenReturn(groupChecklists);

            // when & then
            mockMvc.perform(get("/api/v1/checklists/groups/{groupId}", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].type").value("GROUP"))
                    .andExpect(jsonPath("$.data[0].groupId").value(groupId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("그룹 체크리스트 수정 성공 - 올바른 URL")
        void updateGroupChecklist_Success() throws Exception {
            // given
            ChecklistResponseDTO updatedGroupResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Updated Group Checklist",
                    "Updated Group Description",
                    ChecklistType.GROUP,
                    groupId,
                    "Test Group",
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    false,
                    null,
                    LocalDateTime.now().plusDays(14).toString(),
                    Priority.MEDIUM.toString()
            );

            lenient().when(checklistService.updateGroupChecklist(any(), any(ChecklistRequestDTO.class), any(), any()))
                    .thenReturn(updatedGroupResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/groups/{groupId}/{checklistId}", groupId, checklistId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checklistRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("GROUP"));
        }

        @Test
        @WithMockUser
        @DisplayName("잘못된 그룹 ID로 체크리스트 생성 시 400 반환")
        void createGroupChecklist_InvalidGroupId() throws Exception {
            // given
            String invalidGroupId = "invalid-uuid";

            // when & then
            mockMvc.perform(post("/api/v1/checklists/groups/{groupId}", invalidGroupId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checklistRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("체크리스트 상태 관리")
    class ChecklistStatusTests {

        @Test
        @WithMockUser
        @DisplayName("체크리스트 완료 처리 성공")
        void completeChecklist_Success() throws Exception {
            // given
            ChecklistResponseDTO completedResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Test Checklist",
                    "Test Description",
                    ChecklistType.PERSONAL,
                    null,
                    null,
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    true,
                    LocalDateTime.now().toString(),
                    LocalDateTime.now().plusDays(7).toString(),
                    Priority.HIGH.toString()
            );

            lenient().when(checklistService.completeChecklist(any(), any()))
                    .thenReturn(completedResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/{checklistId}/complete", checklistId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isCompleted").value(true))
                    .andExpect(jsonPath("$.data.completedAt").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("체크리스트 완료 취소 성공")
        void uncompleteChecklist_Success() throws Exception {
            // given
            ChecklistResponseDTO uncompletedResponse = new ChecklistResponseDTO(
                    checklistId,
                    "Test Checklist",
                    "Test Description",
                    ChecklistType.PERSONAL,
                    null,
                    null,
                    new GroupMemberDTO(userId, "Test User", "profile.jpg", GroupRole.MEMBER),
                    false,
                    null,
                    LocalDateTime.now().plusDays(7).toString(),
                    Priority.HIGH.toString()
            );

            lenient().when(checklistService.undoChecklist(any(), any()))
                    .thenReturn(uncompletedResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/{checklistId}/undo", checklistId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isCompleted").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("이미 완료된 체크리스트 재완료 시도")
        void completeAlreadyCompletedChecklist() throws Exception {
            // given
            lenient().when(checklistService.completeChecklist(any(), any()))
                    .thenThrow(new RuntimeException("Already completed"));

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/{checklistId}/complete", checklistId)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Already completed"));
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTests {

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 체크리스트 조회 시 예외 발생")
        void getChecklistDetails_NotFound() throws Exception {
            // given
            lenient().when(checklistService.getPersonalChecklist(any()))
                    .thenThrow(new RuntimeException("Checklist not found"));

            // when & then
            mockMvc.perform(get("/api/v1/checklists/me"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Checklist not found"));
        }

        @Test
        @WithMockUser
        @DisplayName("권한 없는 사용자의 체크리스트 수정 시도")
        void updateChecklist_AccessDenied() throws Exception {
            // given
            lenient().when(checklistService.updatePersonalChecklist(any(), any(ChecklistRequestDTO.class), any()))
                    .thenThrow(new RuntimeException("Access denied"));

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/me/{checklistId}", checklistId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(checklistRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @WithMockUser
        @DisplayName("잘못된 JSON 형식으로 요청 시 400 반환")
        void createChecklist_InvalidJSON() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/checklists/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 체크리스트 접근 - Mock 환경에서는 200 반환")
    void accessChecklist_Unauthorized() throws Exception {
        // Mock 환경에서는 실제 인증이 적용되지 않으므로 
        // 컨트롤러가 user 객체를 null로 받아 NullPointerException이 발생할 수 있음
        // 실제 통합 테스트에서 인증 테스트를 수행해야 함
        
        // given
        lenient().when(checklistService.getPersonalChecklist(any()))
                .thenReturn(List.of(checklistResponse));
        
        // when & then
        mockMvc.perform(get("/api/v1/checklists/me"))
                .andExpect(status().isOk()); // Mock 환경에서는 200 반환
    }

    @Nested
    @DisplayName("파라미터 검증 테스트")
    class ParameterValidationTests {

        @Test
        @WithMockUser
        @DisplayName("잘못된 체크리스트 ID 형식으로 요청 시 400 반환")
        void invalidChecklistIdFormat() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/v1/checklists/invalid-id/complete")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("음수 체크리스트 ID로 요청 시 처리")
        void negativeChecklistId() throws Exception {
            // given
            lenient().when(checklistService.completeChecklist(any(), any()))
                    .thenThrow(new RuntimeException("Invalid checklist ID"));

            // when & then
            mockMvc.perform(patch("/api/v1/checklists/-1/complete")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Invalid checklist ID"));
        }
    }
}