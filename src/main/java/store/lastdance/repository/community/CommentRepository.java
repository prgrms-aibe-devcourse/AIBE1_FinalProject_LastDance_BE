package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.community.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Comment> findByPostId(UUID postId);

    @Override
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    Optional<Comment> findById(UUID commentId);

    long countByPostId(UUID postId);

    @Query("SELECT c.postId, COUNT(c) FROM Comment c WHERE c.postId IN :postIds GROUP BY c.postId")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<UUID> postIds);

    long countByCreatedAtAfter(LocalDateTime dailyStatsCriteria);

    long countByUserId(UUID userId);
}