package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.ScheduleException;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    
    List<ScheduleException> findByScheduleId(Long scheduleId);
    
    @Query("SELECT se FROM ScheduleException se WHERE se.scheduleId = :scheduleId " +
           "AND se.exceptionDate BETWEEN :startDate AND :endDate")
    List<ScheduleException> findByScheduleIdAndDateRange(@Param("scheduleId") Long scheduleId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    boolean existsByScheduleIdAndExceptionDate(Long scheduleId, LocalDateTime exceptionDate);
    
    void deleteByScheduleId(Long scheduleId);
}