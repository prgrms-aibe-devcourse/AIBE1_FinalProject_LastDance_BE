package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.PostConverter;
import store.lastdance.domain.community.Post;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.repository.community.BookmarkRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.community.LikeRepository;
import store.lastdance.repository.community.PostRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityV2QueryServiceImpl implements CommunityV2QueryService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final PostConverter postConverter;

    @Override
    public List<PostResponseDTO> getAllPosts(UUID currentUserId) {
        List<Post> posts = postRepository.findAll();
        
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> postIds = posts.stream()
                .map(Post::getPostId)
                .collect(Collectors.toList());

        // 1. 좋아요/북마크 정보 일괄 조회
        Set<UUID> likedPostIds = (currentUserId != null) 
                ? likeRepository.findPostIdsByUserIdAndPostIdIn(currentUserId, postIds)
                : Collections.emptySet();
        
        Set<UUID> bookmarkedPostIds = (currentUserId != null)
                ? bookmarkRepository.findPostIdsByUserIdAndPostIdIn(currentUserId, postIds)
                : Collections.emptySet();

        // 2. 댓글 수 일괄 조회 (DB 컬럼이 없으므로 직접 카운트 쿼리 실행)
        Map<UUID, Long> commentCounts = commentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        obj -> (UUID) obj[0],
                        obj -> (Long) obj[1]
                ));

        return posts.stream()
                .map(post -> {
                    boolean userLiked = likedPostIds.contains(post.getPostId());
                    boolean userBookmarked = bookmarkedPostIds.contains(post.getPostId());
                    long commentCount = commentCounts.getOrDefault(post.getPostId(), 0L);

                    return postConverter.toResponseDTO(
                            post, 
                            post.getLikeCount(), 
                            commentCount, 
                            userLiked, 
                            userBookmarked
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public PostResponseDTO getPostById(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        long commentCount = commentRepository.countByPostId(postId);
        boolean userLiked = (currentUserId != null) && likeRepository.findByPostIdAndUserId(postId, currentUserId).isPresent();
        boolean userBookmarked = (currentUserId != null) && bookmarkRepository.existsByPostIdAndUserId(postId, currentUserId);
        
        return postConverter.toResponseDTO(
                post, 
                post.getLikeCount(), 
                commentCount, 
                userLiked, 
                userBookmarked
        );
    }
}
