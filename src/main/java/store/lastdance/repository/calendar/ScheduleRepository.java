package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.Schedule;
import store.lastdance.domain.calendar.ScheduleType;
import store.lastdance.domain.calendar.ScheduleCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 사용자 ID로 일정 조회
     */
    List<Schedule> findByUserId(UUID userId);

    /**
     * 사용자 ID와 날짜 범위로 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
           "AND ((s.startDate BETWEEN :startDate AND :endDate) " +
           "OR (s.endDate BETWEEN :startDate AND :endDate) " +
           "OR (s.startDate <= :startDate AND s.endDate >= :endDate))")
    List<Schedule> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 그룹 ID로 일정 조회
     */
    List<Schedule> findByGroupId(UUID groupId);

    /**
     * 그룹 ID와 날짜 범위로 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.groupId = :groupId " +
           "AND ((s.startDate BETWEEN :startDate AND :endDate) " +
           "OR (s.endDate BETWEEN :startDate AND :endDate) " +
           "OR (s.startDate <= :startDate AND s.endDate >= :endDate))")
    List<Schedule> findByGroupIdAndDateRange(@Param("groupId") UUID groupId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자 ID와 일정 타입으로 조회
     */
    List<Schedule> findByUserIdAndType(UUID userId, ScheduleType type);

    /**
     * 사용자 ID와 카테고리로 조회
     */
    List<Schedule> findByUserIdAndCategory(UUID userId, ScheduleCategory category);

    /**
     * 반복 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId AND s.repeatType != 'NONE'")
    List<Schedule> findRecurringSchedulesByUserId(@Param("userId") UUID userId);

    /**
     * 특정 날짜의 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
           "AND DATE(s.startDate) = DATE(:date)")
    List<Schedule> findByUserIdAndDate(@Param("userId") UUID userId,
                                     @Param("date") LocalDateTime date);

    /**
     * 제목으로 일정 검색
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
           "AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Schedule> searchByTitleKeyword(@Param("userId") UUID userId,
                                      @Param("keyword") String keyword);

    /**
     * 다가오는 일정 조회 (알림용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
           "AND s.startDate BETWEEN :from AND :to " +
           "ORDER BY s.startDate ASC")
    List<Schedule> findUpcomingSchedules(@Param("userId") UUID userId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * 월별 일정 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.userId = :userId " +
           "AND YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month")
    Long countByUserIdAndYearMonth(@Param("userId") UUID userId,
                                 @Param("year") int year,
                                 @Param("month") int month);
}
