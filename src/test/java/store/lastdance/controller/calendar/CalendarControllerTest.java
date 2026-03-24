package store.lastdance.controller.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import store.lastdance.domain.calendar.CalendarCategory;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.RepeatType;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calendar.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calendar.response.CalendarResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.exception.GlobalExceptionHandler;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.calendar.CalendarV2Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CalendarV2Controller 테스트")
class CalendarControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CalendarV2Service calendarService;

    @InjectMocks
    private CalendarV2Controller calendarController;

    private UUID userId;
    private UUID groupId;
    private Long calendarId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        calendarId = 1L;

        // GroupControllerTest와 동일한 방식: standaloneSetup + AuthenticationPrincipal resolver
        mockMvc = MockMvcBuilders.standaloneSetup(calendarController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return new CustomOAuth2User(
                                        userId, "test@test.com", "테스트유저",
                                        "KAKAO", "12345", Map.of()
                                );
                            }
                        })
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v2/calendars
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v2/calendars - 일정 생성")
    class CreateCalendarTest {

        @Test
        @DisplayName("성공 - 개인 일정을 생성하면 201을 반환한다")
        void createCalendar_success_personal() throws Exception {
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("개인 일정").description("개인 일정 설명")
                    .startDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                    .endDate(LocalDateTime.of(2025, 6, 1, 11, 0))
                    .isAllDay(false).category(CalendarCategory.GENERAL).build();

            CalendarResponseDTO response = CalendarResponseDTO.builder()
                    .calendarId(calendarId).title("개인 일정")
                    .type(CalendarType.PERSONAL).userId(userId).build();

            given(calendarService.createCalendar(any(), eq(userId), isNull())).willReturn(response);

            mockMvc.perform(post("/api/v2/calendars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.calendarId").value(calendarId))
                    .andExpect(jsonPath("$.data.title").value("개인 일정"))
                    .andExpect(jsonPath("$.data.type").value("PERSONAL"));
        }

        @Test
        @DisplayName("성공 - 그룹 일정을 생성하면 201을 반환한다")
        void createCalendar_success_group() throws Exception {
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("그룹 일정").description("그룹 일정 설명")
                    .startDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                    .endDate(LocalDateTime.of(2025, 6, 1, 11, 0))
                    .isAllDay(false).category(CalendarCategory.GENERAL).build();

            CalendarResponseDTO response = CalendarResponseDTO.builder()
                    .calendarId(calendarId).title("그룹 일정")
                    .type(CalendarType.GROUP).groupId(groupId).groupName("테스트그룹").userId(userId).build();

            given(calendarService.createCalendar(any(), eq(userId), eq(groupId))).willReturn(response);

            mockMvc.perform(post("/api/v2/calendars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .param("groupId", groupId.toString()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("GROUP"))
                    .andExpect(jsonPath("$.data.groupId").value(groupId.toString()));
        }

        @Test
        @DisplayName("실패 - 제목 없이 요청하면 400을 반환한다")
        void createCalendar_fail_noTitle() throws Exception {
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .startDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                    .endDate(LocalDateTime.of(2025, 6, 1, 11, 0))
                    .isAllDay(false).category(CalendarCategory.GENERAL).build();

            mockMvc.perform(post("/api/v2/calendars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 그룹 멤버가 아니면 403을 반환한다")
        void createCalendar_fail_groupAccessDenied() throws Exception {
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("그룹 일정")
                    .startDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                    .endDate(LocalDateTime.of(2025, 6, 1, 11, 0))
                    .isAllDay(false).category(CalendarCategory.GENERAL).build();

            given(calendarService.createCalendar(any(), eq(userId), eq(groupId)))
                    .willThrow(new CustomException(ErrorCode.GROUP_ACCESS_DENIED));

            mockMvc.perform(post("/api/v2/calendars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .param("groupId", groupId.toString()))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_ACCESS_DENIED.getMessage()));
        }

        @Test
        @DisplayName("실패 - 사용자가 존재하지 않으면 404를 반환한다")
        void createCalendar_fail_userNotFound() throws Exception {
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("일정")
                    .startDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                    .endDate(LocalDateTime.of(2025, 6, 1, 11, 0))
                    .isAllDay(false).category(CalendarCategory.GENERAL).build();

            given(calendarService.createCalendar(any(), eq(userId), isNull()))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post("/api/v2/calendars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v2/calendars/me
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/calendars/me - 내 일정 목록 조회")
    class GetMyCalendarsTest {

        @Test
        @DisplayName("성공 - 기본 파라미터로 내 일정을 조회하면 200을 반환한다")
        void getMyCalendars_success_default() throws Exception {
            List<CalendarResponseDTO> responses = List.of(
                    CalendarResponseDTO.builder().calendarId(1L).title("일정 1").type(CalendarType.PERSONAL).userId(userId).build(),
                    CalendarResponseDTO.builder().calendarId(2L).title("일정 2").type(CalendarType.PERSONAL).userId(userId).build()
            );

            given(calendarService.getCalendarsByUser(eq(userId), anyString(), any(), any(), any(), any(), any()))
                    .willReturn(responses);

            mockMvc.perform(get("/api/v2/calendars/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].title").value("일정 1"))
                    .andExpect(jsonPath("$.data[1].title").value("일정 2"));
        }

        @Test
        @DisplayName("성공 - 그룹 필터로 내 일정을 조회하면 200을 반환한다")
        void getMyCalendars_success_withGroupFilter() throws Exception {
            List<CalendarResponseDTO> responses = List.of(
                    CalendarResponseDTO.builder().calendarId(1L).title("그룹 일정")
                            .type(CalendarType.GROUP).groupId(groupId).userId(userId).build()
            );

            given(calendarService.getCalendarsByUser(eq(userId), anyString(), any(), any(), any(), eq(groupId), any()))
                    .willReturn(responses);

            mockMvc.perform(get("/api/v2/calendars/me")
                            .param("groupId", groupId.toString())
                            .param("viewType", "MONTHLY"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].type").value("GROUP"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v2/calendars/{calendarId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/calendars/{calendarId} - 특정 일정 조회")
    class GetCalendarByIdTest {

        @Test
        @DisplayName("성공 - 일정을 조회하면 200을 반환한다")
        void getCalendar_success() throws Exception {
            CalendarResponseDTO response = CalendarResponseDTO.builder()
                    .calendarId(calendarId).title("테스트 일정").description("테스트 설명")
                    .type(CalendarType.PERSONAL).category(CalendarCategory.GENERAL)
                    .userId(userId).repeatType(RepeatType.NONE).build();

            given(calendarService.getCalendarById(calendarId, userId)).willReturn(response);

            mockMvc.perform(get("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.calendarId").value(calendarId))
                    .andExpect(jsonPath("$.data.title").value("테스트 일정"))
                    .andExpect(jsonPath("$.data.type").value("PERSONAL"));
        }

        @Test
        @DisplayName("실패 - 일정이 없으면 404를 반환한다")
        void getCalendar_fail_notFound() throws Exception {
            given(calendarService.getCalendarById(calendarId, userId))
                    .willThrow(new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            mockMvc.perform(get("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 403을 반환한다")
        void getCalendar_fail_accessDenied() throws Exception {
            given(calendarService.getCalendarById(calendarId, userId))
                    .willThrow(new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED));

            mockMvc.perform(get("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_ACCESS_DENIED.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PATCH /api/v2/calendars/{calendarId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/v2/calendars/{calendarId} - 일정 수정")
    class UpdateCalendarTest {

        @Test
        @DisplayName("성공 - 일정을 수정하면 200을 반환한다")
        void updateCalendar_success() throws Exception {
            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder()
                    .title("수정된 제목").description("수정된 설명").build();

            CalendarResponseDTO response = CalendarResponseDTO.builder()
                    .calendarId(calendarId).title("수정된 제목").description("수정된 설명")
                    .type(CalendarType.PERSONAL).userId(userId).build();

            given(calendarService.updateCalendar(eq(calendarId), any(), eq(userId))).willReturn(response);

            mockMvc.perform(patch("/api/v2/calendars/{calendarId}", calendarId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.data.description").value("수정된 설명"));
        }

        @Test
        @DisplayName("실패 - 일정이 없으면 404를 반환한다")
        void updateCalendar_fail_notFound() throws Exception {
            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder().title("수정 시도").build();

            given(calendarService.updateCalendar(eq(calendarId), any(), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            mockMvc.perform(patch("/api/v2/calendars/{calendarId}", calendarId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 403을 반환한다")
        void updateCalendar_fail_accessDenied() throws Exception {
            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder().title("수정 시도").build();

            given(calendarService.updateCalendar(eq(calendarId), any(), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED));

            mockMvc.perform(patch("/api/v2/calendars/{calendarId}", calendarId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_ACCESS_DENIED.getMessage()));
        }

        @Test
        @DisplayName("실패 - 제목이 200자를 초과하면 400을 반환한다")
        void updateCalendar_fail_titleTooLong() throws Exception {
            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder()
                    .title("a".repeat(201)).build();

            mockMvc.perform(patch("/api/v2/calendars/{calendarId}", calendarId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/v2/calendars/{calendarId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/v2/calendars/{calendarId} - 일정 삭제")
    class DeleteCalendarTest {

        @Test
        @DisplayName("성공 - 일정을 삭제하면 200을 반환한다")
        void deleteCalendar_success() throws Exception {
            willDoNothing().given(calendarService).deleteCalendar(calendarId, userId);

            mockMvc.perform(delete("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(calendarService).deleteCalendar(calendarId, userId);
        }

        @Test
        @DisplayName("실패 - 일정이 없으면 404를 반환한다")
        void deleteCalendar_fail_notFound() throws Exception {
            willThrow(new CustomException(ErrorCode.CALENDAR_NOT_FOUND))
                    .given(calendarService).deleteCalendar(calendarId, userId);

            mockMvc.perform(delete("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 403을 반환한다")
        void deleteCalendar_fail_accessDenied() throws Exception {
            willThrow(new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED))
                    .given(calendarService).deleteCalendar(calendarId, userId);

            mockMvc.perform(delete("/api/v2/calendars/{calendarId}", calendarId))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CALENDAR_ACCESS_DENIED.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v2/calendars/groups/{groupId}
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v2/calendars/groups/{groupId} - 그룹 일정 목록 조회")
    class GetGroupCalendarsTest {

        @Test
        @DisplayName("성공 - 그룹 일정을 조회하면 200을 반환한다")
        void getGroupCalendars_success() throws Exception {
            List<CalendarResponseDTO> responses = List.of(
                    CalendarResponseDTO.builder().calendarId(1L).title("그룹 일정 1")
                            .type(CalendarType.GROUP).groupId(groupId).userId(userId).build()
            );

            given(calendarService.getCalendarsByUser(eq(userId), anyString(), any(), any(), any(), eq(groupId), any()))
                    .willReturn(responses);

            mockMvc.perform(get("/api/v2/calendars/groups/{groupId}", groupId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].type").value("GROUP"))
                    .andExpect(jsonPath("$.data[0].groupId").value(groupId.toString()));
        }

        @Test
        @DisplayName("실패 - 그룹 멤버가 아니면 403을 반환한다")
        void getGroupCalendars_fail_accessDenied() throws Exception {
            given(calendarService.getCalendarsByUser(eq(userId), anyString(), any(), any(), any(), eq(groupId), any()))
                    .willThrow(new CustomException(ErrorCode.GROUP_ACCESS_DENIED));

            mockMvc.perform(get("/api/v2/calendars/groups/{groupId}", groupId))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ErrorCode.GROUP_ACCESS_DENIED.getMessage()));
        }
    }
}
