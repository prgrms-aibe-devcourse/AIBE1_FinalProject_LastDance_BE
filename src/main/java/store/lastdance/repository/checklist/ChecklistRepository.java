package store.lastdance.repository.checklist;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.util.Collection;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    Collection<Checklist> findByGroup(Group group);

    Collection<Checklist> findByAssignee(User user);

    Collection<Checklist> findByGroupAndAssignee(Group group, User assignee);

    void deleteByGroupAndAssignee(Group group, User user);
}
