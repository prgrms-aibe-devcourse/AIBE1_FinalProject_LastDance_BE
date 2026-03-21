package store.lastdance.repository.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.calendar.Calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long>, CalendarRepositoryCustom {

    @Modifying
    @Query("DELETE FROM Calendar c WHERE c.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") UUID groupId);

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
}
