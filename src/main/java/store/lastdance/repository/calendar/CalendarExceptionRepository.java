package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.CalendarException;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarExceptionRepository extends JpaRepository<CalendarException, Long> {
    
    List<CalendarException> findByCalendarId(Long calendarId);
    
    @Query("SELECT se FROM CalendarException se WHERE se.calendarId = :calendarId " +
           "AND se.exceptionDate BETWEEN :startDate AND :endDate")
    List<CalendarException> findByCalendarIdAndDateRange(@Param("calendarId") Long calendarId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    boolean existsByCalendarIdAndExceptionDate(Long calendarId, LocalDateTime exceptionDate);
    
    void deleteByCalendarId(Long calendarId);
}