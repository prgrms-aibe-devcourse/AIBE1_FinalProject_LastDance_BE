package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.community.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostId(UUID postId);

    long countByPostId(UUID postId);

    long countByCreatedAtAfter(LocalDateTime dailyStatsCriteria);

    long countByUserId(UUID userId);
}