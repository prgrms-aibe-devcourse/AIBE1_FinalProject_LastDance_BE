package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.community.Like;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByPostId(UUID postId);
    long countByPostId(UUID postId);

    @Query("SELECT l.postId FROM Like l WHERE l.userId = :userId AND l.postId IN :postIds")
    Set<UUID> findPostIdsByUserIdAndPostIdIn(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}