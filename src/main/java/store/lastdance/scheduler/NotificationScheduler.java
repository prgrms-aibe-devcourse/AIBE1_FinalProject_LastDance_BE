package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
    public void processScheduledNotifications() {
        log.info("=== 알림 스케줄러 실행 시작 ===");

        try {
            List<User> emailEnabledUsers = notificationSettingService.emailPermitted();
            List<User> sseEnabledUsers = notificationSettingService.ssePermitted();
            List<User> webPushEnabledUsers = notificationSettingService.webPushPermitted();

            Set<User> allUsers = new HashSet<>();
            allUsers.addAll(emailEnabledUsers);
            allUsers.addAll(sseEnabledUsers);
            allUsers.addAll(webPushEnabledUsers);
            
            List<User> allTargetUsers = new ArrayList<>(allUsers);

            if (allTargetUsers.isEmpty()) {
                log.warn("알림이 허용된 사용자가 없습니다. 알림 설정을 확인하세요.");
                return;
            }

            for (User user : allTargetUsers) {
                try {
                    log.debug("사용자 {} 알림 처리 시작", user.getUserId());
                    checkAndSendNotifications(user);
                } catch (Exception e) {
                    log.error("사용자 {}의 알림 처리 중 오류 발생: {}", user.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("알림 스케줄러 실행 중 전체 오류 발생: {}", e.getMessage(), e);
        }
        
        log.info("=== 알림 스케줄러 실행 완료 ===");
    }

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupSSEConnections() {
        try {
            log.debug("=== SSE 연결 정리 스케줄러 실행 ===");
            sseService.cleanupInactiveConnections();
            log.debug("현재 활성 SSE 연결 수: {}", sseService.getActiveConnectionCount());
        } catch (Exception e) {
            log.error("SSE 연결 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    protected void checkAndSendNotifications(User user) {
        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
        if (setting == null) {
            log.debug("사용자 {}의 알림 설정이 없음, 기본 설정 생성", user.getUserId());
            notificationSettingService.createDefaultSetting(user.getUserId());
            setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
            if (setting == null) {
                log.warn("사용자 {}의 기본 알림 설정 생성 실패", user.getUserId());
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15); // 15분 후

        if (setting.isNotificationEnabled(NotificationType.SCHEDULE)) {
            log.debug("사용자 {}의 일정 알림 체크", user.getUserId());
            checkScheduleNotifications(user, reminderTime);
        }

        if (setting.isNotificationEnabled(NotificationType.PAYMENT)) {
            log.debug("사용자 {}의 정산 알림 체크", user.getUserId());
            checkPaymentNotifications(user, now);
        }

        if (setting.isNotificationEnabled(NotificationType.CHECKLIST)) {
            log.debug("사용자 {}의 체크리스트 알림 체크", user.getUserId());
            checkChecklistNotifications(user, now);
        }
    }

    private void checkScheduleNotifications(User user, LocalDateTime reminderTime) {
        try {
            LocalDateTime startRange = reminderTime.minusMinutes(2);
            LocalDateTime endRange = reminderTime.plusMinutes(2);
            
            log.info("일정 알림 체크 - 사용자: {}, 시간 범위: {} ~ {}", 
                user.getUserId(), startRange, endRange);
            
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
                        // 중복 저장 시도 시 로그만 남기고 계속 진행
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 알림 - 사용자: {}, 일정: {}", user.getUserId(), schedule.getTitle());
                        } else {
                            log.error("일정 알림 처리 중 오류 - 사용자: {}, 일정: {}, 오류: {}", 
                                user.getUserId(), schedule.getTitle(), e.getMessage());
                        }
                    }
                } else {
                    log.info("이미 발송된 알림이므로 건너뜀 - 사용자: {}, 일정: {}", 
                        user.getUserId(), schedule.getTitle());
                }
            }
            
            if (allSchedules.isEmpty()) {
                log.info("15분 후 시작하는 일정이 없음 - 사용자: {}", user.getUserId());
            }
        } catch (Exception e) {
            log.error("일정 알림 체크 중 오류 발생 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    private void checkPaymentNotifications(User user, LocalDateTime now) {
        try {
            // 오늘 날짜의 미정산 분담금 조회
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            log.info("정산 알림 체크 - 사용자: {}, 오늘 날짜: {}", user.getUserId(), today);
            
            // 오늘 날짜에 생성된 미정산 분담금 조회
            List<ExpenseSplit> unpaidSplitsToday = expenseSplitRepository.findUnpaidSplitsByUserIdAndDate(
                user.getUserId(), startOfDay, endOfDay);

            log.info("조회된 오늘 생성된 미정산 분담금 수: {}", unpaidSplitsToday.size());

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
                        log.info("정산 알림 캐시 저장 완료 - 사용자: {}, 지출: {}, 캐시 키: {}",
                            user.getUserId(), expenseTitle, cacheKey);
                        
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
                        // 중복 저장 시도 시 로그만 남기고 계속 진행
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 정산 알림 - 분담금 ID: {}", split.getSplitId());
                        } else {
                            log.error("정산 알림 처리 중 오류 - 사용자: {}, 분담금 ID: {}, 오류: {}", 
                                user.getUserId(), split.getSplitId(), e.getMessage());
                        }
                    }
                } else {
                    log.info("이미 발송된 정산 알림이므로 건너뜀 - 분담금 ID: {}", split.getSplitId());
                }
            }
            
            if (unpaidSplitsToday.isEmpty()) {
                log.info("오늘 생성된 미정산 분담금이 없음 - 사용자: {}", user.getUserId());
            }
        } catch (Exception e) {
            log.error("정산 알림 체크 중 오류 발생 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    private void checkChecklistNotifications(User user, LocalDateTime now) {
        try {
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            log.info("체크리스트 알림 체크 - 사용자: {}, 시간 범위: {} ~ {}", 
                user.getUserId(), startOfDay, endOfDay);
            
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
                        log.info("체크리스트 알림 캐시 저장 완료 - 사용자: {}, 체크리스트: {}, 캐시키: {}", 
                            user.getUserId(), checklist.getTitle(), cacheKey);
                        
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
                        // 중복 저장 시도 시 로그만 남기고 계속 진행
                        if (e.getMessage().contains("constraint") || e.getMessage().contains("duplicate")) {
                            log.debug("이미 처리된 체크리스트 알림 - 체크리스트: {}", checklist.getTitle());
                        } else {
                            log.error("체크리스트 알림 처리 중 오류 - 사용자: {}, 체크리스트: {}, 오류: {}", 
                                user.getUserId(), checklist.getTitle(), e.getMessage());
                        }
                    }
                } else {
                    log.info("이미 발송된 체크리스트 알림이므로 건너뜀 - 체크리스트: {}", checklist.getTitle());
                }
            }
            
            if (dueTodayChecklists.isEmpty()) {
                log.info("오늘 마감인 미완료 체크리스트가 없음 - 사용자: {}", user.getUserId());
            }
        } catch (Exception e) {
            log.error("체크리스트 알림 체크 중 오류 발생 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage(), e);
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
