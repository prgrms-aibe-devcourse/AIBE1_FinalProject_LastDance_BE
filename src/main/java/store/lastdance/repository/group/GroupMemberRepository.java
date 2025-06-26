package store.lastdance.repository.group;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupApplication;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.domain.user.User;

import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    GroupMember findByUserAndGroup(User user, Group group);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group = :group AND gm.user = :user")
    void deleteByGroupAndUser(@Param("group") Group group, @Param("user") User user);
}
