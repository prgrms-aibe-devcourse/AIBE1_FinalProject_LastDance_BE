package store.lastdance.service.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.calendar.CalendarConverter;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.RepeatType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.calendar.DateRangeDTO;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calendar.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calendar.response.CalendarResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.validation.calendar.CalendarValidator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarV2ServiceImpl implements CalendarV2Service {

    private final CalendarRepository calendarRepository;
    private final CalendarConverter calendarConverter;
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

        Calendar calendar = calendarConverter.toEntity(request, user, group, type);
        Calendar savedCalendar = calendarRepository.save(calendar);
        String groupName = (group != null) ? group.getGroupName() : null;

        return calendarConverter.toDto(savedCalendar, user, group, groupName);
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
            if (groupId != null) {
                CalendarValidator.validateGroupMembership(groupId, isGroupMember(groupId, userId));
            }

            if (dateTime == null) {
                dateTime = LocalDateTime.now();
            }
            DateRangeDTO dateRange = calculateDateRange(viewType, dateTime);

            List<Calendar> baseCalendars = calendarRepository.findCalendarsWithDynamicFilters(
                    userId,
                    dateRange.start(),
                    dateRange.end(),
                    type,
                    category,
                    groupId
            );

            List<Calendar> expandedInstances = expandRecurringCalendars(
                    baseCalendars,
                    dateRange.start(),
                    dateRange.end()
            );

            return expandedInstances.stream()
                    .map(calendar -> {
                        Group group = calendar.getGroup();
                        String groupName = (group != null) ? group.getGroupName() : null;

                        return calendarConverter.toDto(
                                calendar,
                                calendar.getUser(),
                                group,
                                groupName
                        );
                    })
                    .toList();

        } catch (Exception e) {
            log.error("일정 통합 조회 중 서버 오류 발생 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.CALENDAR_FOUND_FAILED);
        }
    }

    @Override
    public CalendarResponseDTO getCalendarById(Long calendarId, UUID userId) {

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

        if (lacksPermission(calendar, userId)) {
            throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
        }

        Group calendarGroup = calendar.getGroup();
        String groupName = (calendarGroup != null) ? calendarGroup.getGroupName() : null;

        return calendarConverter.toDto(calendar, calendar.getUser(), calendarGroup, groupName);
    }

    @Override
    @Transactional
    public CalendarResponseDTO updateCalendar(Long calendarId, UpdateCalendarRequestDTO request, UUID userId) {
        try {
            Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            if (lacksPermission(calendar, userId)) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }

            if (request.getTitle() != null) {
                calendar.updateTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                calendar.updateDescription(request.getDescription());
            }

            if (request.getStartDate() != null && request.getEndDate() != null) {
                if (request.getStartDate().isAfter(request.getEndDate())) {
                    throw new CustomException(ErrorCode.INVALID_DATE_ORDER);
                }
                calendar.updateDateTime(request.getStartDate(), request.getEndDate());
            } else if (request.getStartDate() != null) {
                if (request.getStartDate().isAfter(calendar.getEndDate())) {
                    throw new CustomException(ErrorCode.INVALID_DATE_ORDER);
                }
                calendar.updateDateTime(request.getStartDate(), calendar.getEndDate());
            } else if (request.getEndDate() != null) {
                if (calendar.getStartDate().isAfter(request.getEndDate())) {
                    throw new CustomException(ErrorCode.INVALID_DATE_ORDER);
                }
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

            Group updatedGroup = updatedCalendar.getGroup();
            String groupName = (updatedGroup != null) ? updatedGroup.getGroupName() : null;

            return calendarConverter.toDto(updatedCalendar, updatedCalendar.getUser(), updatedGroup, groupName);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new CustomException(ErrorCode.OPTIMISTIC_LOCK_FAILURE);
        } catch (Exception e) {
            log.error("일정 수정 중 서버 오류 발생 - calendarId: {}, userId: {}", calendarId, userId, e);
            throw new CustomException(ErrorCode.CALENDAR_UPDATE_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteCalendar(Long calendarId, UUID userId) {
        try {
            Calendar calendar = calendarRepository.findById(calendarId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CALENDAR_NOT_FOUND));

            if (lacksPermission(calendar, userId)) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }

            calendarRepository.delete(calendar);
        } catch (Exception e) {
            log.error("일정 삭제 중 서버 오류 발생 - calendarId: {}, userId: {}", calendarId, userId, e);
            throw new CustomException(ErrorCode.CALENDAR_DELETE_FAILED);
        }
    }

    @Override
    public boolean isGroupMember(UUID groupId, UUID userId) {
        boolean isOwner = groupRepository.existsByGroupIdAndOwnerId(groupId, userId);
        boolean isMember = groupRepository.existsByGroupIdAndMemberId(groupId, userId);
        return isOwner || isMember;
    }


    private List<Calendar> expandRecurringCalendars(List<Calendar> baseCalendars, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<Calendar> allInstances = new ArrayList<>();

        for (Calendar baseCalendar : baseCalendars) {
            if (baseCalendar.getRepeatType() == null || baseCalendar.getRepeatType() == RepeatType.NONE) {
                allInstances.add(baseCalendar);
            } else {
                List<Calendar> instances = generateRecurringInstances(baseCalendar, rangeStart, rangeEnd);

                allInstances.addAll(instances);
            }
        }

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

        LocalDateTime origin = baseCalendar.getStartDate();
        int originalDay = origin.getDayOfMonth();
        RepeatType repeatType = baseCalendar.getRepeatType();

        LocalDateTime repeatEnd = baseCalendar.getRepeatEndDate();
        Duration duration = Duration.between(origin, baseCalendar.getEndDate());
        LocalDateTime actualEnd = (repeatEnd != null && repeatEnd.isBefore(endDate)) ? repeatEnd : endDate;

        int maxInstances = 1000;
        long step = 0;

        while (instances.size() < maxInstances) {
            LocalDateTime current = nthOccurrence(origin, repeatType, originalDay, step);

            if (current.isAfter(actualEnd)) break;

            if (!current.isBefore(startDate)) {
                instances.add(createInstanceFromBase(baseCalendar, current, duration));
            }

            step++;
        }

        log.debug("반복 일정 인스턴스 생성 완료 - 총 {}개 인스턴스", instances.size());
        return instances;
    }

    private LocalDateTime nthOccurrence(LocalDateTime origin, RepeatType repeatType, int originalDay, long step) {
        return switch (repeatType) {
            case DAILY  -> origin.plusDays(step);
            case WEEKLY -> origin.plusWeeks(step);
            case MONTHLY -> {
                LocalDate base = origin.toLocalDate().withDayOfMonth(1).plusMonths(step);
                yield origin.toLocalDate().withDayOfMonth(1).plusMonths(step)
                            .withDayOfMonth(Math.min(originalDay, base.lengthOfMonth()))
                            .atTime(origin.toLocalTime());
            }
            case YEARLY -> {
                LocalDate base = origin.toLocalDate().withDayOfMonth(1).plusYears(step);
                yield origin.toLocalDate().withDayOfMonth(1).plusYears(step)
                            .withDayOfMonth(Math.min(originalDay, base.lengthOfMonth()))
                            .atTime(origin.toLocalTime());
            }
            default -> origin.plusDays(step);
        };
    }

    private Calendar createInstanceFromBase(Calendar base, LocalDateTime newStartDate, Duration duration) {
        LocalDateTime newEndDate = newStartDate.plus(duration);
        return Calendar.copyWithNewDate(base, newStartDate, newEndDate);
    }

    private DateRangeDTO calculateDateRange(String viewType, LocalDateTime baseDate) {
        return switch (viewType.toUpperCase()) {
            case "DAILY" -> {
                LocalDateTime startOfDay = baseDate.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = baseDate.toLocalDate().atTime(23, 59, 59);
                yield new DateRangeDTO(startOfDay, endOfDay);
            }

            case "WEEKLY" -> {
                LocalDateTime startOfWeek = baseDate.toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .atStartOfDay();
                LocalDateTime endOfWeek = baseDate.toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfWeek, endOfWeek);
            }

            case "MONTHLY" -> {
                LocalDate monthStart = baseDate.toLocalDate().withDayOfMonth(1);
                LocalDate monthEnd = baseDate.toLocalDate().withDayOfMonth(baseDate.toLocalDate().lengthOfMonth());

                LocalDate calendarStart = monthStart.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                LocalDate calendarEnd = monthEnd.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

                yield new DateRangeDTO(
                        calendarStart.atStartOfDay(),
                        calendarEnd.atTime(23, 59, 59)
                );
            }

            case "YEARLY" -> {
                LocalDateTime startOfYear = baseDate.toLocalDate()
                        .withDayOfYear(1)
                        .atStartOfDay();
                LocalDateTime endOfYear = baseDate.toLocalDate()
                        .withDayOfYear(baseDate.toLocalDate().lengthOfYear())
                        .atTime(23, 59, 59);
                yield new DateRangeDTO(startOfYear, endOfYear);
            }

            default -> {
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

    private boolean lacksPermission(Calendar calendar, UUID userId) {
        return !switch (calendar.getType()) {
            case PERSONAL -> {
                log.debug("개인 일정 수정 권한 확인 - 작성자: {}, 요청자: {}",
                    calendar.getUser().getUserId(), userId);
                yield calendar.getUser().getUserId().equals(userId);
            }

            case GROUP -> {
                log.debug("그룹 일정 수정 권한 확인 - 작성자: {}, 요청자: {}, 그룹: {}",
                    calendar.getUser().getUserId(), userId,
                    calendar.getGroup() != null ? calendar.getGroup().getGroupId() : null);

                if (calendar.getUser().getUserId().equals(userId)) {
                    yield true;
                }

                if (calendar.getGroup() != null && calendar.getGroup().getGroupId() != null) {
                    yield isGroupMember(calendar.getGroup().getGroupId(), userId);
                }

                yield false;
            }
        };
    }
}
