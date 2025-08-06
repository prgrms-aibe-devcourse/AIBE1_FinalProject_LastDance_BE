package store.lastdance.service.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.*;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calender.response.CalendarResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.group.GroupNameProjection;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.validation.calendar.CalendarValidator;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CalendarResponseDTO createCalendar(CreateCalendarRequestDTO request, UUID userId, UUID groupId) {
        CalendarValidator.validateCreateCalendar(request, groupId);

        CalendarType type = CalendarType.PERSONAL;
        Group group = null;
        if (groupId != null){
            boolean isMember = isGroupMember(groupId, userId);
            CalendarValidator.validateGroupMembership(groupId, isMember);
            type = CalendarType.GROUP;

            group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
        }

        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Calendar calendar = Calendar.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .isAllDay(request.getIsAllDay())
            .type(type)
            .category(request.getCategory())
            .group(group)
            .user(user)
            .repeatType(request.getRepeatType())
            .repeatEndDate(request.getRepeatEndDate())
            .build();

        Calendar savedCalendar = calendarRepository.save(calendar);

        String groupName = (group != null) ? group.getGroupName() : null;

        return CalendarResponseDTO.builder()
            .calendarId(savedCalendar.getCalendarId())
            .title(savedCalendar.getTitle())
            .description(savedCalendar.getDescription())
            .startDate(savedCalendar.getStartDate())
            .endDate(savedCalendar.getEndDate())
            .isAllDay(savedCalendar.getIsAllDay())
            .type(savedCalendar.getType())
            .category(savedCalendar.getCategory())
            .groupId(group != null ? group.getGroupId() : null)
            .groupName(groupName)
            .userId(user.getUserId())
            .repeatType(savedCalendar.getRepeatType())
            .repeatEndDate(savedCalendar.getRepeatEndDate())
            .createdAt(savedCalendar.getCreatedAt())
            .build();
    }

    @Override
    public List<CalendarResponseDTO> getCalendarsByUser(UUID userId,
                                                        String viewType,
                                                        LocalDateTime dateTime,
                                                        String type,
                                                        String category,
                                                        UUID groupId,
                                                        Pageable pageable) {
        try {
            DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);

            List<Calendar> myCalendars;
            if (dateTime != null) {
                myCalendars = calendarRepository.findByUserIdAndDateRange(userId, dateRange.start(), dateRange.end());
            } else {
                myCalendars = calendarRepository.findByUserId(userId);
            }

            List<Calendar> allCalendars = new ArrayList<>(myCalendars);

            List<Calendar> groupCalendars;
            if (dateTime != null) {
                groupCalendars = calendarRepository.findGroupCalendarsForUser(userId,
                    dateRange.start(), dateRange.end());
            } else {
                groupCalendars = calendarRepository.findAllGroupCalendarsForUser(userId);
            }

            List<Calendar> uniqueGroupCalendars = groupCalendars.stream()
                .filter(calendar -> !calendar.getUser().getUserId().equals(userId))
                .toList();

            allCalendars.addAll(uniqueGroupCalendars);

            List<Calendar> allInstances = expandRecurringCalendars(allCalendars, dateRange.start(),
                dateRange.end());

            allInstances = applyFilters(allInstances, type, category, groupId);

            List<Group> groups = allInstances.stream()
                .map(Calendar::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            Map<UUID, String> groupNameMap = new HashMap<>();
            if (!groups.isEmpty()) {
                List<UUID> groupIds = groups.stream()
                    .map(Group::getGroupId)
                    .distinct()
                    .toList();

                List<GroupNameProjection> groupNames = groupRepository.findGroupNamesByGroupIds(groupIds);
                groupNameMap = groupNames.stream()
                    .collect(Collectors.toMap(
                        GroupNameProjection::getGroupId,
                        GroupNameProjection::getGroupName
                    ));
            }

            final Map<UUID, String> finalGroupNameMap = groupNameMap;
            return allInstances.stream()
                .map(calendar -> {
                    UUID groupID = calendar.getGroup() != null ? calendar.getGroup().getGroupId() : null;
                    String groupName = finalGroupNameMap.get(groupID);

                    return CalendarResponseDTO.builder()
                        .calendarId(calendar.getCalendarId())
                        .title(calendar.getTitle())
                        .description(calendar.getDescription())
                        .startDate(calendar.getStartDate())
                        .endDate(calendar.getEndDate())
                        .isAllDay(calendar.getIsAllDay())
                        .type(calendar.getType())
                        .category(calendar.getCategory())
                        .groupId(groupID)
                        .groupName(groupName)
                        .userId(calendar.getUser().getUserId())
                        .repeatType(calendar.getRepeatType())
                        .repeatEndDate(calendar.getRepeatEndDate())
                        .createdAt(calendar.getCreatedAt())
                        .build();
                }).toList();

        } catch (Exception e) {
            log.warn("그룹 일정 조회 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_FOUND_FAILED);
        }
    }

    @Override
    public CalendarResponseDTO getCalendarById(Long calendarId, UUID userId) {

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

        hasCalendarAccess(calendar, userId);

        return CalendarResponseDTO.builder()
            .calendarId(calendar.getCalendarId())
            .title(calendar.getTitle())
            .description(calendar.getDescription())
            .startDate(calendar.getStartDate())
            .endDate(calendar.getEndDate())
            .isAllDay(calendar.getIsAllDay())
            .type(calendar.getType())
            .category(calendar.getCategory())
            .groupId(calendar.getGroup().getGroupId())
            .groupName(calendar.getGroup().getGroupName())
            .userId(userId)
            .repeatType(calendar.getRepeatType())
            .repeatEndDate(calendar.getRepeatEndDate())
            .createdAt(calendar.getCreatedAt())
            .build();
    }

    @Override
    @Transactional
    public CalendarResponseDTO updateCalendar(Long calendarId, UpdateCalendarRequestDTO request, UUID userId) {

        try {
            Calendar calendar = calendarRepository.findByIdWithLock(calendarId)
                .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            hasCalendarAccess(calendar, userId);

            if (!hasPermission(calendar, userId)) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }

            if (request.getTitle() != null) {
                calendar.updateTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                calendar.updateDescription(request.getDescription());
            }

            if (request.getStartDate() != null && request.getEndDate() != null) {
                validateTimeConflict(calendar, request.getStartDate(), request.getEndDate());
                calendar.updateDateTime(request.getStartDate(), request.getEndDate());
            } else if (request.getStartDate() != null) {
                validateTimeConflict(calendar, request.getStartDate(), calendar.getEndDate());
                calendar.updateDateTime(request.getStartDate(), calendar.getEndDate());
            } else if (request.getEndDate() != null) {
                validateTimeConflict(calendar, calendar.getStartDate(), request.getEndDate());
                calendar.updateDateTime(calendar.getStartDate(), request.getEndDate());
            }

            if (request.getIsAllDay() != null) {
                calendar.updateAllDay(request.getIsAllDay());
            }
            if (request.getCategory() != null) {
                calendar.updateCategory(request.getCategory());
            }
            if (request.getRepeatType() != null) {
                if (request.getRepeatType() == RepeatType.NONE) {
                    calendar.removeRepeat();
                } else {
                    calendar.updateAsRepeating(request.getRepeatType(), request.getRepeatEndDate());
                }
            } else if (request.getRepeatEndDate() != null) {
                calendar.updateRepeatEndDate(request.getRepeatEndDate());
            }

            CalendarValidator.validateDateOrder(calendar.getStartDate(), calendar.getEndDate());

            Calendar updatedCalendar = calendarRepository.save(calendar);

            return CalendarResponseDTO.builder()
                .calendarId(updatedCalendar.getCalendarId())
                .title(updatedCalendar.getTitle())
                .description(updatedCalendar.getDescription())
                .startDate(updatedCalendar.getStartDate())
                .endDate(updatedCalendar.getEndDate())
                .isAllDay(updatedCalendar.getIsAllDay())
                .type(updatedCalendar.getType())
                .category(updatedCalendar.getCategory())
                .groupId(updatedCalendar.getGroup().getGroupId())
                .groupName(updatedCalendar.getGroup().getGroupName())
                .userId(userId)
                .repeatType(updatedCalendar.getRepeatType())
                .repeatEndDate(updatedCalendar.getRepeatEndDate())
                .createdAt(updatedCalendar.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.warn("그룹 일정 수정 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_UPDATE_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteCalendar(Long calendarId, LocalDateTime instanceDate, UUID userId) {
        try {
            Calendar calendar = calendarRepository.findById(calendarId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            if (!hasPermission(calendar, userId)) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }

            calendarRepository.delete(calendar);
        } catch (Exception e) {
            log.warn("그룹 일정 삭제 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.CALENDAR_DELETE_FAILED);
        }
    }

    @Override
    public boolean isGroupMember(UUID groupId, UUID userId) {
        try {
            boolean isOwner = groupRepository.existsByGroupIdAndOwnerId(groupId, userId);
            boolean isMember = groupRepository.existsByGroupIdAndMemberId(groupId, userId);

            return isOwner || isMember;
        } catch (Exception e) {
            log.error("그룹 멤버 권한 확인 중 오류 발생 - 그룹 ID: {}, 사용자: {}, 오류: {}", groupId, userId, e.getMessage());
            return false;
        }
    }

    private void validateTimeConflict(Calendar calendar, LocalDateTime startDate, LocalDateTime endDate) {
        if (calendar.getType() == CalendarType.GROUP && calendar.getGroup().getGroupId() != null) {
            List<Calendar> conflictingCalendars = calendarRepository
                .findConflictingGroupCalendarsExcluding(
                    calendar.getGroup().getGroupId(),
                    calendar.getCalendarId(),
                    startDate,
                    endDate
                );

            if (!conflictingCalendars.isEmpty()) {
                throw new CustomException(ErrorCode.CALENDAR_ALREADY_PRESENT);
            }
        }
    }

    private void hasCalendarAccess(Calendar calendar, UUID userId) {
        if (!calendar.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
        }

        if (calendar.getType() == CalendarType.GROUP && calendar.getGroup().getGroupId() != null) {
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
        }
    }

    private List<Calendar> expandRecurringCalendars(List<Calendar> baseCalendars,
                                                    LocalDateTime rangeStart,
                                                    LocalDateTime rangeEnd) {
        List<Calendar> allInstances = new ArrayList<>();

        for (Calendar baseCalendar : baseCalendars) {
            if (baseCalendar.getRepeatType() == null || baseCalendar.getRepeatType() == RepeatType.NONE) {
                allInstances.add(baseCalendar);
            } else {
                List<Calendar> instances = generateRecurringInstances(baseCalendar, rangeStart, rangeEnd);

                allInstances.addAll(instances);
            }
        }

        // 시작 시간순으로 정렬
        allInstances.sort(Comparator.comparing(Calendar::getStartDate));

        return allInstances;
    }

    private List<Calendar> generateRecurringInstances(Calendar baseCalendar,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        List<Calendar> instances = new ArrayList<>();

        if (baseCalendar.getRepeatType() == null || baseCalendar.getRepeatType() == RepeatType.NONE) {
            instances.add(baseCalendar);
            return instances;
        }

        LocalDateTime current = baseCalendar.getStartDate();
        LocalDateTime repeatEnd = baseCalendar.getRepeatEndDate();
        Duration duration = Duration.between(baseCalendar.getStartDate(), baseCalendar.getEndDate());

        LocalDateTime actualEnd = (repeatEnd != null && repeatEnd.isBefore(endDate)) ? repeatEnd : endDate;

        int maxInstances = 1000;
        int instanceCount = 0;

        while (!current.isAfter(actualEnd) && (repeatEnd == null || !current.isAfter(repeatEnd)) && instanceCount < maxInstances) {
            if (!current.isBefore(startDate)) {
                Calendar instance = createInstanceFromBase(baseCalendar, current, duration);
                instances.add(instance);
                instanceCount++;
            }

            LocalDateTime nextOccurrence = calculateNextOccurrence(current, baseCalendar.getRepeatType());

            // 무한 루프 방지: 다음 발생일이 현재 발생일보다 이전이거나 같으면 중단
            if (!nextOccurrence.isAfter(current)) {
                log.warn("반복 일정 계산 중 무한 루프 감지 (날짜 증가 없음), 중단 - 일정 ID: {}", baseCalendar.getCalendarId());
                break;
            }
            current = nextOccurrence;
        }

        log.info("반복 일정 인스턴스 생성 완료 - 총 {}개 인스턴스", instances.size());
        return instances;
    }

    private Calendar createInstanceFromBase(Calendar base, LocalDateTime newStartDate, Duration duration) {
        Calendar instance = Calendar.builder()
                .title(base.getTitle())
                .description(base.getDescription())
                .startDate(newStartDate)
                .endDate(newStartDate.plus(duration))
                .isAllDay(base.getIsAllDay())
                .type(base.getType())
                .category(base.getCategory())
                .group(base.getGroup())
                .user(base.getUser())
                .repeatType(base.getRepeatType())
                .repeatEndDate(base.getRepeatEndDate())
                .build();

        try {
            java.lang.reflect.Field field = Calendar.class.getDeclaredField("calendarId");
            field.setAccessible(true);
            field.set(instance, base.getCalendarId());
        } catch (Exception e) {
            log.debug("calendarId 설정 실패, 원본 ID 없이 진행: {}", e.getMessage());
        }
        return instance;
    }
    
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
                    .filter(calendar -> calendar.getGroup() != null && groupId.equals(calendar.getGroup().getGroupId()))
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
                log.debug("개인 일정 수정 권한 확인 - 작성자: {}, 요청자: {}",
                        calendar.getUser().getUserId(), userId);
                yield calendar.getUser().getUserId().equals(userId);
            }

            case GROUP -> {
                log.debug("그룹 일정 수정 권한 확인 - 작성자: {}, 요청자: {}, 그룹: {}",
                        calendar.getUser().getUserId(), userId, calendar.getGroup().getGroupId());

                if (calendar.getUser().getUserId().equals(userId)) {
                    yield true;
                }

                if (calendar.getGroup().getGroupId() != null) {
                    yield isGroupMember(calendar.getGroup().getGroupId(), userId);
                }

                yield false;
            }
        };
    }
}
