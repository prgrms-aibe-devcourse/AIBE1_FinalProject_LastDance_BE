package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.community.Bookmark;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    @Query("SELECT b.postId FROM Bookmark b WHERE b.userId = :userId AND b.postId IN :postIds")
    Set<UUID> findPostIdsByUserIdAndPostIdIn(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}
