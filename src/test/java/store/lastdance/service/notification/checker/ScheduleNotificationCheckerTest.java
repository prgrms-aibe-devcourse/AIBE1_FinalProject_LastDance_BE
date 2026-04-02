package store.lastdance.service.notification.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.service.notification.NotificationSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleNotificationChecker 테스트")
class ScheduleNotificationCheckerTest {

    @Mock private CalendarRepository calendarRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private NotificationSender notificationSender;

    @InjectMocks
    private ScheduleNotificationChecker checker;

    private UUID userId;
    private User user;
    private NotificationSetting setting;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);

        setting = NotificationSetting.builder().userId(userId).build();
        setting.updateSSEEnabled(true);
        setting.updateScheduleReminder(true);

        now = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────
    // getType
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("getType은 SCHEDULE을 반환한다")
    void getType_returnsSchedule() {
        assertThat(checker.getType()).isEqualTo(NotificationType.SCHEDULE);
    }

    // ──────────────────────────────────────────────
    // check - 개인 일정
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("개인 일정 알림")
    class PersonalSchedule {

        @Test
        @DisplayName("개인 일정이 있으면 '[개인 일정]' 제목으로 sendIfNotCached를 호출한다")
        void personalSchedule_sendsWithPersonalTitle() {
            Calendar schedule = mockCalendar(CalendarType.PERSONAL, null, "팀 미팅");
            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                    contains("[개인 일정]"), eq("15분 후 시작 예정입니다."), eq("1"));
        }

        @Test
        @DisplayName("조회 범위는 now+15분 ±2분으로 설정된다")
        void queryRange_is15MinPlusMinus2() {
            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            LocalDateTime expectedStart = now.plusMinutes(13);
            LocalDateTime expectedEnd = now.plusMinutes(17);

            then(calendarRepository).should().findByUserIdAndStartTimeBetween(
                    eq(userId),
                    argThat(t -> !t.isBefore(expectedStart) && !t.isAfter(expectedStart.plusSeconds(5))),
                    argThat(t -> !t.isBefore(expectedEnd) && !t.isAfter(expectedEnd.plusSeconds(5)))
            );
        }

        @Test
        @DisplayName("해당 시간대에 일정이 없으면 sendIfNotCached를 호출하지 않는다")
        void noSchedules_noop() {
            given(calendarRepository.findByUserIdAndStartTimeBetween(any(), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(any(), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            then(notificationSender).should(never()).sendIfNotCached(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("개인 일정 여러 개가 있으면 각각 sendIfNotCached를 호출한다")
        void multiplePersonalSchedules_sendsEach() {
            Calendar s1 = mockCalendar(CalendarType.PERSONAL, null, "미팅");
            Calendar s2 = mockCalendarWithId(2L, CalendarType.PERSONAL, null, "점심");
            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(s1, s2));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            then(notificationSender).should(times(2))
                    .sendIfNotCached(eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                            anyString(), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - 그룹 일정
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("그룹 일정 알림")
    class GroupSchedule {

        @Test
        @DisplayName("그룹 일정이 있으면 '[그룹명 일정]' 제목으로 sendIfNotCached를 호출한다")
        void groupSchedule_sendsWithGroupName() {
            UUID groupId = UUID.randomUUID();
            Group group = mockGroup(groupId);
            Calendar schedule = mockCalendar(CalendarType.GROUP, group, "MT");

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(groupRepository.findGroupNameByGroupId(groupId))
                    .willReturn(Optional.of("우리집"));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                    contains("[우리집 일정]"), anyString(), anyString());
        }

        @Test
        @DisplayName("그룹명 조회 실패 시 '[그룹 일정]'으로 대체한다")
        void groupSchedule_groupNameFallback() {
            UUID groupId = UUID.randomUUID();
            Group group = mockGroup(groupId);
            Calendar schedule = mockCalendar(CalendarType.GROUP, group, "MT");

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(groupRepository.findGroupNameByGroupId(groupId))
                    .willReturn(Optional.empty());

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                    contains("[그룹 일정]"), anyString(), anyString());
        }

        @Test
        @DisplayName("그룹 일정이지만 group이 null이면 '[개인 일정]'으로 처리한다")
        void groupTypeButNullGroup_treatedAsPersonal() {
            Calendar schedule = mockCalendar(CalendarType.GROUP, null, "이상한 일정");

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                    contains("[개인 일정]"), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - 개인 + 그룹 혼합
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("개인 + 그룹 혼합")
    class MixedSchedule {

        @Test
        @DisplayName("개인 일정과 그룹 일정이 함께 있으면 둘 다 sendIfNotCached를 호출한다")
        void mixed_sendsForBoth() {
            Calendar personal = mockCalendar(CalendarType.PERSONAL, null, "개인 미팅");
            UUID groupId = UUID.randomUUID();
            Group group = mockGroup(groupId);
            Calendar group1 = mockCalendarWithId(2L, CalendarType.GROUP, group, "그룹 회의");

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(personal));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(group1));
            given(groupRepository.findGroupNameByGroupId(groupId))
                    .willReturn(Optional.of("스터디"));

            checker.check(user, setting, now);

            then(notificationSender).should(times(2))
                    .sendIfNotCached(eq(user), eq(setting), eq(NotificationType.SCHEDULE),
                            anyString(), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private Calendar mockCalendar(CalendarType type, Group group, String title) {
        return mockCalendarWithId(1L, type, group, title);
    }

    private Calendar mockCalendarWithId(Long id, CalendarType type, Group group, String title) {
        Calendar calendar = mock(Calendar.class);
        lenient().when(calendar.getCalendarId()).thenReturn(id);
        lenient().when(calendar.getType()).thenReturn(type);
        lenient().when(calendar.getGroup()).thenReturn(group);
        lenient().when(calendar.getTitle()).thenReturn(title);
        return calendar;
    }

    private Group mockGroup(UUID groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return group;
    }
}
