package store.lastdance.repository.calendar;

import store.lastdance.domain.calendar.Calendar;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CalendarRepositoryCustom {
    List<Calendar> findCalendarsWithDynamicFilters(
            UUID userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String type,
            String category,
            UUID groupId);
}