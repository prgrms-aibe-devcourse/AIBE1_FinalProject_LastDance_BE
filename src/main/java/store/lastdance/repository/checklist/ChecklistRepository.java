package store.lastdance.repository.checklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    Collection<Checklist> findByGroup(Group group);

    Collection<Checklist> findByAssignee(User user);

    Collection<Checklist> findByGroupAndAssignee(Group group, User assignee);

    void deleteByGroupAndAssignee(Group group, User user);
    
    @Modifying
    void deleteByGroup(Group group);

    /**
     * 특정 사용자의 특정 날짜 범위의 미완료 체크리스트 조회 (알림용)
     */
    @Query("SELECT c FROM Checklist c WHERE c.assignee.userId = :userId " +
           "AND c.dueDate BETWEEN :startDate AND :endDate " +
           "AND c.isCompleted = false " +
           "ORDER BY c.dueDate ASC")
    List<Checklist> findByUserIdAndDueDateBetweenAndIsCompletedFalse(@Param("userId") UUID userId,
                                                                   @Param("startDate") java.time.LocalDateTime startDate,
                                                                   @Param("endDate") java.time.LocalDateTime endDate);
}
