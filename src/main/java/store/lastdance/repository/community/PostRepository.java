package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.community.PostCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Post> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    Optional<Post> findById(UUID postId);

    // 사용자 ID로 게시글 조회
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Post> findByUserId(@Param("userId") UUID userId);

    // 카테고리별 게시글 조회
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Post> findByCategory(@Param("category") PostCategory category);

    // 제목 키워드로 검색
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Post> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    // 특정 사용자 ID와 카테고리로 필터링
    @EntityGraph(attributePaths = {"user", "user.profileImageFile"})
    List<Post> findByUserIdAndCategory(@Param("userId") UUID userId, @Param("category") PostCategory category);

    long countByCreatedAtAfter(LocalDateTime dailyStatsCriteria);

    long countByUserId(UUID userId);
}
