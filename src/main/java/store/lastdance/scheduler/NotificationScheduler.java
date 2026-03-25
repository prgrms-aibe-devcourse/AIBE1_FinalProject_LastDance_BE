package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.redis.NotificationCacheRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationCacheRepository notificationCacheRepository;
    private final MailV2Service mailService;
    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationSettingV2Service notificationSettingService;
    private final HybridNotificationV2Service hybridNotificationService;
    private final SSENotificationV2Service sseService;
    private final GroupRepository groupRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void processScheduledNotifications() {
        List<NotificationSetting> settings = settingRepository.findAllActiveWithUser();

        if (settings.isEmpty()) {
            return;
        }

        for (NotificationSetting setting : settings) {
            User user = setting.getUser();
            if (user == null) {
                continue;
            }
            try {
                checkAndSendNotifications(user, setting);
            } catch (Exception e) {
                log.error("알림 처리 실패: userId={}, error={}", user.getUserId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupSSEConnections() {
        try {
            sseService.cleanupInactiveConnections();
        } catch (Exception e) {
            log.error("SSE 연결 정리 실패: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    protected void checkAndSendNotifications(User user, NotificationSetting setting) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        if (setting.isNotificationEnabled(NotificationType.SCHEDULE)) {
            checkScheduleNotifications(user, setting, reminderTime);
        }
        if (setting.isNotificationEnabled(NotificationType.PAYMENT)) {
            checkPaymentNotifications(user, setting, now);
        }
        if (setting.isNotificationEnabled(NotificationType.CHECKLIST)) {
            checkChecklistNotifications(user, setting, now);
        }
    }

    @Transactional(readOnly = true)
    protected void checkScheduleNotifications(User user, NotificationSetting setting, LocalDateTime reminderTime) {
        LocalDateTime startRange = reminderTime.minusMinutes(2);
        LocalDateTime endRange = reminderTime.plusMinutes(2);

        List<Calendar> personalSchedules = calendarRepository.findByUserIdAndStartTimeBetween(
                user.getUserId(), startRange, endRange);
        List<Calendar> groupSchedules = calendarRepository.findGroupCalendarsForUserInTimeRange(
                user.getUserId(), startRange, endRange);

        List<Calendar> allSchedules = new java.util.ArrayList<>();
        allSchedules.addAll(personalSchedules);
        allSchedules.addAll(groupSchedules);

        for (Calendar schedule : allSchedules) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.SCHEDULE, schedule.getCalendarId().toString());

            if (notificationCacheRepository.existsById(cacheKey)) {
                continue;
            }

            String scheduleTypeText;
            if (schedule.getType() == CalendarType.GROUP && schedule.getGroup() != null) {
                String groupName = groupRepository.findGroupNameByGroupId(schedule.getGroup().getGroupId())
                        .orElse("그룹");
                scheduleTypeText = "[" + groupName + " 일정] ";
            } else {
                scheduleTypeText = "[개인 일정] ";
            }
            String title = scheduleTypeText + schedule.getTitle();
            String content = "15분 후 시작 예정입니다.";

            try {
                notificationCacheRepository.save(NotificationCache.create(
                        user.getUserId(), NotificationType.SCHEDULE, title, content,
                        schedule.getCalendarId().toString()));

                hybridNotificationService.sendNotification(
                        user.getUserId(), NotificationType.SCHEDULE, title, content,
                        schedule.getCalendarId().toString(), setting);

                if (setting.isEmailEnabledForType(NotificationType.SCHEDULE)) {
                    mailService.sendScheduleReminder(
                            user.getEmail(), title, content, getMailProviderByUser(user));
                }
            } catch (Exception e) {
                log.error("일정 알림 처리 실패: userId={}, scheduleId={}, error={}",
                        user.getUserId(), schedule.getCalendarId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    protected void checkPaymentNotifications(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<ExpenseSplit> unpaidSplitsToday = expenseSplitRepository.findUnpaidSplitsByUserAndDate(
                user, startOfDay, endOfDay);

        for (ExpenseSplit split : unpaidSplitsToday) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.PAYMENT, split.getSplitId().toString());

            if (notificationCacheRepository.existsById(cacheKey)) {
                continue;
            }

            String expenseTitle = split.getExpense() != null
                    ? (split.getExpense().getGroup() != null
                    ? "[" + split.getExpense().getGroup().getGroupName() + " 정산] " + split.getExpense().getTitle()
                    : "[개인 정산] " + split.getExpense().getTitle())
                    : "지출";
            String title = expenseTitle + " (분담금: " + split.getAmount() + "원)";
            String content = "새로운 정산 요청이 있습니다.";

            try {
                notificationCacheRepository.save(NotificationCache.create(
                        user.getUserId(), NotificationType.PAYMENT, title, content,
                        split.getSplitId().toString()));

                hybridNotificationService.sendNotification(
                        user.getUserId(), NotificationType.PAYMENT, title, content,
                        split.getSplitId().toString(), setting);

                if (setting.isEmailEnabledForType(NotificationType.PAYMENT)) {
                    mailService.sendPaymentReminder(
                            user.getEmail(), title, content, getMailProviderByUser(user));
                }
            } catch (Exception e) {
                log.error("정산 알림 처리 실패: userId={}, splitId={}, error={}",
                        user.getUserId(), split.getSplitId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    protected void checkChecklistNotifications(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Checklist> dueTodayChecklists = checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                user.getUserId(), startOfDay, endOfDay);

        for (Checklist checklist : dueTodayChecklists) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.CHECKLIST, checklist.getChecklistId().toString());

            if (notificationCacheRepository.existsById(cacheKey)) {
                continue;
            }

            String title = checklist.getGroup() != null
                    ? "[" + checklist.getGroup().getGroupName() + " 할일] " + checklist.getTitle()
                    : "[개인 할일] " + checklist.getTitle();
            String content = "오늘이 마감일입니다.";

            try {
                notificationCacheRepository.save(NotificationCache.create(
                        user.getUserId(), NotificationType.CHECKLIST, title, content,
                        checklist.getChecklistId().toString()));

                hybridNotificationService.sendNotification(
                        user.getUserId(), NotificationType.CHECKLIST, title, content,
                        checklist.getChecklistId().toString(), setting);

                if (setting.isEmailEnabledForType(NotificationType.CHECKLIST)) {
                    mailService.sendChecklistReminder(
                            user.getEmail(), title, content, getMailProviderByUser(user));
                }
            } catch (Exception e) {
                log.error("체크리스트 알림 처리 실패: userId={}, checklistId={}, error={}",
                        user.getUserId(), checklist.getChecklistId(), e.getMessage());
            }
        }
    }

    private String getMailProviderByUser(User user) {
        return switch (user.getProvider()) {
            case NAVER -> "naver";
            default -> "gmail";
        };
    }
}
