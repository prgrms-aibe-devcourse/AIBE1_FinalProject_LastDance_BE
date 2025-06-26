package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.community.PostCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // 사용자 ID로 게시글 조회
    List<Post> findByUserId(UUID userId);

    // 카테고리별 게시글 조회
    List<Post> findByCategory(PostCategory category);

    // 제목 키워드로 검색
    List<Post> findByTitleContainingIgnoreCase(String keyword);

    // 특정 사용자 ID와 카테고리로 필터링
    List<Post> findByUserIdAndCategory(UUID userId, PostCategory category);

    long countByCreatedAtAfter(LocalDateTime dailyStatsCriteria);

    long countByUserId(UUID userId);
}
