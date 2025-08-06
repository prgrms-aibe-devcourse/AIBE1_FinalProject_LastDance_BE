package store.lastdance.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    boolean existsByInviteCode(String code);

    Optional<Group> findByInviteCode(String inviteCode);

    List<Group> findByMembers_User(User user);
    
    /**
     * 특정 그룹에 특정 사용자가 멤버로 속해있는지 확인
     */
    @Query(value = "SELECT CASE WHEN COUNT(gm.group_id) > 0 THEN true ELSE false END " +
                   "FROM group_members gm " +
                   "WHERE gm.group_id = :groupId AND gm.user_id = :userId",
            nativeQuery = true)
    boolean existsByGroupIdAndMemberId(@Param("groupId") UUID groupId, 
                                     @Param("userId") UUID userId);
    
    /**
     * 그룹 소유자인지 확인
     */
    @Query(value = "SELECT CASE WHEN COUNT(g.group_id) > 0 THEN true ELSE false END " +
                   "FROM groups g " + 
                   "WHERE g.group_id = :groupId AND g.owner_id = :userId",
            nativeQuery = true)
    boolean existsByGroupIdAndOwnerId(@Param("groupId") UUID groupId, 
                                    @Param("userId") UUID userId);

    @Query("SELECT g.groupName FROM Group g WHERE g.groupId = :groupId")
    Optional<String> findGroupNameByGroupId(@Param("groupId") UUID groupId);

    /**
     * 여러 그룹 ID로 그룹 이름들을 일괄 조회
     */
    @Query("SELECT g.groupId as groupId, g.groupName as groupName " +
           "FROM Group g WHERE g.groupId IN :groupIds")
    List<GroupNameProjection> findGroupNamesByGroupIds(@Param("groupIds") List<UUID> groupIds);
}
