package store.lastdance.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    GroupMember findByUserAndGroup(User user, Group group);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group = :group AND gm.user = :user")
    void deleteByGroupAndUser(@Param("group") Group group, @Param("user") User user);

    long countByUser_UserId(UUID userId);

    // 그룹의 모든 멤버 조회 (정산용)
    List<GroupMember> findByGroup(Group group);

    // 그룹ID로 멤버 조회
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.groupId = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") UUID groupId);

    List<GroupMember> group(Group group);

    List<GroupMember> user(User user);

    // 사용자가 특정 그룹의 멤버인지 확인
    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.user.userId = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    Object countByGroup(Group group);

    Object findByUser(User owner);
}
