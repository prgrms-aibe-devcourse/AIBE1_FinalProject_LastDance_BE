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
import store.lastdance.service.notification.MailService;
import store.lastdance.service.notification.NotificationSettingService;
import store.lastdance.service.notification.HybridNotificationService;
import store.lastdance.service.notification.SSENotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Scheduled(fixedRate = 30000) // 30초마다 실행 (테스트용으로 변경)
    @Transactional(readOnly = true)
    public void processScheduledNotifications() {
        log.info("=== 알림 스케줄러 실행 시작 ===");

        try {
            List<User> enabledUsers = notificationSettingService.emailPermitted();

            if (enabledUsers.isEmpty()) {
                log.warn("이메일 알림이 허용된 사용자가 없습니다. 알림 설정을 확인하세요.");
                return;
            }

            for (User user : enabledUsers) {
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

    // SSE 연결 정리 스케줄러 추가
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

    @Transactional
    protected void checkAndSendNotifications(User user) {
        // 사용자의 알림 설정 조회
        NotificationSetting setting = settingRepository.findByUserId(user.getUserId()).orElse(null);
        if (setting == null || !setting.getEmailEnabled()) {
            log.debug("사용자 {}의 이메일 알림이 비활성화됨", user.getUserId());
            return;
        }

        log.debug("사용자 {}의 알림 체크 시작 - 이메일: {}", user.getUserId(), user.getEmail());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15); // 15분 후

        // 일정 알림 체크
        if (setting.getScheduleReminder()) {
            log.debug("사용자 {}의 일정 알림 체크", user.getUserId());
            checkScheduleNotifications(user, reminderTime);
        }

        // 정산 요청 알림 체크
        if (setting.getPaymentReminder()) {
            log.debug("사용자 {}의 정산 알림 체크", user.getUserId());
            checkPaymentNotifications(user, now);
        }

        // 체크리스트 알림 체크
        if (setting.getChecklistReminder()) {
            log.debug("사용자 {}의 체크리스트 알림 체크", user.getUserId());
            checkChecklistNotifications(user, now);
        }
    }

    private void checkScheduleNotifications(User user, LocalDateTime reminderTime) {
        try {
            // 15분 후에 시작하는 일정들 조회
            LocalDateTime startRange = reminderTime.minusMinutes(2); // 약간의 여유
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

            log.info("조회된 예정 일정 수 - 개인: {}, 그룹: {}, 총합: {}", 
                personalSchedules.size(), groupSchedules.size(), allSchedules.size());
            
            for (Calendar schedule : allSchedules) {
                log.info("일정 발견 - ID: {}, 제목: {}, 시작시간: {}, 타입: {}", 
                    schedule.getCalendarId(), schedule.getTitle(), schedule.getStartDate(), 
                    schedule.getType());
                
                // 이미 알림이 발송되었는지 체크 (사용자별로)
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.SCHEDULE, schedule.getCalendarId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                log.info("알림 발송 이력 체크 - 사용자: {}, 캐시키: {}, 이미 발송됨: {}", 
                    user.getUserId(), cacheKey, alreadySent);
                
                if (!alreadySent) {
                    String scheduleTypeText = schedule.getType() == CalendarType.GROUP ? "[그룹] " : "";
                    String title = scheduleTypeText + schedule.getTitle();
                    String content = "15분 후 시작 예정입니다.";

                    NotificationCache cache = NotificationCache.create(
                        user.getUserId(), 
                        NotificationType.SCHEDULE, 
                        title, 
                        content,
                        schedule.getCalendarId().toString()
                    );

                    notificationCacheRepository.save(cache);
                    log.info("알림 캐시 저장 완료 - 사용자: {}, 일정: {}, 캐시키: {}", 
                        user.getUserId(), title, cacheKey);
                    
                    // 1단계: SSE + 웹푸시 시도
                    hybridNotificationService.sendNotification(
                        user.getUserId(),
                        NotificationType.SCHEDULE,
                        title,
                        content,
                        schedule.getCalendarId().toString()
                    );
                    
                    // 2단계: 이메일 발송
                    String mailProvider = getMailProviderByUser(user);
                    log.info("이메일 발송 시도 - 수신자: {}, 일정: {}, 발송서비스: {}", 
                        user.getEmail(), title, mailProvider.toUpperCase());
                    
                    mailService.sendScheduleReminder(
                        user.getEmail(), 
                        title, 
                        content,
                        mailProvider
                    );
                    
                    log.info("일정 알림 발송 완료 (실시간+이메일) - 사용자: {}, 일정: {}, 서비스: {}", 
                        user.getUserId(), title, mailProvider.toUpperCase());
                }
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
                log.info("미정산 분담금 발견 - ID: {}, 지출ID: {}, 금액: {}, 생성일: {}", 
                    split.getSplitId(), split.getExpenseId(), split.getAmount(), split.getCreatedAt());
                
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.PAYMENT, split.getSplitId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                log.info("정산 알림 발송 이력 체크 - 캐시키: {}, 이미 발송됨: {}", cacheKey, alreadySent);
                
                if (!alreadySent) {
                    String expenseTitle = split.getExpense() != null ? split.getExpense().getTitle() : "그룹 지출";
                    String title = expenseTitle + " (분담금: " + split.getAmount() + "원)";
                    String content = "새로운 정산 요청이 있습니다.";

                    NotificationCache cache = NotificationCache.create(
                        user.getUserId(),
                        NotificationType.PAYMENT,
                        title,
                        content,
                        split.getSplitId().toString()
                    );
                    notificationCacheRepository.save(cache);
                    log.info("정산 알림 캐시 저장 완료 - 사용자: {}, 지출: {}, 캐시키: {}", 
                        user.getUserId(), expenseTitle, cacheKey);
                    
                    // 1단계: SSE + 웹푸시 시도
                    hybridNotificationService.sendNotification(
                        user.getUserId(),
                        NotificationType.PAYMENT,
                        title,
                        content,
                        split.getSplitId().toString()
                    );
                    
                    // 2단계: 이메일 발송
                    String mailProvider = getMailProviderByUser(user);
                    log.info("정산 이메일 발송 시도 - 수신자: {}, 지출: {}, 분담금: {}, 발송서비스: {}", 
                        user.getEmail(), expenseTitle, split.getAmount(), mailProvider.toUpperCase());
                    
                    mailService.sendPaymentReminder(
                        user.getEmail(),
                        title,
                        content,
                        mailProvider
                    );
                    
                    log.info("정산 알림 발송 완료 (실시간+이메일) - 사용자: {}, 항목: {}, 분담금: {}", 
                        user.getUserId(), expenseTitle, split.getAmount());
                }
            }
        } catch (Exception e) {
            log.error("정산 알림 체크 중 오류 발생 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    private void checkChecklistNotifications(User user, LocalDateTime now) {
        try {
            // 오늘이 마감일인 체크리스트들 조회
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            log.info("체크리스트 알림 체크 - 사용자: {}, 시간 범위: {} ~ {}", 
                user.getUserId(), startOfDay, endOfDay);
            
            List<Checklist> dueTodayChecklists = checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                user.getUserId(), startOfDay, endOfDay);

            log.info("조회된 오늘 마감 미완료 체크리스트 수: {}", dueTodayChecklists.size());

            for (Checklist checklist : dueTodayChecklists) {
                log.info("체크리스트 발견 - ID: {}, 제목: {}, 마감일: {}, 완료여부: {}", 
                    checklist.getChecklistId(), checklist.getTitle(), 
                    checklist.getDueDate(), checklist.getIsCompleted());
                
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.CHECKLIST, checklist.getChecklistId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                log.info("체크리스트 알림 발송 이력 체크 - 캐시키: {}, 이미 발송됨: {}", cacheKey, alreadySent);
                
                if (!alreadySent) {
                    String title = checklist.getTitle();
                    String content = "오늘이 마감일입니다.";

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
                    
                    // 1단계: SSE + 웹푸시 시도
                    hybridNotificationService.sendNotification(
                        user.getUserId(),
                        NotificationType.CHECKLIST,
                        title,
                        content,
                        checklist.getChecklistId().toString()
                    );
                    
                    // 2단계: 이메일 발송
                    String mailProvider = getMailProviderByUser(user);
                    log.info("체크리스트 이메일 발송 시도 - 수신자: {}, 체크리스트: {}, 발송서비스: {}", 
                        user.getEmail(), checklist.getTitle(), mailProvider.toUpperCase());
                    
                    mailService.sendChecklistReminder(
                        user.getEmail(),
                        title,
                        content,
                        mailProvider
                    );
                    
                    log.info("체크리스트 알림 발송 완료 (실시간+이메일) - 사용자: {}, 항목: {}", user.getUserId(), checklist.getTitle());
                }
            }
        } catch (Exception e) {
            log.error("체크리스트 알림 체크 중 오류 발생 - 사용자: {}, 오류: {}", user.getUserId(), e.getMessage(), e);
        }
    }

    private String getMailProviderByUser(User user) {
        return switch (user.getProvider()) {
            case NAVER -> "naver";
            default -> "gmail";
        };
    }
}
