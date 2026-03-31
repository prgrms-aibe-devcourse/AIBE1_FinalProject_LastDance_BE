package store.lastdance.service.notification;

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
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.repository.redis.NotificationCacheRepository;

import java.math.BigDecimal;
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
@DisplayName("NotificationProcessServiceImpl 테스트")
class NotificationProcessServiceImplTest {

    @Mock private NotificationCacheRepository notificationCacheRepository;
    @Mock private SSENotificationV2Service sseService;
    @Mock private MailV2Service mailService;
    @Mock private CalendarRepository calendarRepository;
    @Mock private ChecklistRepository checklistRepository;
    @Mock private ExpenseSplitRepository expenseSplitRepository;
    @Mock private NotificationSettingRepository settingRepository;
    @Mock private GroupRepository groupRepository;

    @InjectMocks
    private NotificationProcessServiceImpl service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn("test@gmail.com");
        lenient().when(user.getProvider()).thenReturn(OAuthProvider.GOOGLE);
    }

    // ──────────────────────────────────────────────
    // findAllActiveSettings
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("findAllActiveSettings")
    class FindAllActiveSettings {

        @Test
        @DisplayName("repository에서 반환한 리스트를 그대로 반환한다")
        void returnsRepositoryResult() {
            NotificationSetting setting = buildSetting(true, false);
            given(settingRepository.findAllActiveWithUser()).willReturn(List.of(setting));

            List<NotificationSetting> result = service.findAllActiveSettings();

            assertThat(result).hasSize(1);
            then(settingRepository).should().findAllActiveWithUser();
        }
    }

    // ──────────────────────────────────────────────
    // checkAndSendNotifications
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("checkAndSendNotifications")
    class CheckAndSendNotifications {

        @Test
        @DisplayName("세 알림 타입이 모두 활성화되면 세 체크 메서드를 모두 실행한다")
        void allEnabled_runsAllThreeChecks() {
            NotificationSetting setting = buildSetting(true, false);

            given(calendarRepository.findByUserIdAndStartTimeBetween(any(), any(), any())).willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(any(), any(), any())).willReturn(List.of());
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(any(), any(), any())).willReturn(List.of());
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(any(), any(), any())).willReturn(List.of());

            service.checkAndSendNotifications(user, setting);

            then(calendarRepository).should().findByUserIdAndStartTimeBetween(any(), any(), any());
            then(expenseSplitRepository).should().findUnpaidSplitsByUserAndDate(any(), any(), any());
            then(checklistRepository).should().findByUserIdAndDueDateBetweenAndIsCompletedFalse(any(), any(), any());
        }

        @Test
        @DisplayName("일정 알림이 비활성화되면 캘린더 조회를 하지 않는다")
        void scheduleDisabled_skipsCalendarQuery() {
            NotificationSetting setting = NotificationSetting.builder().userId(userId).build();
            setting.updatePaymentReminder(true);
            setting.updateChecklistReminder(true);
            // scheduleReminder 기본값 false 유지

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(any(), any(), any())).willReturn(List.of());
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(any(), any(), any())).willReturn(List.of());

            service.checkAndSendNotifications(user, setting);

            then(calendarRepository).should(never()).findByUserIdAndStartTimeBetween(any(), any(), any());
        }

        @Test
        @DisplayName("정산 알림이 비활성화되면 정산 조회를 하지 않는다")
        void paymentDisabled_skipsExpenseQuery() {
            NotificationSetting setting = NotificationSetting.builder().userId(userId).build();
            setting.updateScheduleReminder(true);
            setting.updateChecklistReminder(true);

            given(calendarRepository.findByUserIdAndStartTimeBetween(any(), any(), any())).willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(any(), any(), any())).willReturn(List.of());
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(any(), any(), any())).willReturn(List.of());

            service.checkAndSendNotifications(user, setting);

            then(expenseSplitRepository).should(never()).findUnpaidSplitsByUserAndDate(any(), any(), any());
        }

        @Test
        @DisplayName("체크리스트 알림이 비활성화되면 체크리스트 조회를 하지 않는다")
        void checklistDisabled_skipsChecklistQuery() {
            NotificationSetting setting = NotificationSetting.builder().userId(userId).build();
            setting.updateScheduleReminder(true);
            setting.updatePaymentReminder(true);

            given(calendarRepository.findByUserIdAndStartTimeBetween(any(), any(), any())).willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(any(), any(), any())).willReturn(List.of());
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(any(), any(), any())).willReturn(List.of());

            service.checkAndSendNotifications(user, setting);

            then(checklistRepository).should(never()).findByUserIdAndDueDateBetweenAndIsCompletedFalse(any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────
    // checkScheduleNotifications
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("checkScheduleNotifications")
    class CheckScheduleNotifications {

        @Test
        @DisplayName("캐시에 없는 개인 일정이 있으면 SSE를 전송하고 캐시에 저장한다")
        void personalSchedule_sendsSSEAndCaches() {
            NotificationSetting setting = buildSetting(true, false);
            Calendar schedule = mock(Calendar.class);
            given(schedule.getCalendarId()).willReturn(1L);
            given(schedule.getType()).willReturn(CalendarType.PERSONAL);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.SCHEDULE), anyString())).willReturn(true);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should().sendNotification(
                    eq(userId), contains("[개인 일정]"), anyString(), eq(NotificationType.SCHEDULE), anyString());
            then(notificationCacheRepository).should().save(any(NotificationCache.class));
        }

        @Test
        @DisplayName("캐시에 없는 그룹 일정이 있으면 그룹명이 포함된 제목으로 SSE를 전송한다")
        void groupSchedule_includesGroupNameInTitle() {
            NotificationSetting setting = buildSetting(true, false);
            UUID groupId = UUID.randomUUID();

            Group group = mock(Group.class);
            given(group.getGroupId()).willReturn(groupId);

            Calendar schedule = mock(Calendar.class);
            given(schedule.getCalendarId()).willReturn(1L);
            given(schedule.getType()).willReturn(CalendarType.GROUP);
            given(schedule.getGroup()).willReturn(group);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(groupRepository.findGroupNameByGroupId(groupId))
                    .willReturn(Optional.of("우리집"));
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.SCHEDULE), anyString())).willReturn(true);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should().sendNotification(
                    eq(userId), contains("[우리집 일정]"), anyString(),
                    eq(NotificationType.SCHEDULE), anyString());
        }

        @Test
        @DisplayName("그룹명 조회 실패 시 '그룹'으로 대체한다")
        void groupSchedule_groupNameFallback() {
            NotificationSetting setting = buildSetting(true, false);
            UUID groupId = UUID.randomUUID();
            // findGroupNameByGroupId가 empty를 반환하므로 getGroupName은 호출되지 않음 → getGroupId만 stubbing
            Group group = mock(Group.class);
            given(group.getGroupId()).willReturn(groupId);
            Calendar schedule = mockCalendar(CalendarType.GROUP, group);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(groupRepository.findGroupNameByGroupId(groupId)).willReturn(Optional.empty());
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.SCHEDULE), anyString())).willReturn(true);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should().sendNotification(
                    eq(userId), contains("[그룹 일정]"), anyString(), eq(NotificationType.SCHEDULE), anyString());
        }

        @Test
        @DisplayName("이메일 설정이 켜져 있으면 이메일도 함께 전송한다")
        void emailEnabled_sendsEmailToo() {
            NotificationSetting setting = buildSetting(true, true);
            Calendar schedule = mock(Calendar.class);
            given(schedule.getCalendarId()).willReturn(1L);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(any(), anyString(), anyString(), any(), anyString()))
                    .willReturn(true);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(mailService).should().sendScheduleReminder(
                    eq("test@gmail.com"), anyString(), anyString(), eq("gmail"));
        }

        @Test
        @DisplayName("캐시에 이미 있는 일정은 SSE 전송과 캐시 저장을 건너뛴다")
        void alreadyCached_skipsAll() {
            NotificationSetting setting = buildSetting(true, false);
            // 캐시 스킵 시 getTitle/getType/getGroup은 호출되지 않으므로 getCalendarId만 stubbing
            Calendar schedule = mock(Calendar.class);
            given(schedule.getCalendarId()).willReturn(1L);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(notificationCacheRepository.existsById(anyString())).willReturn(true);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
            then(notificationCacheRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("해당 시간대에 일정이 없으면 아무것도 하지 않는다")
        void noSchedules_noop() {
            NotificationSetting setting = buildSetting(true, false);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
            then(notificationCacheRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("SSE가 비활성화되어 있으면 SSE 전송을 하지 않는다")
        void sseDisabled_skipsSSE() {
            NotificationSetting setting = buildSetting(false, false); // SSE off
            Calendar schedule = mock(Calendar.class);
            given(schedule.getCalendarId()).willReturn(1L);

            given(calendarRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
                    .willReturn(List.of(schedule));
            given(calendarRepository.findGroupCalendarsForUserInTimeRange(eq(userId), any(), any()))
                    .willReturn(List.of());
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);

            service.checkScheduleNotifications(user, setting, LocalDateTime.now().plusMinutes(15));

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
            // 캐시는 저장해야 함
            then(notificationCacheRepository).should().save(any(NotificationCache.class));
        }
    }

    // ──────────────────────────────────────────────
    // checkPaymentNotifications
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("checkPaymentNotifications")
    class CheckPaymentNotifications {

        @Test
        @DisplayName("미납 개인 정산이 있고 캐시에 없으면 SSE를 전송하고 캐시에 저장한다")
        void personalSplit_sendsSSEAndCaches() {
            NotificationSetting setting = buildSetting(true, false);
            ExpenseSplit split = mockSplit(null, "회식비", new BigDecimal("30000"));

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.PAYMENT), anyString())).willReturn(true);

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(sseService).should().sendNotification(
                    eq(userId), contains("[개인 정산]"), anyString(), eq(NotificationType.PAYMENT), anyString());
            then(notificationCacheRepository).should().save(any(NotificationCache.class));
        }

        @Test
        @DisplayName("그룹 정산이면 제목에 그룹명이 포함된다")
        void groupSplit_includesGroupNameInTitle() {
            NotificationSetting setting = buildSetting(true, false);
            // resolvePaymentTitle에서 getGroupName만 호출됨 → getGroupId는 불필요
            Group group = mock(Group.class);
            given(group.getGroupName()).willReturn("여행팀");
            ExpenseSplit split = mockSplit(group, "항공권", new BigDecimal("150000"));

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.PAYMENT), anyString())).willReturn(true);

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(sseService).should().sendNotification(
                    eq(userId), contains("[여행팀 정산]"), anyString(), eq(NotificationType.PAYMENT), anyString());
        }

        @Test
        @DisplayName("expense가 null이면 '지출'로 대체한 제목으로 전송한다")
        void nullExpense_fallbackTitle() {
            NotificationSetting setting = buildSetting(true, false);
            ExpenseSplit split = mock(ExpenseSplit.class);
            given(split.getSplitId()).willReturn(1L);
            given(split.getAmount()).willReturn(new BigDecimal("5000"));
            given(split.getExpense()).willReturn(null);

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.PAYMENT), anyString())).willReturn(true);

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(sseService).should().sendNotification(
                    eq(userId), contains("지출"), anyString(), eq(NotificationType.PAYMENT), anyString());
        }

        @Test
        @DisplayName("NAVER 사용자는 메일 provider가 naver로 전송된다")
        void naverUser_usesNaverProvider() {
            given(user.getProvider()).willReturn(OAuthProvider.NAVER);
            NotificationSetting setting = buildSetting(true, true);
            ExpenseSplit split = mockSplit(null, "밥값", new BigDecimal("10000"));

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(any(), anyString(), anyString(), any(), anyString()))
                    .willReturn(true);

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(mailService).should().sendPaymentReminder(anyString(), anyString(), anyString(), eq("naver"));
        }

        @Test
        @DisplayName("캐시에 이미 있는 정산은 건너뛴다")
        void alreadyCached_skips() {
            NotificationSetting setting = buildSetting(true, false);
            // 캐시 스킵 시 getExpense/getAmount는 호출되지 않으므로 getSplitId만 stubbing
            ExpenseSplit split = mock(ExpenseSplit.class);
            given(split.getSplitId()).willReturn(1L);

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));
            given(notificationCacheRepository.existsById(anyString())).willReturn(true);

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
            then(notificationCacheRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("미납 정산이 없으면 아무것도 하지 않는다")
        void noSplits_noop() {
            NotificationSetting setting = buildSetting(true, false);

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of());

            service.checkPaymentNotifications(user, setting, LocalDateTime.now());

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────
    // checkChecklistNotifications
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("checkChecklistNotifications")
    class CheckChecklistNotifications {

        @Test
        @DisplayName("오늘 마감인 개인 체크리스트가 있으면 SSE를 전송하고 캐시에 저장한다")
        void personalChecklist_sendsSSEAndCaches() {
            NotificationSetting setting = buildSetting(true, false);
            Checklist checklist = mockChecklist(null, "청소하기");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.CHECKLIST), anyString())).willReturn(true);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(sseService).should().sendNotification(
                    eq(userId), contains("[개인 할일]"), anyString(), eq(NotificationType.CHECKLIST), anyString());
            then(notificationCacheRepository).should().save(any(NotificationCache.class));
        }

        @Test
        @DisplayName("그룹 체크리스트이면 제목에 그룹명이 포함된다")
        void groupChecklist_includesGroupNameInTitle() {
            NotificationSetting setting = buildSetting(true, false);
            // checklist.getGroup().getGroupName()만 호출됨 → getGroupId는 불필요
            Group group = mock(Group.class);
            given(group.getGroupName()).willReturn("스터디");
            Checklist checklist = mockChecklist(group, "발표 준비");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(eq(userId), anyString(), anyString(),
                    eq(NotificationType.CHECKLIST), anyString())).willReturn(true);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(sseService).should().sendNotification(
                    eq(userId), contains("[스터디 할일]"), anyString(), eq(NotificationType.CHECKLIST), anyString());
        }

        @Test
        @DisplayName("체크리스트 이메일 알림이 켜져 있으면 이메일도 전송한다")
        void emailEnabled_sendsEmailToo() {
            NotificationSetting setting = buildSetting(true, true);
            Checklist checklist = mockChecklist(null, "장보기");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            given(sseService.sendNotification(any(), anyString(), anyString(), any(), anyString()))
                    .willReturn(true);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(mailService).should().sendChecklistReminder(
                    eq("test@gmail.com"), anyString(), anyString(), eq("gmail"));
        }

        @Test
        @DisplayName("캐시에 이미 있는 체크리스트는 건너뛴다")
        void alreadyCached_skips() {
            NotificationSetting setting = buildSetting(true, false);
            // 캐시 스킵 시 getTitle/getGroup은 호출되지 않으므로 getChecklistId만 stubbing
            Checklist checklist = mock(Checklist.class);
            given(checklist.getChecklistId()).willReturn(1L);

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(true);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
            then(notificationCacheRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("마감 체크리스트가 없으면 아무것도 하지 않는다")
        void noChecklists_noop() {
            NotificationSetting setting = buildSetting(true, false);

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of());

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(sseService).should(never()).sendNotification(any(), any(), any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────
    // getMailProviderByUser (via sendMail 간접 검증)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("메일 provider 분기")
    class MailProvider {

        @Test
        @DisplayName("GOOGLE 사용자는 gmail provider로 이메일을 전송한다")
        void google_usesGmail() {
            given(user.getProvider()).willReturn(OAuthProvider.GOOGLE);
            NotificationSetting setting = buildSetting(false, true);
            Checklist checklist = mockChecklist(null, "할일");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(mailService).should().sendChecklistReminder(anyString(), anyString(), anyString(), eq("gmail"));
        }

        @Test
        @DisplayName("KAKAO 사용자는 gmail provider로 이메일을 전송한다")
        void kakao_usesGmail() {
            given(user.getProvider()).willReturn(OAuthProvider.KAKAO);
            NotificationSetting setting = buildSetting(false, true);
            Checklist checklist = mockChecklist(null, "할일");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(mailService).should().sendChecklistReminder(anyString(), anyString(), anyString(), eq("gmail"));
        }

        @Test
        @DisplayName("NAVER 사용자는 naver provider로 이메일을 전송한다")
        void naver_usesNaver() {
            given(user.getProvider()).willReturn(OAuthProvider.NAVER);
            NotificationSetting setting = buildSetting(false, true);
            Checklist checklist = mockChecklist(null, "할일");

            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                    eq(userId), any(), any())).willReturn(List.of(checklist));
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);

            service.checkChecklistNotifications(user, setting, LocalDateTime.now());

            then(mailService).should().sendChecklistReminder(anyString(), anyString(), anyString(), eq("naver"));
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private NotificationSetting buildSetting(boolean sseEnabled, boolean emailEnabled) {
        NotificationSetting setting = NotificationSetting.builder().userId(userId).build();
        setting.updateSSEEnabled(sseEnabled);
        setting.updateEmailEnabled(emailEnabled);
        setting.updateScheduleReminder(true);
        setting.updatePaymentReminder(true);
        setting.updateChecklistReminder(true);
        return setting;
    }

    /** DB PK가 @GeneratedValue라 mock으로 생성 */
    private Calendar mockCalendar(CalendarType type, Group group) {
        Calendar calendar = mock(Calendar.class);
        given(calendar.getCalendarId()).willReturn(1L);
        given(calendar.getTitle()).willReturn("테스트 일정");
        given(calendar.getType()).willReturn(type);
        given(calendar.getGroup()).willReturn(group);
        return calendar;
    }

    private ExpenseSplit mockSplit(Group group, String expenseTitle, BigDecimal amount) {
        Expense expense = mock(Expense.class);
        given(expense.getTitle()).willReturn(expenseTitle);
        given(expense.getGroup()).willReturn(group);

        ExpenseSplit split = mock(ExpenseSplit.class);
        given(split.getSplitId()).willReturn(1L);
        given(split.getAmount()).willReturn(amount);
        given(split.getExpense()).willReturn(expense);
        return split;
    }

    private Checklist mockChecklist(Group group, String title) {
        Checklist checklist = mock(Checklist.class);
        given(checklist.getChecklistId()).willReturn(1L);
        given(checklist.getTitle()).willReturn(title);
        given(checklist.getGroup()).willReturn(group);
        return checklist;
    }
}
