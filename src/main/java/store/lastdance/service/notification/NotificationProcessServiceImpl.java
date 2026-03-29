package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.expense.ExpenseSplit;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationProcessServiceImpl implements NotificationProcessService {

    private final NotificationCacheRepository notificationCacheRepository;
    private final SSENotificationV2Service sseService;
    private final MailV2Service mailService;
    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final NotificationSettingRepository settingRepository;
    private final GroupRepository groupRepository;

    @Override
    public List<NotificationSetting> findAllActiveSettings() {
        return settingRepository.findAllActiveWithUser();
    }

    @Override
    public void checkAndSendNotifications(User user, NotificationSetting setting) {
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

    @Override
    public void checkScheduleNotifications(User user, NotificationSetting setting, LocalDateTime reminderTime) {
        LocalDateTime startRange = reminderTime.minusMinutes(2);
        LocalDateTime endRange = reminderTime.plusMinutes(2);

        List<Calendar> allSchedules = new ArrayList<>();
        allSchedules.addAll(calendarRepository.findByUserIdAndStartTimeBetween(user.getUserId(), startRange, endRange));
        allSchedules.addAll(calendarRepository.findGroupCalendarsForUserInTimeRange(user.getUserId(), startRange, endRange));

        for (Calendar schedule : allSchedules) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.SCHEDULE, schedule.getCalendarId().toString());
            if (notificationCacheRepository.existsById(cacheKey)) continue;

            String title = resolveSchedulePrefix(schedule) + schedule.getTitle();
            String content = "15분 후 시작 예정입니다.";

            sendAndCache(user, setting, NotificationType.SCHEDULE, title, content, schedule.getCalendarId().toString());
        }
    }

    @Override
    public void checkPaymentNotifications(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<ExpenseSplit> splits = expenseSplitRepository.findUnpaidSplitsByUserAndDate(user, startOfDay, endOfDay);

        for (ExpenseSplit split : splits) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.PAYMENT, split.getSplitId().toString());
            if (notificationCacheRepository.existsById(cacheKey)) continue;

            String title = resolvePaymentTitle(split);
            String content = "새로운 정산 요청이 있습니다.";

            sendAndCache(user, setting, NotificationType.PAYMENT, title, content, split.getSplitId().toString());
        }
    }

    @Override
    public void checkChecklistNotifications(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Checklist> checklists = checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                user.getUserId(), startOfDay, endOfDay);

        for (Checklist checklist : checklists) {
            String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.CHECKLIST, checklist.getChecklistId().toString());
            if (notificationCacheRepository.existsById(cacheKey)) continue;

            String title = checklist.getGroup() != null
                    ? "[" + checklist.getGroup().getGroupName() + " 할일] " + checklist.getTitle()
                    : "[개인 할일] " + checklist.getTitle();
            String content = "오늘이 마감일입니다.";

            sendAndCache(user, setting, NotificationType.CHECKLIST, title, content, checklist.getChecklistId().toString());
        }
    }

    private String getMailProviderByUser(User user) {
        return user.getProvider() == OAuthProvider.NAVER ? "naver" : "gmail";
    }

    private void sendAndCache(User user, NotificationSetting setting,
                              NotificationType type, String title, String content, String relatedId) {
        try {
            sendSSE(user, setting, type, title, content, relatedId);
            sendMail(user, setting, type, title, content);
            notificationCacheRepository.save(
                    NotificationCache.create(user.getUserId(), type, title, content, relatedId));
        } catch (Exception e) {
            log.error("알림 처리 실패: userId={}, type={}, relatedId={}, error={}",
                    user.getUserId(), type, relatedId, e.getMessage());
        }
    }

    private void sendSSE(User user, NotificationSetting setting,
                         NotificationType type, String title, String content, String relatedId) {
        if (!setting.isSseEnabled()) return;
        if (sseService.sendNotification(user.getUserId(), title, content, type, relatedId)) {
            log.info("SSE 알림 전송 완료: userId={}, type={}", user.getUserId(), type);
        }
    }

    private void sendMail(User user, NotificationSetting setting,
                          NotificationType type, String title, String content) {
        if (!setting.isEmailEnabledForType(type)) return;
        String provider = getMailProviderByUser(user);
        switch (type) {
            case SCHEDULE  -> mailService.sendScheduleReminder(user.getEmail(), title, content, provider);
            case PAYMENT   -> mailService.sendPaymentReminder(user.getEmail(), title, content, provider);
            case CHECKLIST -> mailService.sendChecklistReminder(user.getEmail(), title, content, provider);
        }
        log.info("이메일 알림 전송 완료: userId={}, type={}", user.getUserId(), type);
    }

    private String resolveSchedulePrefix(Calendar schedule) {
        if (schedule.getType() == CalendarType.GROUP && schedule.getGroup() != null) {
            String groupName = groupRepository.findGroupNameByGroupId(schedule.getGroup().getGroupId())
                    .orElse("그룹");
            return "[" + groupName + " 일정] ";
        }
        return "[개인 일정] ";
    }

    private String resolvePaymentTitle(ExpenseSplit split) {
        if (split.getExpense() == null) return "지출 (분담금: " + split.getAmount() + "원)";
        String base = split.getExpense().getGroup() != null
                ? "[" + split.getExpense().getGroup().getGroupName() + " 정산] " + split.getExpense().getTitle()
                : "[개인 정산] " + split.getExpense().getTitle();
        return base + " (분담금: " + split.getAmount() + "원)";
    }
}
