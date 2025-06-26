package store.lastdance.service.calendar;

import org.springframework.data.domain.Pageable;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarService {

    Calendar createCalendar(CreateCalendarRequestDTO request, UUID userId);

    List<Calendar> getCalendarsByUser(UUID userId,
                                      String viewType,
                                      LocalDateTime dateTime,
                                      String type,
                                      String category,
                                      UUID groupId,
                                      Pageable pageable);

    Calendar getCalendarById(Long calendarId, UUID userId);

    Calendar updateCalendar(Long calendarId, UpdateCalendarRequestDTO request, UUID userId);

    void deleteCalendar(Long calendarId, String deleteType, LocalDateTime instanceDate, UUID userId);

    List<Calendar> getCalendarsByGroup(UUID groupId, String viewType, LocalDateTime dateTime,
                                      String type, String category, Pageable pageable);

    List<Calendar> getCalendarsByGroup(UUID groupId, LocalDateTime startDate, LocalDateTime endDate);

    List<Calendar> getCalendarsByGroupWithViewType(UUID groupId, String viewType, LocalDateTime dateTime,
                                                  String type, String category, Pageable pageable);

    List<Calendar> getRecurringInstances(Long calendarId,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        UUID userId);

    boolean isGroupMember(UUID groupId, UUID userId);

    // ✅ 예외 날짜 조회 메서드 추가
    List<LocalDateTime> getExceptionDatesForCalendar(Long calendarId);
}
