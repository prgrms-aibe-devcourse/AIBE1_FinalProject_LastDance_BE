package store.lastdance.service.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.*;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
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

    private final CalendarRepository calendarRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Calendar createCalendar(CreateCalendarRequestDTO request, UUID userId) {
        log.info("일정 생성 - 사용자: {}, 제목: {}", userId, request.getTitle());

        validateCalendarRequest(request);

        if (request.getType().equals(CalendarType.GROUP) && request.getGroupId() != null) {
            if (!isGroupMember(request.getGroupId(), userId)) {
                throw new IllegalArgumentException("해당 그룹에 일정을 생성할 권한이 없습니다.");
            }
        }

        Calendar calendar = Calendar.builder()
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

        Calendar savedCalendar = calendarRepository.save(calendar);
        log.info("일정 생성 완료 - ID: {}", savedCalendar.getCalendarId());

        return savedCalendar;
    }

    @Override
    public List<Calendar> getCalendarsByUser(UUID userId,
                                             String viewType,
                                             LocalDateTime dateTime,
                                             String type,
                                             String category,
                                             UUID groupId,
                                             Pageable pageable) {

        log.info("사용자 일정 조회 - 사용자: {}, 달력 종류: {}", userId, viewType);

        DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);

        // 1. 내가 만든 일정들 조회 (개인 + 그룹 일정)
        List<Calendar> myCalendars;
        if (dateTime != null) {
            myCalendars = calendarRepository.findByUserIdAndDateRange(userId, dateRange.start(), dateRange.end());
        } else {
            myCalendars = calendarRepository.findByUserId(userId);
        }

        List<Calendar> allCalendars = new ArrayList<>(myCalendars);
        log.info("내가 만든 일정 수: {}", myCalendars.size());

        // 2. 내가 속한 그룹들의 다른 멤버가 만든 일정들 조회
        try {
            List<Calendar> groupCalendars;
            if (dateTime != null) {
                groupCalendars = calendarRepository.findGroupCalendarsForUser(userId, dateRange.start(), dateRange.end());
            } else {
                groupCalendars = calendarRepository.findAllGroupCalendarsForUser(userId);
            }

            // 중복 제거 (내가 만든 그룹 일정은 이미 위에서 조회됨)
            List<Calendar> uniqueGroupCalendars = groupCalendars.stream()
                    .filter(calendar -> !calendar.getUserId().equals(userId))
                    .toList();

            allCalendars.addAll(uniqueGroupCalendars);
            log.info("그룹의 다른 멤버가 만든 일정 수: {}", uniqueGroupCalendars.size());
        } catch (Exception e) {
            log.warn("그룹 일정 조회 중 오류 발생: {}", e.getMessage());
        }

        List<Calendar> allInstances = expandRecurringCalendars(allCalendars, dateRange.start(), dateRange.end());

        // 추가 필터링 적용
        allInstances = applyFilters(allInstances, type, category, groupId);

        log.info("총 조회된 일정 수: {} (반복 일정 확장 포함)", allInstances.size());
        return allInstances;
    }

    @Override
    public Calendar getCalendarById(Long calendarId, UUID userId) {
        log.info("일정 상세 조회 - 일정 ID: {}, 사용자: {}", calendarId, userId);

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + calendarId));

        if (!hasCalendarAccess(calendar, userId)) {
            throw new IllegalArgumentException("해당 일정에 접근할 권한이 없습니다.");
        }

        return calendar;
    }

    @Override
    @Transactional
    public Calendar updateCalendar(Long calendarId, UpdateCalendarRequestDTO request, UUID userId) {
        log.info("일정 수정 - 일정 ID: {}, 사용자: {}", calendarId, userId);

        Calendar calendar = getCalendarById(calendarId, userId);

        if (!hasPermission(calendar, userId)) {
            throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
        }

        if (request.getTitle() != null) {
            calendar.updateTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            calendar.updateDescription(request.getDescription());
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            calendar.updateDateTime(request.getStartDate(), request.getEndDate());
        } else if (request.getStartDate() != null) {
            calendar.updateDateTime(request.getStartDate(), calendar.getEndDate());
        } else if (request.getEndDate() != null) {
            calendar.updateDateTime(calendar.getStartDate(), request.getEndDate());
        }
        if (request.getIsAllDay() != null) {
            calendar.updateAllDay(request.getIsAllDay());
        }
        if (request.getCategory() != null) {
            calendar.updateCategory(request.getCategory());
        }
        if (request.getGroupId() != null) {
            calendar.updateGroupId(request.getGroupId());
        }
        if (request.getRepeatType() != null) {
            if (request.getRepeatType() == RepeatType.NONE) {
                calendar.removeRepeat();
            } else {
                calendar.setAsRepeating(request.getRepeatType(), request.getRepeatEndDate());
            }
        } else if (request.getRepeatEndDate() != null) {
            // 반복 종료일만 수정하는 경우
            calendar.updateRepeatEndDate(request.getRepeatEndDate());
        }

        validateCalendarData(calendar);

        Calendar updatedCalendar = calendarRepository.save(calendar);
        log.info("일정 수정 완료 - ID: {}", updatedCalendar.getCalendarId());

        return updatedCalendar;
    }

    @Override
    @Transactional
    public void deleteCalendar(Long calendarId, LocalDateTime instanceDate, UUID userId) {
        log.info("일정 삭제 - 일정 ID: {}, 사용자: {}", calendarId, userId);

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. ID: " + calendarId));

        if (!hasPermission(calendar, userId)) {
            throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
        }

        calendarRepository.delete(calendar);
        log.info("일정 삭제 완료 - ID: {}", calendarId);

    }

    @Override
    public List<Calendar> getCalendarsByGroup(UUID groupId, String viewType, LocalDateTime dateTime, String type, String category, Pageable pageable) {
        log.info("그룹 일정 조회 (뷰타입) - 그룹 ID: {}, 뷰타입: {}, 기준날짜: {}", groupId, viewType, dateTime);

        DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);

        List<Calendar> baseCalendars = calendarRepository.findByGroupIdAndDateRange(
                groupId, dateRange.start(), dateRange.end());

        List<Calendar> allInstances = expandRecurringCalendars(baseCalendars, dateRange.start(), dateRange.end());

        // 추가 필터링 적용
        allInstances = applyFilters(allInstances, type, category, null);

        log.info("조회된 그룹 일정 수: {} (반복 일정 확장 포함)", allInstances.size());
        return allInstances;
    }

    @Override
    public List<Calendar> getCalendarsByGroup(UUID groupId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("그룹 일정 조회 (날짜 범위) - 그룹 ID: {}, 기간: {} ~ {}", groupId, startDate, endDate);

        List<Calendar> baseCalendars;
        if (startDate != null && endDate != null) {
            baseCalendars = calendarRepository.findByGroupIdAndDateRange(groupId, startDate, endDate);
        } else {
            baseCalendars = calendarRepository.findByGroupId(groupId);
        }

        if (startDate != null && endDate != null) {
            return expandRecurringCalendars(baseCalendars, startDate, endDate);
        } else {
            // 전체 조회시에는 현재 시점 기준으로 1년치만 확장
            LocalDateTime now = LocalDateTime.now();
            return expandRecurringCalendars(baseCalendars, now, now.plusYears(1));
        }
    }

    @Override
    public List<Calendar> getCalendarsByGroupWithViewType(UUID groupId, String viewType, LocalDateTime dateTime,
                                                          String type, String category, Pageable pageable) {
        return getCalendarsByGroup(groupId, viewType, dateTime, type, category, pageable);
    }

    @Override
    public List<Calendar> getRecurringInstances(Long calendarId,
                                                LocalDateTime startDate,
                                                LocalDateTime endDate,
                                                UUID userId) {
        log.info("반복 일정 인스턴스 조회 - 일정 ID: {}, 기간: {} ~ {}", calendarId, startDate, endDate);

        Calendar calendar = getCalendarById(calendarId, userId);

        if (calendar.getRepeatType() == RepeatType.NONE) {
            return List.of(calendar);
        }

        return generateRecurringInstances(calendar, startDate, endDate);
    }

    @Override
    public boolean isGroupMember(UUID groupId, UUID userId) {
        log.debug("그룹 멤버 권한 확인 - 그룹 ID: {}, 사용자: {}", groupId, userId);

        try {
            boolean isOwner = groupRepository.existsByGroupIdAndOwnerId(groupId, userId);
            boolean isMember = groupRepository.existsByGroupIdAndMemberId(groupId, userId);

            boolean hasAccess = isOwner || isMember;
            log.debug("그룹 접근 권한 - 소유자: {}, 멤버: {}, 결과: {}", isOwner, isMember, hasAccess);

            return hasAccess;
        } catch (Exception e) {
            log.error("그룹 멤버 권한 확인 중 오류 발생 - 그룹 ID: {}, 사용자: {}, 오류: {}",
                    groupId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * 일정 요청 데이터 검증
     */
    private void validateCalendarRequest(CreateCalendarRequestDTO request) {
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
    private void validateCalendarData(Calendar calendar) {
        if (calendar.getStartDate().isAfter(calendar.getEndDate())) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
    }

    /**
     * 일정 접근 권한 확인
     */
    private boolean hasCalendarAccess(Calendar calendar, UUID userId) {
        if (calendar.getUserId().equals(userId)) {
            return true;
        }

        if (calendar.getType() == CalendarType.GROUP && calendar.getGroupId() != null) {
            return isGroupMember(calendar.getGroupId(), userId);
        }
        return false;
    }

    /**
     * 반복 일정들을 실제 인스턴스로 확장
     */
    private List<Calendar> expandRecurringCalendars(List<Calendar> baseCalendars,
                                                    LocalDateTime rangeStart,
                                                    LocalDateTime rangeEnd) {
        List<Calendar> allInstances = new ArrayList<>();

        for (Calendar baseCalendar : baseCalendars) {
            if (baseCalendar.getRepeatType() == null || baseCalendar.getRepeatType() == RepeatType.NONE) {
                // 반복 일정이 아니면 그대로 추가
                allInstances.add(baseCalendar);
            } else {
                // 반복 일정이면 인스턴스들 생성해서 추가
                List<Calendar> instances = generateRecurringInstances(baseCalendar, rangeStart, rangeEnd);

                allInstances.addAll(instances);
            }
        }

        // 시작 시간순으로 정렬
        allInstances.sort(Comparator.comparing(Calendar::getStartDate));

        return allInstances;
    }

    /**
     * 반복 일정 인스턴스 생성
     */
    private List<Calendar> generateRecurringInstances(Calendar baseCalendar,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        log.info("반복 일정 인스턴스 생성 - 기본 일정 ID: {}, 반복 타입: {}, 조회 범위: {} ~ {}",
                baseCalendar.getCalendarId(), baseCalendar.getRepeatType(), startDate, endDate);

        List<Calendar> instances = new ArrayList<>();

        if (baseCalendar.getRepeatType() == null || baseCalendar.getRepeatType() == RepeatType.NONE) {
            instances.add(baseCalendar);
            return instances;
        }

        LocalDateTime current = baseCalendar.getStartDate();
        LocalDateTime repeatEnd = baseCalendar.getRepeatEndDate();
        Duration duration = Duration.between(baseCalendar.getStartDate(), baseCalendar.getEndDate());

        // 반복 종료일과 조회 범위 중 더 이른 날짜까지만 생성
        LocalDateTime actualEnd = (repeatEnd != null && repeatEnd.isBefore(endDate)) ? repeatEnd : endDate;

        // 안전장치: 최대 1000개 인스턴스만 생성 (무한 루프 방지)
        int maxInstances = 1000;
        int instanceCount = 0;

        while (!current.isAfter(actualEnd) && instanceCount < maxInstances) {
            // 조회 범위 안에 있으면 인스턴스 생성
            if (!current.isBefore(startDate)) {
                Calendar instance = createInstanceFromBase(baseCalendar, current, duration);
                instances.add(instance);
                instanceCount++;
            }

            // 다음 반복 날짜 계산
            current = calculateNextOccurrence(current, baseCalendar.getRepeatType());

            // 무한 루프 방지: 다음 날짜가 현재보다 이전이면 중단
            if (!current.isAfter(baseCalendar.getStartDate().plus(Duration.ofDays((long) instanceCount * 400)))) {
                log.warn("반복 일정 계산 중 무한 루프 감지, 중단 - 일정 ID: {}", baseCalendar.getCalendarId());
                break;
            }
        }

        log.info("반복 일정 인스턴스 생성 완료 - 총 {}개 인스턴스", instances.size());
        return instances;
    }

    /**
     * 기본 일정을 바탕으로 새로운 인스턴스 생성
     */
    private Calendar createInstanceFromBase(Calendar base, LocalDateTime newStartDate, Duration duration) {
        Calendar instance = Calendar.builder()
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
            java.lang.reflect.Field field = Calendar.class.getDeclaredField("calendarId");
            field.setAccessible(true);
            field.set(instance, base.getCalendarId());
        } catch (Exception e) {
            log.debug("calendarId 설정 실패, 원본 ID 없이 진행: {}", e.getMessage());
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
            case YEARLY -> current.plusYears(1);
            default -> {
                log.warn("알 수 없는 반복 타입: {}", repeatType);
                yield current.plusDays(1); // 기본값: 하루 후
            }
        };
    }

    /**
     * 일정 목록에 필터 적용
     */
    private List<Calendar> applyFilters(List<Calendar> calendars, String type, String category, UUID groupId) {
        List<Calendar> filteredCalendars = calendars;

        if (type != null) {
            CalendarType calendarType = CalendarType.valueOf(type.toUpperCase());
            filteredCalendars = filteredCalendars.stream()
                    .filter(calendar -> calendar.getType() == calendarType)
                    .toList();
        }

        if (category != null) {
            CalendarCategory calendarCategory = CalendarCategory.valueOf(category.toUpperCase());
            filteredCalendars = filteredCalendars.stream()
                    .filter(calendar -> calendar.getCategory() == calendarCategory)
                    .toList();
        }

        if (groupId != null) {
            filteredCalendars = filteredCalendars.stream()
                    .filter(calendar -> groupId.equals(calendar.getGroupId()))
                    .toList();
        }

        return filteredCalendars;
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
                // 캘린더 그리드에 맞는 날짜 범위 (이전달 마지막주 ~ 다음달 첫주)
                LocalDate monthStart = baseDate.toLocalDate().withDayOfMonth(1);
                LocalDate monthEnd = baseDate.toLocalDate().withDayOfMonth(baseDate.toLocalDate().lengthOfMonth());

                // 해당 월 1일이 포함된 주의 시작일 (일요일)
                LocalDate calendarStart = monthStart.with(DayOfWeek.SUNDAY);
                if (calendarStart.isAfter(monthStart)) {
                    calendarStart = calendarStart.minusWeeks(1);
                }

                // 해당 월 말일이 포함된 주의 종료일 (토요일)
                LocalDate calendarEnd = monthEnd.with(DayOfWeek.SATURDAY);
                if (calendarEnd.isBefore(monthEnd)) {
                    calendarEnd = calendarEnd.plusWeeks(1);
                }

                yield new DateRangeDTO(
                        calendarStart.atStartOfDay(),
                        calendarEnd.atTime(23, 59, 59)
                );
            }

            case "YEARLY" -> {
                // 연간 (해당 년도 1월 1일 ~ 12월 31일)
                LocalDateTime startOfYear = baseDate.toLocalDate()
                        .withDayOfYear(1)
                        .atStartOfDay();
                LocalDateTime endOfYear = baseDate.toLocalDate()
                        .withDayOfYear(baseDate.toLocalDate().lengthOfYear())
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfYear, endOfYear);
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

    private boolean hasPermission(Calendar calendar, UUID userId) {
        return switch (calendar.getType()) {
            case PERSONAL -> {
                // 개인 일정: 작성자만 수정 가능
                log.debug("개인 일정 수정 권한 확인 - 작성자: {}, 요청자: {}",
                        calendar.getUserId(), userId);
                yield calendar.getUserId().equals(userId);
            }

            case GROUP -> {
                // 그룹 일정: 작성자 또는 그룹 멤버가 수정 가능
                log.debug("그룹 일정 수정 권한 확인 - 작성자: {}, 요청자: {}, 그룹: {}",
                        calendar.getUserId(), userId, calendar.getGroupId());

                // 작성자인 경우
                if (calendar.getUserId().equals(userId)) {
                    yield true;
                }

                // 그룹 멤버인 경우 (그룹 ID가 있을 때만)
                if (calendar.getGroupId() != null) {
                    yield isGroupMember(calendar.getGroupId(), userId);
                }

                yield false;
            }
        };
    }
}
