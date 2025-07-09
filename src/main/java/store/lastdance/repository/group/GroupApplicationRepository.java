package store.lastdance.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupApplicationRepository extends JpaRepository<GroupApplication, Long> {

    @Modifying
    @Query("DELETE FROM GroupApplication ga WHERE ga.group = :group AND ga.user = :user")
    void deleteByGroupAndUser(@Param("group") Group group, @Param("user") User user);

    boolean existsByGroupAndUser(Group group, User user);

    List<GroupApplication> findByGroup(Group group);

    Object findByGroupAndUser(Group group, User member);
}
