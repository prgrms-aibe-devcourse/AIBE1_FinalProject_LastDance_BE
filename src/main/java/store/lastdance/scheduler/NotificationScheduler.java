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
import store.lastdance.repository.notification.NotificationCacheRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationCacheRepository notificationCacheRepository;
    private final MailService mailService;
    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationSettingService notificationSettingService;
    private final HybridNotificationService hybridNotificationService;
    private final SSENotificationService sseService;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional(readOnly = true)
    public void processScheduledNotifications() {

        try {
            List<User> emailEnabledUsers = notificationSettingService.emailPermitted();
            List<User> sseEnabledUsers = notificationSettingService.ssePermitted();
            List<User> webPushEnabledUsers = notificationSettingService.webPushPermitted();

            Set<User> allUsers = new HashSet<>();
            allUsers.addAll(emailEnabledUsers);
            allUsers.addAll(sseEnabledUsers);
            allUsers.addAll(webPushEnabledUsers);
            
            List<User> allTargetUsers = new ArrayList<>(allUsers);



            for (User user : allTargetUsers) {
                try {
                    checkAndSendNotifications(user);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        
    }

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional(readOnly = true)
    public void cleanupSSEConnections() {
        try {
            sseService.cleanupInactiveConnections();
        } catch (Exception e) {
        }
    }

    @Transactional(readOnly = true)
    protected void checkAndSendNotifications(User user) {
        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
        if (setting == null) {
            notificationSettingService.createDefaultSetting(user.getUserId());
            setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
            if (setting == null) {
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15); // 15분 후

        if (setting.isNotificationEnabled(NotificationType.SCHEDULE)) {
            checkScheduleNotifications(user, reminderTime);
        }

        if (setting.isNotificationEnabled(NotificationType.PAYMENT)) {
            checkPaymentNotifications(user, now);
        }

        if (setting.isNotificationEnabled(NotificationType.CHECKLIST)) {
            checkChecklistNotifications(user, now);
        }
    }

    @Transactional(readOnly = true)
    protected void checkScheduleNotifications(User user, LocalDateTime reminderTime) {
        try {
            LocalDateTime startRange = reminderTime.minusMinutes(2);
            LocalDateTime endRange = reminderTime.plusMinutes(2);
            

            // 1. 개인 일정 조회
            List<Calendar> personalSchedules = calendarRepository.findByUserIdAndStartTimeBetween(
                user.getUserId(), startRange, endRange);
            
            // 2. 사용자가 속한 그룹들의 그룹 일정 조회
            List<Calendar> groupSchedules = calendarRepository.findGroupCalendarsForUserInTimeRange(
                user.getUserId(), startRange, endRange);

            // 전체 일정 합치기
            List<Calendar> allSchedules = new java.util.ArrayList<>();
            allSchedules.addAll(personalSchedules);
            allSchedules.addAll(groupSchedules);
            
            for (Calendar schedule : allSchedules) {
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.SCHEDULE, schedule.getCalendarId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                
                if (!alreadySent) {
                    String scheduleTypeText = schedule.getType() == CalendarType.GROUP
                            ? "[" + (schedule.getGroup() != null ? schedule.getGroup().getGroupName()+" 일정" : "그룹일정") + "] "
                            : "[개인 일정]";
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

                        // 2. 이메일 알림 (설정 체크 후 발송)
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
                    } catch (Exception e) {

                    }
                }
            }
            

        } catch (Exception e) {

        }
    }

    @Transactional(readOnly = true)
    protected void checkPaymentNotifications(User user, LocalDateTime now) {
        try {
            // 오늘 날짜의 미정산 분담금 조회
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            

            // 오늘 날짜에 생성된 미정산 분담금 조회
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

                        // 1. 실시간 알림
                        hybridNotificationService.sendNotification(
                            user.getUserId(),
                            NotificationType.PAYMENT,
                            title,
                            content,
                            split.getSplitId().toString()
                        );
                        
                        // 2. 이메일 알림 (설정 체크 후 발송)
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
                    } catch (Exception e) {

                    }
                }
            }
            

        } catch (Exception e) {
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

                        // 1. 실시간 알림
                        hybridNotificationService.sendNotification(
                            user.getUserId(),
                            NotificationType.CHECKLIST,
                            title,
                            content,
                            checklist.getChecklistId().toString()
                        );
                        
                        // 2. 이메일 알림 (설정 체크 후 발송)
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

                    }
                }
            }
            

        } catch (Exception e) {

        }
    }

    /**
     * 사용자의 OAuth Provider에 따라 적절한 메일 서비스 선택
     */
    private String getMailProviderByUser(User user) {
        return switch (user.getProvider()) {
            case GOOGLE -> "gmail";
            case NAVER -> "naver";
            case KAKAO -> "gmail"; // 카카오는 Gmail SMTP 사용
            default -> "gmail"; // 기본값
        };
    }
}
