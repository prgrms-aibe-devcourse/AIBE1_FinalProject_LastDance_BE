package store.lastdance.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupApplicationRepository extends JpaRepository<GroupApplication, Long> {

    void deleteByGroupAndUser(Group group, User user);

    boolean existsByGroupAndUser(Group group, User user);
}
