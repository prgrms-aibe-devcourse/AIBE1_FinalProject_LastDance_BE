package store.lastdance.service.notification.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.service.notification.NotificationSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChecklistNotificationChecker 테스트")
class ChecklistNotificationCheckerTest {

    @Mock private ChecklistRepository checklistRepository;
    @Mock private NotificationSender notificationSender;

    @InjectMocks
    private ChecklistNotificationChecker checker;

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
        setting.updateChecklistReminder(true);

        now = LocalDate.now().atTime(9, 0);
    }

    // ──────────────────────────────────────────────
    // getType
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("getType은 CHECKLIST를 반환한다")
    void getType_returnsChecklist() {
        assertThat(checker.getType()).isEqualTo(NotificationType.CHECKLIST);
    }

    // ──────────────────────────────────────────────
    // check - 조회 범위
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("조회 범위는 당일 자정부터 다음날 자정까지이다")
    void queryRange_isFullDay() {
        given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                .willReturn(List.of());

        checker.check(user, setting, now);

        LocalDateTime expectedStart = now.toLocalDate().atStartOfDay();
        LocalDateTime expectedEnd = expectedStart.plusDays(1);

        then(checklistRepository).should().findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                eq(userId),
                eq(expectedStart),
                eq(expectedEnd)
        );
    }

    // ──────────────────────────────────────────────
    // check - 개인 할일
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("개인 할일 알림")
    class PersonalChecklist {

        @Test
        @DisplayName("개인 할일이 있으면 '[개인 할일]' 제목으로 sendIfNotCached를 호출한다")
        void personalChecklist_sendsWithPersonalTitle() {
            Checklist checklist = mockChecklist(1L, null, "청소하기");
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of(checklist));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                    contains("[개인 할일]"), eq("오늘이 마감일입니다."), eq("1"));
        }

        @Test
        @DisplayName("개인 할일 제목에 할일명이 포함된다")
        void personalChecklist_titleContainsTaskName() {
            Checklist checklist = mockChecklist(1L, null, "장보기");
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of(checklist));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                    contains("장보기"), anyString(), anyString());
        }

        @Test
        @DisplayName("마감 할일이 없으면 sendIfNotCached를 호출하지 않는다")
        void noChecklists_noop() {
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            then(notificationSender).should(never()).sendIfNotCached(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("마감 할일이 여러 개면 각각 sendIfNotCached를 호출한다")
        void multipleChecklists_sendsEach() {
            Checklist c1 = mockChecklist(1L, null, "운동하기");
            Checklist c2 = mockChecklist(2L, null, "독서하기");
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of(c1, c2));

            checker.check(user, setting, now);

            then(notificationSender).should(times(2))
                    .sendIfNotCached(eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                            anyString(), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - 그룹 할일
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("그룹 할일 알림")
    class GroupChecklist {

        @Test
        @DisplayName("그룹 할일이면 '[그룹명 할일]' 제목으로 sendIfNotCached를 호출한다")
        void groupChecklist_sendsWithGroupName() {
            Group group = mockGroup("스터디");
            Checklist checklist = mockChecklist(1L, group, "발표 준비");
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of(checklist));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                    contains("[스터디 할일]"), anyString(), anyString());
        }

        @Test
        @DisplayName("그룹 할일 제목에 할일명이 포함된다")
        void groupChecklist_titleContainsTaskName() {
            Group group = mockGroup("우리집");
            Checklist checklist = mockChecklist(1L, group, "공과금 납부");
            given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                    .willReturn(List.of(checklist));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                    contains("공과금 납부"), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - relatedId 검증
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("sendIfNotCached에 checklist의 ID가 relatedId로 전달된다")
    void relatedId_isChecklistId() {
        Checklist checklist = mockChecklist(42L, null, "할일");
        given(checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(eq(userId), any(), any()))
                .willReturn(List.of(checklist));

        checker.check(user, setting, now);

        then(notificationSender).should().sendIfNotCached(
                eq(user), eq(setting), eq(NotificationType.CHECKLIST),
                anyString(), anyString(), eq("42"));
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private Checklist mockChecklist(Long id, Group group, String title) {
        Checklist checklist = mock(Checklist.class);
        given(checklist.getChecklistId()).willReturn(id);
        given(checklist.getGroup()).willReturn(group);
        lenient().when(checklist.getTitle()).thenReturn(title);
        return checklist;
    }

    private Group mockGroup(String groupName) {
        Group group = mock(Group.class);
        given(group.getGroupName()).willReturn(groupName);
        return group;
    }
}
