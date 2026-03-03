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
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.redis.NotificationCacheRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
        try {
            List<User> emailEnabledUsers = notificationSettingService.emailPermitted();
            List<User> sseEnabledUsers = notificationSettingService.ssePermitted();

            Set<User> allUsers = new HashSet<>();
            allUsers.addAll(emailEnabledUsers);
            allUsers.addAll(sseEnabledUsers);

            List<User> allTargetUsers = new ArrayList<>(allUsers);

            if (allTargetUsers.isEmpty()) {
                return;
            }

            for (User user : allTargetUsers) {
                try {
                    checkAndSendNotifications(user);
                } catch (CustomException e) {
                    throw new CustomException(ErrorCode.NOTIFICATION_FAILED);
                }
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SCHEDULER_FAILED);
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional(readOnly = true)
    public void cleanupSSEConnections() {
        try {
            sseService.cleanupInactiveConnections();
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SSE_CONNECTION_CLEANUP_FAILED);
        }
    }

    @Transactional(readOnly = true)
    protected void checkAndSendNotifications(User user) {
        try {
            NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
            if (setting == null) {
                notificationSettingService.createDefaultSetting(user.getUserId());
                setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
                if (setting == null) {
                    throw new CustomException(ErrorCode.NOTIFICATION_SETTING_CREATE_FAILED);
                }
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime = now.plusMinutes(15);

            if (setting.isNotificationEnabled(NotificationType.SCHEDULE)) {
                checkScheduleNotifications(user, reminderTime);
            }

            if (setting.isNotificationEnabled(NotificationType.PAYMENT)) {
                checkPaymentNotifications(user, now);
            }

            if (setting.isNotificationEnabled(NotificationType.CHECKLIST)) {
                checkChecklistNotifications(user, now);
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SCHEDULER_FAILED);
        }
    }

    @Transactional(readOnly = true)
    protected void checkScheduleNotifications(User user, LocalDateTime reminderTime) {
        try {
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
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                if (!alreadySent) {
                    String scheduleTypeText;
                    if (schedule.getType() == CalendarType.GROUP) {
                        if (schedule.getGroup().getGroupId() != null) {
                            String groupName = groupRepository.findGroupNameByGroupId(schedule.getGroup().getGroupId())
                                    .orElse("그룹");
                            scheduleTypeText = "[" + groupName + " 일정] ";
                        } else {
                            scheduleTypeText = "[그룹일정] ";
                        }
                    } else {
                        scheduleTypeText = "[개인 일정] ";
                    }
                    String title = scheduleTypeText + schedule.getTitle();
                    String content = "15분 후 시작 예정입니다.";

                    try {
                        NotificationCache cache = NotificationCache.create(
                            user.getUserId(), 
                            NotificationType.SCHEDULE, 
                            title, 
                            content,
                            schedule.getCalendarId().toString()
                        );

                        notificationCacheRepository.save(cache);

                        hybridNotificationService.sendNotification(
                            user.getUserId(),
                            NotificationType.SCHEDULE,
                            title,
                            content,
                            schedule.getCalendarId().toString()
                        );

                        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
                        if (setting != null && setting.isEmailEnabledForType(NotificationType.SCHEDULE)) {
                            String mailProvider = getMailProviderByUser(user);
                            mailService.sendScheduleReminder(
                                user.getEmail(),
                                title,
                                content,
                                mailProvider
                            );
                        }
                    } catch (CustomException e) {
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 알림 - 사용자: {}, 일정: {}", user.getUserId(), schedule.getTitle());
                        } else {
                            throw new CustomException(ErrorCode.NOTIFICATION_FAILED);
                        }
                    }
                }
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_CHECK_FAILED);
        }
    }

    @Transactional(readOnly = true)
    protected void checkPaymentNotifications(User user, LocalDateTime now) {
        try {
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<ExpenseSplit> unpaidSplitsToday = expenseSplitRepository.findUnpaidSplitsByUserAndDate(
                user, startOfDay, endOfDay);

            for (ExpenseSplit split : unpaidSplitsToday) {
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.PAYMENT, split.getSplitId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                
                if (!alreadySent) {
                    String expenseTitle = split.getExpense() != null
                            ? (split.getExpense().getGroup() != null
                            ? "[" + split.getExpense().getGroup().getGroupName() + " 정산] " + split.getExpense().getTitle()
                            : "[개인 정산] " + split.getExpense().getTitle())
                            : "지출";
                    String title = expenseTitle + " (분담금: " + split.getAmount() + "원)";
                    String content = "새로운 정산 요청이 있습니다.";

                    try {
                        NotificationCache cache = NotificationCache.create(
                            user.getUserId(),
                            NotificationType.PAYMENT,
                            title,
                            content,
                            split.getSplitId().toString()
                        );
                        notificationCacheRepository.save(cache);

                        hybridNotificationService.sendNotification(
                            user.getUserId(),
                            NotificationType.PAYMENT,
                            title,
                            content,
                            split.getSplitId().toString()
                        );
                        
                        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
                        if (setting != null && setting.isEmailEnabledForType(NotificationType.PAYMENT)) {
                            String mailProvider = getMailProviderByUser(user);
                            mailService.sendPaymentReminder(
                                user.getEmail(),
                                title,
                                content,
                                mailProvider
                            );
                        }
                    } catch (CustomException e) {
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 정산 알림 - 분담금 ID: {}", split.getSplitId());
                        } else {
                            throw new CustomException(ErrorCode.NOTIFICATION_PAYMENT_FAILED);
                        }
                    }
                }
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_PAYMENT_FAILED);
        }
    }

    @Transactional(readOnly = true)
    protected void checkChecklistNotifications(User user, LocalDateTime now) {
        try {
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<Checklist> dueTodayChecklists = checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                user.getUserId(), startOfDay, endOfDay);

            for (Checklist checklist : dueTodayChecklists) {
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.CHECKLIST, checklist.getChecklistId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);

                if (!alreadySent) {
                    String title = checklist.getGroup() != null
                            ? "[" + checklist.getGroup().getGroupName() + " 할일] " + checklist.getTitle()
                            : "[개인 할일] " + checklist.getTitle();
                    String content = "오늘이 마감일입니다.";

                    try {
                        NotificationCache cache = NotificationCache.create(
                            user.getUserId(),
                            NotificationType.CHECKLIST,
                            title,
                            content,
                            checklist.getChecklistId().toString()
                        );
                        notificationCacheRepository.save(cache);
                        
                        hybridNotificationService.sendNotification(
                            user.getUserId(),
                            NotificationType.CHECKLIST,
                            title,
                            content,
                            checklist.getChecklistId().toString()
                        );
                        
                        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
                        if (setting != null && setting.isEmailEnabledForType(NotificationType.CHECKLIST)) {
                            String mailProvider = getMailProviderByUser(user);
                            mailService.sendChecklistReminder(
                                user.getEmail(),
                                title,
                                content,
                                mailProvider
                            );
                        }
                    } catch (Exception e) {
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 체크리스트 알림 - 체크리스트: {}", checklist.getTitle());
                        } else {
                            throw new CustomException(ErrorCode.NOTIFICATION_CHECKLIST_FAILED);
                        }
                    }
                }
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_CHECKLIST_FAILED);
        }
    }

    private String getMailProviderByUser(User user) {
        return switch (user.getProvider()) {
            case GOOGLE -> "gmail";
            case NAVER -> "naver";
            case KAKAO -> "gmail";
            default -> "gmail";
        };
    }
}
