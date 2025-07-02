package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.notification.NotificationCacheRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.MailService;
import store.lastdance.service.notification.NotificationSettingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationCacheRepository notificationCacheRepository;
    private final MailService mailService;
    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationSettingService notificationSettingService;

    @Scheduled(fixedRate = 120000) // 2분마다 실행
    public void processScheduledNotifications() {
        log.info("=== 알림 스케줄러 실행 시작 ===");

        try {
            // 이메일 알림이 허용된 사용자들 조회
            List<User> enabledUsers = notificationSettingService.emailPermitted();
            log.info("이메일 알림 허용 사용자 수: {}", enabledUsers.size());

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

    private void checkAndSendNotifications(User user) {
        // 사용자의 알림 설정 조회
        NotificationSetting setting = settingRepository.findByUserId(user.getUserId());
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
                    // 사용자의 OAuth Provider에 따라 메일 서비스 선택
                    String mailProvider = getMailProviderByUser(user);
                    
                    String scheduleTypeText = schedule.getType() == CalendarType.GROUP ? "[그룹] " : "";
                    log.info("이메일 발송 시도 - 수신자: {}, 일정: {}{}, 발송서비스: {}", 
                        user.getEmail(), scheduleTypeText, schedule.getTitle(), mailProvider.toUpperCase());
                    
                    // 이메일 발송
                    mailService.sendScheduleReminder(
                        user.getEmail(), 
                        scheduleTypeText + schedule.getTitle(), 
                        "15분 후 시작 예정입니다.",
                        mailProvider
                    );
                    
                    // 발송 기록 저장 (사용자별로)
                    NotificationCache cache = NotificationCache.create(
                        user.getUserId(),
                        NotificationType.SCHEDULE,
                        schedule.getTitle(),
                        "일정 알림이 발송되었습니다.",
                        schedule.getCalendarId().toString()
                    );
                    notificationCacheRepository.save(cache);
                    
                    log.info("일정 알림 발송 완료 - 사용자: {}, 일정: {}{}, 서비스: {}", 
                        user.getUserId(), scheduleTypeText, schedule.getTitle(), mailProvider.toUpperCase());
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
                log.info("미정산 분담금 발견 - ID: {}, 지출ID: {}, 금액: {}, 생성일: {}", 
                    split.getSplitId(), split.getExpenseId(), split.getAmount(), split.getCreatedAt());
                
                String cacheKey = NotificationCache.generateKey(
                    user.getUserId(), NotificationType.PAYMENT, split.getSplitId().toString());
                
                boolean alreadySent = notificationCacheRepository.existsById(cacheKey);
                log.info("정산 알림 발송 이력 체크 - 캐시키: {}, 이미 발송됨: {}", cacheKey, alreadySent);
                
                if (!alreadySent) {
                    String mailProvider = getMailProviderByUser(user);
                    String expenseTitle = split.getExpense() != null ? split.getExpense().getTitle() : "그룹 지출";
                    log.info("정산 이메일 발송 시도 - 수신자: {}, 지출: {}, 분담금: {}, 발송서비스: {}", 
                        user.getEmail(), expenseTitle, split.getAmount(), mailProvider.toUpperCase());
                    
                    mailService.sendPaymentReminder(
                        user.getEmail(),
                        expenseTitle + " (분담금: " + split.getAmount() + "원)",
                        "새로운 정산 요청이 있습니다.",
                        mailProvider
                    );
                    
                    NotificationCache cache = NotificationCache.create(
                        user.getUserId(),
                        NotificationType.PAYMENT,
                        expenseTitle,
                        "정산 알림이 발송되었습니다.",
                        split.getSplitId().toString()
                    );
                    notificationCacheRepository.save(cache);
                    
                    log.info("정산 알림 발송 완료 - 사용자: {}, 항목: {}, 분담금: {}", 
                        user.getUserId(), expenseTitle, split.getAmount());
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
                    String mailProvider = getMailProviderByUser(user);
                    log.info("체크리스트 이메일 발송 시도 - 수신자: {}, 체크리스트: {}, 발송서비스: {}", 
                        user.getEmail(), checklist.getTitle(), mailProvider.toUpperCase());
                    
                    mailService.sendChecklistReminder(
                        user.getEmail(),
                        checklist.getTitle(),
                        "오늘이 마감일입니다.",
                        mailProvider
                    );
                    
                    NotificationCache cache = NotificationCache.create(
                        user.getUserId(),
                        NotificationType.CHECKLIST,
                        checklist.getTitle(),
                        "체크리스트 알림이 발송되었습니다.",
                        checklist.getChecklistId().toString()
                    );
                    notificationCacheRepository.save(cache);
                    
                    log.info("체크리스트 알림 발송 완료 - 사용자: {}, 항목: {}", user.getUserId(), checklist.getTitle());
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
