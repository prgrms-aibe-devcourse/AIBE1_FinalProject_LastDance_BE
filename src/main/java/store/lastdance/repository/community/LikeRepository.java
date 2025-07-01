package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.community.Like;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByPostId(UUID postId);
    long countByPostId(UUID postId);
}