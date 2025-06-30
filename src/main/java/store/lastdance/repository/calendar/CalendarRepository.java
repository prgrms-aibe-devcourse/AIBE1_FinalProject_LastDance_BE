package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.CalendarCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    /**
     * 사용자 ID로 일정 조회 (그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.userId = :userId")
    List<Calendar> findByUserId(UUID userId);

    /**
     * 사용자 ID와 날짜 범위로 일정 조회 (반복 일정 포함, 그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.userId = :userId " +
           "AND ((" +
           // 일반 일정 (반복 없음)
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           // 반복 일정 - 조회 범위와 겹칠 가능성이 있는 모든 반복 일정
           "(c.repeatType != 'NONE' AND c.repeatType IS NOT NULL) AND " +
           "c.startDate <= :endDate AND " +
           "(c.repeatEndDate IS NULL OR c.repeatEndDate >= :startDate)" +
           "))")
    List<Calendar> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 그룹 ID로 일정 조회 (그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.groupId = :groupId")
    List<Calendar> findByGroupId(UUID groupId);

    /**
     * 그룹 ID와 날짜 범위로 일정 조회 (반복 일정 포함, 그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.groupId = :groupId " +
           "AND ((" +
           // 일반 일정 (반복 없음)
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           // 반복 일정 - 조회 범위와 겹칠 가능성이 있는 모든 반복 일정
           "(c.repeatType != 'NONE' AND c.repeatType IS NOT NULL) AND " +
           "c.startDate <= :endDate AND " +
           "(c.repeatEndDate IS NULL OR c.repeatEndDate >= :startDate)" +
           "))")
    List<Calendar> findByGroupIdAndDateRange(@Param("groupId") UUID groupId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자 ID와 일정 타입으로 조회
     */
    List<Calendar> findByUserIdAndType(UUID userId, CalendarType type);

    /**
     * 사용자 ID와 카테고리로 조회
     */
    List<Calendar> findByUserIdAndCategory(UUID userId, CalendarCategory category);

    /**
     * 반복 일정 조회
     */
    @Query("SELECT s FROM Calendar s WHERE s.userId = :userId AND s.repeatType != 'NONE'")
    List<Calendar> findRecurringCalendarsByUserId(@Param("userId") UUID userId);

    /**
     * 특정 날짜의 일정 조회
     */
    @Query("SELECT s FROM Calendar s WHERE s.userId = :userId " +
           "AND DATE(s.startDate) = DATE(:date)")
    List<Calendar> findByUserIdAndDate(@Param("userId") UUID userId,
                                     @Param("date") LocalDateTime date);

    /**
     * 제목으로 일정 검색
     */
    @Query("SELECT s FROM Calendar s WHERE s.userId = :userId " +
           "AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Calendar> searchByTitleKeyword(@Param("userId") UUID userId,
                                      @Param("keyword") String keyword);

    /**
     * 다가오는 일정 조회 (알림용)
     */
    @Query("SELECT s FROM Calendar s WHERE s.userId = :userId " +
           "AND s.startDate BETWEEN :from AND :to " +
           "ORDER BY s.startDate ASC")
    List<Calendar> findUpcomingCalendars(@Param("userId") UUID userId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * 시작 시간 범위로 일정 조회 (알림 스케줄러용)
     */
    @Query("SELECT c FROM Calendar c WHERE c.startDate BETWEEN :startTime AND :endTime " +
           "ORDER BY c.startDate ASC")
    List<Calendar> findByStartDateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 월별 일정 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Calendar s WHERE s.userId = :userId " +
           "AND YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month")
    Long countByUserIdAndYearMonth(@Param("userId") UUID userId,
                                 @Param("year") int year,
                                 @Param("month") int month);

    /**
     * 모든 그룹 일정 조회 (날짜 범위)
     */
    @Query("SELECT c FROM Calendar c WHERE c.type = 'GROUP' " +
           "AND ((" +
           // 일반 일정 (반복 없음)
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           // 반복 일정 - 조회 범위와 겹칠 가능성이 있는 모든 반복 일정
           "(c.repeatType != 'NONE' AND c.repeatType IS NOT NULL) AND " +
           "c.startDate <= :endDate AND " +
           "(c.repeatEndDate IS NULL OR c.repeatEndDate >= :startDate)" +
           "))")
    List<Calendar> findAllGroupCalendarsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 모든 그룹 일정 조회
     */
    @Query("SELECT c FROM Calendar c WHERE c.type = 'GROUP'")
    List<Calendar> findAllGroupCalendars();

    /**
     * 사용자가 속한 그룹들의 일정 조회 (날짜 범위, 그룹 정보 포함)
     */
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.type = 'GROUP' " +
           "AND c.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ") " +
           "AND ((" +
           // 일반 일정 (반복 없음)
           "(c.repeatType = 'NONE' OR c.repeatType IS NULL) AND " +
           "((c.startDate BETWEEN :startDate AND :endDate) " +
           "OR (c.endDate BETWEEN :startDate AND :endDate) " +
           "OR (c.startDate <= :startDate AND c.endDate >= :endDate))" +
           ") OR (" +
           // 반복 일정 - 조회 범위와 겹칠 가능성이 있는 모든 반복 일정
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
    @Query("SELECT c FROM Calendar c LEFT JOIN FETCH c.group WHERE c.type = 'GROUP' " +
           "AND c.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ")")
    List<Calendar> findAllGroupCalendarsForUser(@Param("userId") UUID userId);

    /**
     * 특정 사용자의 특정 시간대 일정 조회 (알림용)
     */
    @Query("SELECT c FROM Calendar c WHERE c.userId = :userId " +
           "AND c.startDate BETWEEN :startTime AND :endTime")
    List<Calendar> findByUserIdAndStartTimeBetween(@Param("userId") UUID userId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 사용자가 속한 그룹들의 특정 시간대 그룹 일정 조회 (알림용)
     */
    @Query("SELECT c FROM Calendar c WHERE c.type = 'GROUP' " +
           "AND c.startDate BETWEEN :startTime AND :endTime " +
           "AND c.groupId IN (" +
           "    SELECT g.groupId FROM Group g WHERE g.owner.userId = :userId " +
           "    UNION " +
           "    SELECT gm.group.groupId FROM GroupMember gm WHERE gm.user.userId = :userId" +
           ")")
    List<Calendar> findGroupCalendarsForUserInTimeRange(@Param("userId") UUID userId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);
}
