package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.CalendarCategory;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    @Modifying
    @Query("DELETE FROM Calendar c WHERE c.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") UUID groupId);

    /**
     * 사용자 ID로 일정 조회 (그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c WHERE c.user.userId = :userId")
    List<Calendar> findByUserId(@Param("userId") UUID userId);

    /**
     * 사용자 ID와 날짜 범위로 일정 조회 (반복 일정 포함, fetch join으로 N+1 방지)
     */
    @Query("SELECT c FROM Calendar c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.group " +
           "WHERE c.user.userId = :userId " +
           "AND ((" +
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           "(c.repeatType != 'NONE' AND c.repeatType IS NOT NULL) AND " +
           "c.startDate <= :endDate AND " +
           "(c.repeatEndDate IS NULL OR c.repeatEndDate >= :startDate)" +
           "))")
    List<Calendar> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자가 속한 그룹들의 일정 조회 (날짜 범위, fetch join으로 N+1 방지)
     */
    @Query("SELECT c FROM Calendar c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.group " +
           "WHERE c.type = 'GROUP' " +
           "AND c.group.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ") " +
           "AND ((" +
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           "(c.repeatType != 'NONE' AND c.repeatType IS NOT NULL) AND " +
           "c.startDate <= :endDate AND " +
           "(c.repeatEndDate IS NULL OR c.repeatEndDate >= :startDate)" +
           "))")
    List<Calendar> findGroupCalendarsForUser(@Param("userId") UUID userId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자가 속한 그룹들의 모든 일정 조회 (그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c WHERE c.type = 'GROUP' " +
           "AND c.group.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ")")
    List<Calendar> findAllGroupCalendarsForUser(@Param("userId") UUID userId);

    /**
     * 특정 사용자의 특정 시간대 일정 조회 (알림용)
     */
    @Query("SELECT c FROM Calendar c WHERE c.user.userId = :userId " +
           "AND c.startDate BETWEEN :startTime AND :endTime")
    List<Calendar> findByUserIdAndStartTimeBetween(@Param("userId") UUID userId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 사용자가 속한 그룹들의 특정 시간대 그룹 일정 조회 (알림용)
     */
    @Query("SELECT c FROM Calendar c WHERE c.type = 'GROUP' " +
           "AND c.startDate BETWEEN :startTime AND :endTime " +
           "AND c.group.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ")")
    List<Calendar> findGroupCalendarsForUserInTimeRange(@Param("userId") UUID userId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 비관적 락을 사용한 일정 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Calendar c WHERE c.calendarId = :calendarId")
    Optional<Calendar> findByIdWithLock(@Param("calendarId") Long calendarId);
}
