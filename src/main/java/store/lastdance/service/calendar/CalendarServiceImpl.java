package store.lastdance.service.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.*;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.calender.request.CreateScheduleRequestDTO;
import store.lastdance.dto.calender.request.UpdateScheduleRequestDTO;
import store.lastdance.repository.calendar.ScheduleRepository;
import store.lastdance.repository.calendar.ScheduleExceptionRepository;
import store.lastdance.repository.group.GroupRepository;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Schedule createSchedule(CreateScheduleRequestDTO request, UUID userId) {
        log.info("일정 생성 - 사용자: {}, 제목: {}", userId, request.getTitle());

        // 입력값 검증
        validateScheduleRequest(request);

        // 그룹 일정인 경우 그룹 멤버 권한 확인
        if (request.getType() == ScheduleType.GROUP && request.getGroupId() != null) {
            if (!isGroupMember(request.getGroupId(), userId)) {
                throw new IllegalArgumentException("해당 그룹에 일정을 생성할 권한이 없습니다.");
            }
        }

        // Schedule 엔티티 생성
        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isAllDay(request.getIsAllDay())
                .type(request.getType())
                .category(request.getCategory())
                .groupId(request.getGroupId())
                .userId(userId)
                .repeatType(request.getRepeatType())
                .repeatEndDate(request.getRepeatEndDate())
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("일정 생성 완료 - ID: {}", savedSchedule.getScheduleId());

        return savedSchedule;
    }

    @Override
    public List<Schedule> getSchedulesByUser(UUID userId, 
                                           String viewType,
                                           LocalDateTime dateTime,
                                           String type, 
                                           String category, 
                                           UUID groupId, 
                                           Pageable pageable) {
        
        log.info("사용자 일정 조회 - 사용자: {}, 달력 종류: {}", userId, viewType);

        // 날짜 범위 계산
        DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);
        
        // 기본 일정들 조회 (반복 일정 포함)
        List<Schedule> baseSchedules;
        if (dateTime != null) {
            baseSchedules = scheduleRepository.findByUserIdAndDateRange(userId, dateRange.start(), dateRange.end());
        } else {
            baseSchedules = scheduleRepository.findByUserId(userId);
        }

        // 반복 일정을 실제 인스턴스로 확장
        List<Schedule> allInstances = expandRecurringSchedules(baseSchedules, dateRange.start(), dateRange.end());

        // 추가 필터링
        if (type != null) {
            ScheduleType scheduleType = ScheduleType.valueOf(type.toUpperCase());
            allInstances = allInstances.stream()
                    .filter(schedule -> schedule.getType() == scheduleType)
                    .toList();
        }

        if (category != null) {
            ScheduleCategory scheduleCategory = ScheduleCategory.valueOf(category.toUpperCase());
            allInstances = allInstances.stream()
                    .filter(schedule -> schedule.getCategory() == scheduleCategory)
                    .toList();
        }

        if (groupId != null) {
            allInstances = allInstances.stream()
                    .filter(schedule -> groupId.equals(schedule.getGroupId()))
                    .toList();
        }

        log.info("조회된 일정 수: {} (반복 일정 확장 포함)", allInstances.size());
        return allInstances;
    }

    @Override
    public Schedule getScheduleById(Long scheduleId, UUID userId) {
        log.info("일정 상세 조회 - 일정 ID: {}, 사용자: {}", scheduleId, userId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + scheduleId));

        // 권한 확인
        if (!hasScheduleAccess(schedule, userId)) {
            throw new IllegalArgumentException("해당 일정에 접근할 권한이 없습니다.");
        }

        return schedule;
    }

    @Override
    @Transactional
    public Schedule updateSchedule(Long scheduleId, UpdateScheduleRequestDTO request, UUID userId) {
        log.info("일정 수정 - 일정 ID: {}, 사용자: {}", scheduleId, userId);

        Schedule schedule = getScheduleById(scheduleId, userId);

        // 수정 권한 확인
        if (!hasPermission(schedule, userId)) {
            throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
        }

        // 필드별 업데이트 (엔티티의 업데이트 메서드 사용)
        if (request.getTitle() != null) {
            schedule.updateTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            schedule.updateDescription(request.getDescription());
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            schedule.updateDateTime(request.getStartDate(), request.getEndDate());
        } else if (request.getStartDate() != null) {
            schedule.updateDateTime(request.getStartDate(), schedule.getEndDate());
        } else if (request.getEndDate() != null) {
            schedule.updateDateTime(schedule.getStartDate(), request.getEndDate());
        }
        if (request.getIsAllDay() != null) {
            schedule.updateAllDay(request.getIsAllDay());
        }
        if (request.getType() != null) {
            schedule.updateType(request.getType());
        }
        if (request.getCategory() != null) {
            schedule.updateCategory(request.getCategory());
        }
        if (request.getGroupId() != null) {
            schedule.updateGroupId(request.getGroupId());
        }
        if (request.getRepeatType() != null) {
            if (request.getRepeatType() == RepeatType.NONE) {
                schedule.removeRepeat();
            } else {
                schedule.setAsRepeating(request.getRepeatType(), request.getRepeatEndDate());
            }
        } else if (request.getRepeatEndDate() != null) {
            // 반복 종료일만 수정하는 경우
            schedule.updateRepeatEndDate(request.getRepeatEndDate());
        }

        // 수정된 데이터 검증
        validateScheduleData(schedule);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("일정 수정 완료 - ID: {}", updatedSchedule.getScheduleId());

        return updatedSchedule;
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, String deleteType, LocalDateTime instanceDate, UUID userId) {
        log.info("일정 삭제 - 일정 ID: {}, 삭제 타입: {}, 사용자: {}", scheduleId, deleteType, userId);

        Schedule schedule = getScheduleById(scheduleId, userId);

        // 삭제 권한 확인 (소유자만 삭제 가능)
        if (!hasPermission(schedule, userId)) {
            throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
        }

        switch (deleteType.toLowerCase()) {
            case "single":
                // 단일 일정 삭제 (반복 일정의 특정 인스턴스)
                deleteSingleInstance(schedule, instanceDate);
                break;
            case "future":
                // 이후 모든 반복 일정 삭제
                deleteFutureInstances(schedule, instanceDate);
                break;
            case "all":
            default:
                // 모든 일정 삭제 (기본값)
                scheduleRepository.delete(schedule);
                log.info("일정 삭제 완료 - ID: {}", scheduleId);
                break;
        }
    }

    @Override
    public List<Schedule> getSchedulesByGroup(UUID groupId, String viewType, LocalDateTime dateTime, String type, String category, Pageable pageable) {
        log.info("그룹 일정 조회 (뷰타입) - 그룹 ID: {}, 뷰타입: {}, 기준날짜: {}", groupId, viewType, dateTime);

        DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);
        
        // 기본 그룹 일정들 조회 (반복 일정 포함)
        List<Schedule> baseSchedules = scheduleRepository.findByGroupIdAndDateRange(
            groupId, dateRange.start(), dateRange.end());

        // 반복 일정을 실제 인스턴스로 확장
        List<Schedule> allInstances = expandRecurringSchedules(baseSchedules, dateRange.start(), dateRange.end());

        // 추가 필터링
        if (type != null) {
            ScheduleType scheduleType = ScheduleType.valueOf(type.toUpperCase());
            allInstances = allInstances.stream()
                    .filter(schedule -> schedule.getType() == scheduleType)
                    .toList();
        }

        if (category != null) {
            ScheduleCategory scheduleCategory = ScheduleCategory.valueOf(category.toUpperCase());
            allInstances = allInstances.stream()
                    .filter(schedule -> schedule.getCategory() == scheduleCategory)
                    .toList();
        }

        log.info("조회된 그룹 일정 수: {} (반복 일정 확장 포함)", allInstances.size());
        return allInstances;
    }
    
    @Override
    public List<Schedule> getSchedulesByGroup(UUID groupId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("그룹 일정 조회 (날짜 범위) - 그룹 ID: {}, 기간: {} ~ {}", groupId, startDate, endDate);

        List<Schedule> baseSchedules;
        if (startDate != null && endDate != null) {
            baseSchedules = scheduleRepository.findByGroupIdAndDateRange(groupId, startDate, endDate);
        } else {
            baseSchedules = scheduleRepository.findByGroupId(groupId);
        }

        // 반복 일정을 실제 인스턴스로 확장
        if (startDate != null && endDate != null) {
            return expandRecurringSchedules(baseSchedules, startDate, endDate);
        } else {
            // 전체 조회시에는 현재 시점 기준으로 1년치만 확장
            LocalDateTime now = LocalDateTime.now();
            return expandRecurringSchedules(baseSchedules, now, now.plusYears(1));
        }
    }
    
    @Override
    public List<Schedule> getSchedulesByGroupWithViewType(UUID groupId, String viewType, LocalDateTime dateTime, 
                                                         String type, String category, Pageable pageable) {
        return getSchedulesByGroup(groupId, viewType, dateTime, type, category, pageable);
    }

    @Override
    public List<Schedule> getRecurringInstances(Long scheduleId, 
                                              LocalDateTime startDate, 
                                              LocalDateTime endDate, 
                                              UUID userId) {
        log.info("반복 일정 인스턴스 조회 - 일정 ID: {}, 기간: {} ~ {}", scheduleId, startDate, endDate);

        Schedule schedule = getScheduleById(scheduleId, userId);

        if (schedule.getRepeatType() == RepeatType.NONE) {
            // 반복 일정이 아닌 경우 원본만 반환
            return List.of(schedule);
        }

        // 반복 일정 인스턴스 생성 로직
        return generateRecurringInstances(schedule, startDate, endDate);
    }

    @Override
    public boolean isGroupMember(UUID groupId, UUID userId) {
        log.debug("그룹 멤버 권한 확인 - 그룹 ID: {}, 사용자: {}", groupId, userId);
        
        // 실제로는 GroupMember 테이블을 조회해야 함
        // 임시로 true 반환 (실제 구현 시 수정 필요)
        return groupRepository.existsByGroupIdAndMemberId(groupId, userId);
    }

    /**
     * 일정 요청 데이터 검증
     */
    private void validateScheduleRequest(CreateScheduleRequestDTO request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }

        if (request.getRepeatType() != RepeatType.NONE && request.getRepeatEndDate() != null) {
            if (request.getRepeatEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("반복 종료일이 시작일보다 이를 수 없습니다.");
            }
        }
    }

    /**
     * 일정 데이터 검증
     */
    private void validateScheduleData(Schedule schedule) {
        if (schedule.getStartDate().isAfter(schedule.getEndDate())) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
    }

    /**
     * 일정 접근 권한 확인
     */
    private boolean hasScheduleAccess(Schedule schedule, UUID userId) {
        // 본인이 만든 일정
        if (schedule.getUserId().equals(userId)) {
            return true;
        }

        // 그룹 일정이고 해당 그룹의 멤버인 경우
        if (schedule.getType() == ScheduleType.GROUP && schedule.getGroupId() != null) {
            return isGroupMember(schedule.getGroupId(), userId);
        }

        return false;
    }

    /**
     * 단일 인스턴스 삭제 (반복 일정의 특정 날짜만)
     */
    private void deleteSingleInstance(Schedule schedule, LocalDateTime instanceDate) {
        log.info("단일 인스턴스 삭제 시작 - 일정 ID: {}, 인스턴스 날짜: {}", 
                schedule.getScheduleId(), instanceDate);
        
        // 입력값 검증
        if (instanceDate == null) {
            throw new IllegalArgumentException("인스턴스 날짜가 필요합니다.");
        }
        
        // 반복 일정이 아닌 경우 전체 일정 삭제
        if (schedule.getRepeatType() == RepeatType.NONE) {
            log.info("반복 일정이 아니므로 전체 일정을 삭제합니다 - ID: {}", schedule.getScheduleId());
            scheduleRepository.delete(schedule);
            return;
        }
        
        // 이미 해당 날짜에 예외가 있는지 확인
        if (scheduleExceptionRepository.existsByScheduleIdAndExceptionDate(
                schedule.getScheduleId(), instanceDate)) {
            log.warn("이미 삭제된 인스턴스입니다 - 일정 ID: {}, 날짜: {}", 
                    schedule.getScheduleId(), instanceDate);
            throw new IllegalArgumentException("이미 삭제된 인스턴스입니다.");
        }
        
        // 예외 날짜로 기록하여 해당 인스턴스를 숨김 처리
        ScheduleException exception = ScheduleException.builder()
                .scheduleId(schedule.getScheduleId())
                .exceptionDate(instanceDate)
                .exceptionType(ExceptionType.DELETED)
                .build();
        
        scheduleExceptionRepository.save(exception);
        
        log.info("단일 인스턴스 삭제 완료 - 일정 ID: {}, 예외 날짜 기록: {}", 
                schedule.getScheduleId(), instanceDate);
    }

    /**
     * 이후 인스턴스 삭제 (특정 날짜 이후의 모든 반복 일정)
     */
    private void deleteFutureInstances(Schedule schedule, LocalDateTime fromDate) {
        log.info("이후 인스턴스 삭제 시작 - 일정 ID: {}, 기준 날짜: {}", schedule.getScheduleId(), fromDate);
        
        // 비즈니스 로직이 포함된 메서드 사용
        schedule.stopRepeatingAfter(fromDate);
        
        scheduleRepository.save(schedule);
        log.info("이후 인스턴스 삭제 완료 - 일정 ID: {}", schedule.getScheduleId());
    }

    /**
     * 반복 일정들을 실제 인스턴스로 확장
     */
    private List<Schedule> expandRecurringSchedules(List<Schedule> baseSchedules, 
                                                   LocalDateTime rangeStart, 
                                                   LocalDateTime rangeEnd) {
        List<Schedule> allInstances = new ArrayList<>();
        
        for (Schedule baseSchedule : baseSchedules) {
            if (baseSchedule.getRepeatType() == null || baseSchedule.getRepeatType() == RepeatType.NONE) {
                // 반복 일정이 아니면 그대로 추가
                allInstances.add(baseSchedule);
            } else {
                // 반복 일정이면 인스턴스들 생성해서 추가
                List<Schedule> instances = generateRecurringInstances(baseSchedule, rangeStart, rangeEnd);
                
                // 예외 날짜(삭제된 날짜들) 제외
                List<Schedule> filteredInstances = filterExceptionDates(instances, baseSchedule.getScheduleId());
                allInstances.addAll(filteredInstances);
            }
        }
        
        // 시작 시간순으로 정렬
        allInstances.sort(Comparator.comparing(Schedule::getStartDate));
        
        return allInstances;
    }

    /**
     * 예외 날짜(삭제된 인스턴스)들을 제외한 인스턴스 반환
     */
    private List<Schedule> filterExceptionDates(List<Schedule> instances, Long scheduleId) {
        // 해당 일정의 삭제된 예외 날짜들 조회
        List<ScheduleException> exceptions = scheduleExceptionRepository.findByScheduleId(scheduleId);
        
        // 삭제된 날짜들만 추출
        List<LocalDateTime> deletedDates = exceptions.stream()
            .filter(exception -> exception.getExceptionType() == ExceptionType.DELETED)
            .map(ScheduleException::getExceptionDate)
            .toList();
        
        if (deletedDates.isEmpty()) {
            return instances; // 삭제된 예외가 없으면 그대로 반환
        }
        
        // 삭제된 날짜에 해당하는 인스턴스들 제외
        List<Schedule> filteredInstances = instances.stream()
            .filter(instance -> !deletedDates.contains(instance.getStartDate()))
            .toList();
        
        log.debug("삭제된 예외 날짜 필터링 완료 - 원본: {}개, 필터링 후: {}개, 삭제된 날짜: {}개", 
                 instances.size(), filteredInstances.size(), deletedDates.size());
        
        return filteredInstances;
    }

    /**
     * 반복 일정 인스턴스 생성
     */
    private List<Schedule> generateRecurringInstances(Schedule baseSchedule, 
                                                     LocalDateTime startDate, 
                                                     LocalDateTime endDate) {
        log.info("반복 일정 인스턴스 생성 - 기본 일정 ID: {}, 반복 타입: {}, 조회 범위: {} ~ {}", 
                baseSchedule.getScheduleId(), baseSchedule.getRepeatType(), startDate, endDate);
        
        List<Schedule> instances = new ArrayList<>();
        
        // 반복 일정이 아니면 원본만 반환
        if (baseSchedule.getRepeatType() == null || baseSchedule.getRepeatType() == RepeatType.NONE) {
            instances.add(baseSchedule);
            return instances;
        }
        
        LocalDateTime current = baseSchedule.getStartDate();
        LocalDateTime repeatEnd = baseSchedule.getRepeatEndDate();
        Duration duration = Duration.between(baseSchedule.getStartDate(), baseSchedule.getEndDate());
        
        // 반복 종료일과 조회 범위 중 더 이른 날짜까지만 생성
        LocalDateTime actualEnd = (repeatEnd != null && repeatEnd.isBefore(endDate)) ? repeatEnd : endDate;
        
        // 안전장치: 최대 1000개 인스턴스만 생성 (무한 루프 방지)
        int maxInstances = 1000;
        int instanceCount = 0;
        
        while (!current.isAfter(actualEnd) && instanceCount < maxInstances) {
            // 조회 범위 안에 있으면 인스턴스 생성
            if (!current.isBefore(startDate)) {
                Schedule instance = createInstanceFromBase(baseSchedule, current, duration);
                instances.add(instance);
                instanceCount++;
            }
            
            // 다음 반복 날짜 계산
            current = calculateNextOccurrence(current, baseSchedule.getRepeatType());
            
            // 무한 루프 방지: 다음 날짜가 현재보다 이전이면 중단
            if (!current.isAfter(baseSchedule.getStartDate().plus(Duration.ofDays((long) instanceCount * 400)))) {
                log.warn("반복 일정 계산 중 무한 루프 감지, 중단 - 일정 ID: {}", baseSchedule.getScheduleId());
                break;
            }
        }
        
        log.info("반복 일정 인스턴스 생성 완료 - 총 {}개 인스턴스", instances.size());
        return instances;
    }
    
    /**
     * 기본 일정을 바탕으로 새로운 인스턴스 생성
     */
    private Schedule createInstanceFromBase(Schedule base, LocalDateTime newStartDate, Duration duration) {
        Schedule instance = Schedule.builder()
            .title(base.getTitle())
            .description(base.getDescription())
            .startDate(newStartDate)           // 새로운 시작 시간
            .endDate(newStartDate.plus(duration)) // 지속 시간 유지
            .isAllDay(base.getIsAllDay())
            .type(base.getType())
            .category(base.getCategory())
            .groupId(base.getGroupId())
            .userId(base.getUserId())
            .repeatType(base.getRepeatType())
            .repeatEndDate(base.getRepeatEndDate())
            .build();
            
        // 원본 ID를 리플렉션으로 설정하여 클라이언트에서 구분 가능하도록 함
        try {
            java.lang.reflect.Field field = Schedule.class.getDeclaredField("scheduleId");
            field.setAccessible(true);
            field.set(instance, base.getScheduleId());
        } catch (Exception e) {
            log.debug("scheduleId 설정 실패, 원본 ID 없이 진행: {}", e.getMessage());
        }
        
        return instance;
    }
    
    /**
     * 반복 타입에 따른 다음 발생 날짜 계산
     */
    private LocalDateTime calculateNextOccurrence(LocalDateTime current, RepeatType repeatType) {
        return switch (repeatType) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            default -> {
                log.warn("알 수 없는 반복 타입: {}", repeatType);
                yield current.plusDays(1); // 기본값: 하루 후
            }
        };
    }

    private DateRangeDTO calculateDateRange(String viewType, LocalDateTime baseDate) {
        return switch (viewType.toUpperCase()) {
            case "DAILY" -> {
                // 하루 (00:00:00 ~ 23:59:59)
                LocalDateTime startOfDay = baseDate.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = baseDate.toLocalDate().atTime(23, 59, 59);
                yield new DateRangeDTO(startOfDay, endOfDay);
            }

            case "WEEKLY" -> {
                // 주간 (해당 주의 월요일 ~ 일요일)
                LocalDateTime startOfWeek = baseDate.toLocalDate()
                        .with(DayOfWeek.MONDAY)
                        .atStartOfDay();
                LocalDateTime endOfWeek = baseDate.toLocalDate()
                        .with(DayOfWeek.SUNDAY)
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfWeek, endOfWeek);
            }

            case "MONTHLY" -> {
                // 월간 (해당 월의 1일 ~ 말일)
                LocalDateTime startOfMonth = baseDate.toLocalDate()
                        .withDayOfMonth(1)
                        .atStartOfDay();
                LocalDateTime endOfMonth = baseDate.toLocalDate()
                        .withDayOfMonth(baseDate.toLocalDate().lengthOfMonth())
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfMonth, endOfMonth);
            }

            default -> {
                // 기본값: 월간
                LocalDateTime startOfMonth = baseDate.toLocalDate()
                        .withDayOfMonth(1)
                        .atStartOfDay();
                LocalDateTime endOfMonth = baseDate.toLocalDate()
                        .withDayOfMonth(baseDate.toLocalDate().lengthOfMonth())
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfMonth, endOfMonth);
            }
        };
    }

    private boolean hasPermission(Schedule schedule, UUID userId) {
        return switch (schedule.getType()) {
            case PERSONAL -> {
                // 개인 일정: 작성자만 수정 가능
                log.debug("개인 일정 수정 권한 확인 - 작성자: {}, 요청자: {}",
                        schedule.getUserId(), userId);
                yield schedule.getUserId().equals(userId);
            }

            case GROUP -> {
                // 그룹 일정: 작성자 또는 그룹 멤버가 수정 가능
                log.debug("그룹 일정 수정 권한 확인 - 작성자: {}, 요청자: {}, 그룹: {}",
                        schedule.getUserId(), userId, schedule.getGroupId());

                // 작성자인 경우
                if (schedule.getUserId().equals(userId)) {
                    yield true;
                }

                // 그룹 멤버인 경우 (그룹 ID가 있을 때만)
                if (schedule.getGroupId() != null) {
                    yield isGroupMember(schedule.getGroupId(), userId);
                }

                yield false;
            }
        };
    }
}
