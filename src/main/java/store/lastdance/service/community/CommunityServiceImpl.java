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
import store.lastdance.domain.community.PostCategory;

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
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .title(request.getTitle())
                .content(request.getContent())
                .category(PostCategory.valueOf(String.valueOf(request.getCategory())))
                .userId(userId)
                .build();

        post.setUser(user);

        Post savedPost = postRepository.save(post);
        // DTO의 from 메서드를 호출하는 대신, 서비스에서 직접 DTO 빌더를 사용
        return createPostResponseDTO(savedPost, 0, 0, false, false);
    }

    @Override
    public List<PostResponseDTO> getAllPosts(UUID currentUserId) {
        return postRepository.findAll().stream()
                .map(post -> {
                    long likeCount = likeRepository.countByPostId(post.getPostId());
                    long commentCount = commentRepository.countByPostId(post.getPostId());
                    boolean userLiked = likeRepository.findByPostIdAndUserId(post.getPostId(), currentUserId).isPresent();
                    boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(post.getPostId(), currentUserId);
                    return createPostResponseDTO(post, likeCount, commentCount, userLiked, userBookmarked);
                })
                .collect(Collectors.toList());
    }

    @Override
    public PostResponseDTO getPostById(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        long likeCount = likeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId);
        boolean userLiked = likeRepository.findByPostIdAndUserId(postId, currentUserId).isPresent();
        boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, currentUserId);
        return createPostResponseDTO(post, likeCount, commentCount, userLiked, userBookmarked);
    }

    @Override
    @Transactional
    public PostResponseDTO updatePost(UUID postId, UpdatePostRequestDTO request, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        post.updateTitle(request.getTitle());
        post.updateContent(request.getContent());


        Post updatedPost = postRepository.save(post);
        // 업데이트 후에는 좋아요/댓글 수 등을 다시 조회하거나, 필요에 따라 기본값 사용
        // 여기서는 간단하게 게시글 상세 조회 시와 동일하게 조회해서 반환
        long likeCount = likeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId);
        boolean userLiked = likeRepository.findByPostIdAndUserId(postId, userId).isPresent();
        boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, userId);

        return createPostResponseDTO(updatedPost, likeCount, commentCount, userLiked, userBookmarked);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다.")); // IllegalArgumentException 사용

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다."); // IllegalArgumentException 사용
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public boolean toggleLike(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다.")); // IllegalArgumentException 사용

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
        // Post post = postRepository.findById(postId) // 게시글 존재 여부 확인 필요하다면 추가
        //         .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

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


    private PostResponseDTO createPostResponseDTO(
            Post post,
            long likeCount,
            long commentCount,
            boolean userLiked,
            boolean userBookmarked
    ) {
        String profileImageUrl = (post.getUser() != null && post.getUser().getProfileImageFile() != null)
                ? post.getUser().getProfileImageFile().getFileUrl()
                : null; // 또는 기본 이미지 URL 등을 설정

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .categoryName(post.getCategory().getDescription())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .authorProfileImageUrl(profileImageUrl)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .userLiked(userLiked)
                .userBookmarked(userBookmarked)
                .build();
    }
}