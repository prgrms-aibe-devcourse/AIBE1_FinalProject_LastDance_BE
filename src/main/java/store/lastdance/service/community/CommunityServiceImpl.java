package store.lastdance.service.community;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.community.Bookmark;
import store.lastdance.domain.community.Like;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.user.User;
import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.UpdatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.repository.community.BookmarkRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.community.LikeRepository;
import store.lastdance.repository.community.PostRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.domain.community.PostCategory; // ✅ PostCategory 임포트

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository; // 댓글 갯수를 위해 추가

    @Override
    public PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .title(request.getTitle())
                .content(request.getContent())
                .category(PostCategory.valueOf(String.valueOf(request.getCategory()))) // ✅ String -> PostCategory Enum 변환
                .userId(userId)
                .build();

        post.setUser(user);

        return PostResponseDTO.from(postRepository.save(post));
    }

    @Override
    public List<PostResponseDTO> getAllPosts(UUID currentUserId) {
        return postRepository.findAll().stream()
                .map(post -> {
                    long likeCount = likeRepository.countByPostId(post.getPostId());
                    long commentCount = commentRepository.countByPostId(post.getPostId()); // 댓글 갯수 조회
                    boolean userLiked = likeRepository.findByPostIdAndUserId(post.getPostId(), currentUserId).isPresent();
                    boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(post.getPostId(), currentUserId);
                    // 카테고리 정보가 포함된 DTO 생성
                    return PostResponseDTO.from(post, likeCount, commentCount, userLiked, userBookmarked);
                })
                .collect(Collectors.toList());
    }

    @Override
    public PostResponseDTO getPostById(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        long likeCount = likeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId); // 댓글 갯수 조회
        boolean userLiked = likeRepository.findByPostIdAndUserId(postId, currentUserId).isPresent();
        boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, currentUserId);
        // 카테고리 정보가 포함된 DTO 생성
        return PostResponseDTO.from(post, likeCount, commentCount, userLiked, userBookmarked);
    }

    @Override
    public PostResponseDTO updatePost(UUID postId, UpdatePostRequestDTO request, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }

        post.updateTitle(request.getTitle());
        post.updateContent(request.getContent());
        // 필요하다면 request.getCategory()를 사용하여 post.updateCategory() 호출
        // post.updateCategory(PostCategory.valueOf(request.getCategory()));

        // 업데이트된 게시글 반환 시 카테고리 정보도 포함됩니다.
        return PostResponseDTO.from(postRepository.save(post));
    }

    @Override
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public boolean toggleLike(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        return likeRepository.findByPostIdAndUserId(postId, userId)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    post.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    Like newLike = Like.builder()
                            .postId(postId)
                            .userId(userId)
                            .build();
                    likeRepository.save(newLike);
                    post.incrementLikeCount();
                    return true;
                });
    }

    @Override
    @Transactional
    public boolean toggleBookmark(UUID postId, UUID userId) {
        return bookmarkRepository.findByPostIdAndUserId(postId, userId)
                .map(existingBookmark -> {
                    bookmarkRepository.delete(existingBookmark);
                    return false;
                })
                .orElseGet(() -> {
                    Bookmark bookmark = Bookmark.builder()
                            .postId(postId)
                            .userId(userId)
                            .build();
                    bookmarkRepository.save(bookmark);
                    return true;
                });
    }
}