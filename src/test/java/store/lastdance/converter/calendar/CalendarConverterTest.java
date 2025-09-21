package store.lastdance.converter.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import store.lastdance.domain.calendar.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calendar.response.CalendarResponseDTO;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarConverter 테스트")
class CalendarConverterTest {

    @InjectMocks
    private CalendarConverter sut;

    private User user;
    private Group group;
    private UUID userId;
    private UUID groupId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        startTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        endTime = LocalDateTime.of(2024, 1, 1, 10, 0);

        user = User.builder()
                .email("test@test.com")
                .username("테스트유저")
                .nickname("테스트닉네임")
                .provider(OAuthProvider.KAKAO)
                .providerId("12345")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        Field userIdField = User.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        ReflectionUtils.setField(userIdField, user, userId);

        group = Group.builder()
                .groupName("테스트그룹")
                .inviteCode("TEST01")
                .owner(user)
                .build();

        Field groupIdField = Group.class.getDeclaredField("groupId");
        groupIdField.setAccessible(true);
        ReflectionUtils.setField(groupIdField, group, groupId);
    }

    @Nested
    @DisplayName("toEntity (CreateCalendarRequestDTO 기반) 메서드 테스트")
    class ToEntityFromRequestTest {

        @Test
        @DisplayName("성공 - CreateCalendarRequestDTO로부터 개인 일정 엔티티를 생성한다")
        void toEntity_success_personalCalendar() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("개인 일정")
                    .description("개인 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(false)
                    .category(CalendarCategory.GENERAL)
                    .repeatType(RepeatType.WEEKLY)
                    .repeatEndDate(startTime.plusWeeks(4))
                    .build();

            // when
            Calendar result = sut.toEntity(request, user, null, CalendarType.PERSONAL);

            // then
            assertThat(result.getTitle()).isEqualTo("개인 일정");
            assertThat(result.getDescription()).isEqualTo("개인 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(startTime);
            assertThat(result.getEndDate()).isEqualTo(endTime);
            assertThat(result.getIsAllDay()).isFalse();
            assertThat(result.getType()).isEqualTo(CalendarType.PERSONAL);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.GENERAL);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getGroup()).isNull();
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.WEEKLY);
            assertThat(result.getRepeatEndDate()).isEqualTo(startTime.plusWeeks(4));
        }

        @Test
        @DisplayName("성공 - CreateCalendarRequestDTO로부터 그룹 일정 엔티티를 생성한다")
        void toEntity_success_groupCalendar() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("그룹 일정")
                    .description("그룹 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(true)
                    .category(CalendarCategory.GENERAL)
                    .repeatType(RepeatType.NONE)
                    .build();

            // when
            Calendar result = sut.toEntity(request, user, group, CalendarType.GROUP);

            // then
            assertThat(result.getTitle()).isEqualTo("그룹 일정");
            assertThat(result.getDescription()).isEqualTo("그룹 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(startTime);
            assertThat(result.getEndDate()).isEqualTo(endTime);
            assertThat(result.getIsAllDay()).isTrue();
            assertThat(result.getType()).isEqualTo(CalendarType.GROUP);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.GENERAL);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getGroup()).isEqualTo(group);
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.NONE);
            assertThat(result.getRepeatEndDate()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity (반복 일정 인스턴스 생성용) 메서드 테스트")
    class ToEntityFromBaseTest {

        @Test
        @DisplayName("성공 - 기존 일정으로부터 새로운 시간의 인스턴스를 생성한다")
        void toEntity_success_recurringInstance() throws Exception {
            // given
            Calendar baseCalendar = Calendar.builder()
                    .title("반복 일정")
                    .description("반복 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(false)
                    .type(CalendarType.PERSONAL)
                    .category(CalendarCategory.GENERAL)
                    .user(user)
                    .group(null)
                    .repeatType(RepeatType.WEEKLY)
                    .repeatEndDate(startTime.plusWeeks(8))
                    .build();

            Field calendarIdField = Calendar.class.getDeclaredField("calendarId");
            calendarIdField.setAccessible(true);
            ReflectionUtils.setField(calendarIdField, baseCalendar, 1L);

            LocalDateTime newStartDate = startTime.plusWeeks(1);
            Duration duration = Duration.ofHours(1);

            // when
            Calendar result = sut.toEntity(baseCalendar, newStartDate, duration);

            // then
            assertThat(result.getTitle()).isEqualTo("반복 일정");
            assertThat(result.getDescription()).isEqualTo("반복 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(newStartDate);
            assertThat(result.getEndDate()).isEqualTo(newStartDate.plus(duration));
            assertThat(result.getIsAllDay()).isFalse();
            assertThat(result.getType()).isEqualTo(CalendarType.PERSONAL);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.GENERAL);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getGroup()).isNull();
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.WEEKLY);
            assertThat(result.getRepeatEndDate()).isEqualTo(startTime.plusWeeks(8));
        }

        @Test
        @DisplayName("성공 - 그룹 반복 일정의 인스턴스를 생성한다")
        void toEntity_success_groupRecurringInstance() {
            // given
            Calendar baseCalendar = Calendar.builder()
                    .title("그룹 반복 일정")
                    .description("그룹 반복 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(true)
                    .type(CalendarType.GROUP)
                    .category(CalendarCategory.MEETING)
                    .user(user)
                    .group(group)
                    .repeatType(RepeatType.MONTHLY)
                    .repeatEndDate(startTime.plusMonths(6))
                    .build();

            LocalDateTime newStartDate = startTime.plusMonths(1);
            Duration duration = Duration.ofHours(2);

            // when
            Calendar result = sut.toEntity(baseCalendar, newStartDate, duration);

            // then
            assertThat(result.getTitle()).isEqualTo("그룹 반복 일정");
            assertThat(result.getDescription()).isEqualTo("그룹 반복 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(newStartDate);
            assertThat(result.getEndDate()).isEqualTo(newStartDate.plus(duration));
            assertThat(result.getIsAllDay()).isTrue();
            assertThat(result.getType()).isEqualTo(CalendarType.GROUP);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.MEETING);
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getGroup()).isEqualTo(group);
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.MONTHLY);
            assertThat(result.getRepeatEndDate()).isEqualTo(startTime.plusMonths(6));
        }
    }

    @Nested
    @DisplayName("toDto 메서드 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("성공 - Calendar 엔티티를 CalendarResponseDTO로 변환한다")
        void toDto_success_withoutGroup() throws Exception {
            // given
            Calendar calendar = Calendar.builder()
                    .title("개인 일정")
                    .description("개인 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(false)
                    .type(CalendarType.PERSONAL)
                    .category(CalendarCategory.GENERAL)
                    .user(user)
                    .group(null)
                    .repeatType(RepeatType.NONE)
                    .repeatEndDate(null)
                    .build();

            Field calendarIdField = Calendar.class.getDeclaredField("calendarId");
            calendarIdField.setAccessible(true);
            ReflectionUtils.setField(calendarIdField, calendar, 1L);

            Field createdAtField = calendar.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 8, 0);
            ReflectionUtils.setField(createdAtField, calendar, createdAt);

            // when
            CalendarResponseDTO result = sut.toDto(calendar, user, null, null);

            // then
            assertThat(result.getCalendarId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("개인 일정");
            assertThat(result.getDescription()).isEqualTo("개인 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(startTime);
            assertThat(result.getEndDate()).isEqualTo(endTime);
            assertThat(result.getIsAllDay()).isFalse();
            assertThat(result.getType()).isEqualTo(CalendarType.PERSONAL);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.GENERAL);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getGroupId()).isNull();
            assertThat(result.getGroupName()).isNull();
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.NONE);
            assertThat(result.getRepeatEndDate()).isNull();
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("성공 - 그룹 일정 엔티티를 CalendarResponseDTO로 변환한다")
        void toDto_success_withGroup() throws Exception {
            // given
            Calendar calendar = Calendar.builder()
                    .title("그룹 일정")
                    .description("그룹 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(true)
                    .type(CalendarType.GROUP)
                    .category(CalendarCategory.GENERAL)
                    .user(user)
                    .group(group)
                    .repeatType(RepeatType.WEEKLY)
                    .repeatEndDate(startTime.plusWeeks(4))
                    .build();

            Field calendarIdField = Calendar.class.getDeclaredField("calendarId");
            calendarIdField.setAccessible(true);
            ReflectionUtils.setField(calendarIdField, calendar, 2L);

            Field createdAtField = calendar.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 8, 30);
            ReflectionUtils.setField(createdAtField, calendar, createdAt);

            String groupName = "테스트그룹명";

            // when
            CalendarResponseDTO result = sut.toDto(calendar, user, group, groupName);

            // then
            assertThat(result.getCalendarId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("그룹 일정");
            assertThat(result.getDescription()).isEqualTo("그룹 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(startTime);
            assertThat(result.getEndDate()).isEqualTo(endTime);
            assertThat(result.getIsAllDay()).isTrue();
            assertThat(result.getType()).isEqualTo(CalendarType.GROUP);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.GENERAL);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getGroupId()).isEqualTo(groupId);
            assertThat(result.getGroupName()).isEqualTo(groupName);
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.WEEKLY);
            assertThat(result.getRepeatEndDate()).isEqualTo(startTime.plusWeeks(4));
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("성공 - null 그룹명으로도 정상적으로 변환한다")
        void toDto_success_withNullGroupName() throws Exception {
            // given
            Calendar calendar = Calendar.builder()
                    .title("그룹 일정")
                    .description("그룹 일정 설명")
                    .startDate(startTime)
                    .endDate(endTime)
                    .isAllDay(false)
                    .type(CalendarType.GROUP)
                    .category(CalendarCategory.MEETING)
                    .user(user)
                    .group(group)
                    .repeatType(RepeatType.NONE)
                    .build();

            Field calendarIdField = Calendar.class.getDeclaredField("calendarId");
            calendarIdField.setAccessible(true);
            ReflectionUtils.setField(calendarIdField, calendar, 3L);

            Field createdAtField = calendar.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 9, 0);
            ReflectionUtils.setField(createdAtField, calendar, createdAt);

            // when
            CalendarResponseDTO result = sut.toDto(calendar, user, group, null);

            // then
            assertThat(result.getCalendarId()).isEqualTo(3L);
            assertThat(result.getTitle()).isEqualTo("그룹 일정");
            assertThat(result.getDescription()).isEqualTo("그룹 일정 설명");
            assertThat(result.getStartDate()).isEqualTo(startTime);
            assertThat(result.getEndDate()).isEqualTo(endTime);
            assertThat(result.getIsAllDay()).isFalse();
            assertThat(result.getType()).isEqualTo(CalendarType.GROUP);
            assertThat(result.getCategory()).isEqualTo(CalendarCategory.MEETING);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getGroupId()).isEqualTo(groupId);
            assertThat(result.getGroupName()).isNull();
            assertThat(result.getRepeatType()).isEqualTo(RepeatType.NONE);
            assertThat(result.getRepeatEndDate()).isNull();
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }
    }
}
