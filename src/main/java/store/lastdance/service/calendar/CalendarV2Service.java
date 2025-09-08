package store.lastdance.service.calendar;

import org.springframework.data.domain.Pageable;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calendar.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calendar.response.CalendarResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarV2Service {

    CalendarResponseDTO createCalendar(CreateCalendarRequestDTO request, UUID userId, UUID groupId);

    List<CalendarResponseDTO> getCalendarsByUser(UUID userId, String viewType, LocalDateTime dateTime, String type, String category, UUID groupId, Pageable pageable);

    CalendarResponseDTO getCalendarById(Long calendarId, UUID userId);

    CalendarResponseDTO updateCalendar(Long calendarId, UpdateCalendarRequestDTO request, UUID userId);

    void deleteCalendar(Long calendarId, UUID userId);

    boolean isGroupMember(UUID groupId, UUID userId);
}
