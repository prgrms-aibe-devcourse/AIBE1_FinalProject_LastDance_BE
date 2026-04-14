package store.lastdance.service.community;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.converter.PostConverter;
import store.lastdance.domain.community.Bookmark;
import store.lastdance.domain.community.Like;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.user.User;
import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.dto.community.post.UpdatePostRequestDTO;
import store.lastdance.repository.community.BookmarkRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.community.LikeRepository;
import store.lastdance.repository.community.PostRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityV2ServiceImpl implements CommunityV2Service {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final PostConverter postConverter;

    @Override
    @Transactional
    public PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Post post = postConverter.toEntity(request, user);
        Post savedPost = postRepository.save(post);
        return postConverter.toResponseDTO(savedPost, 0, 0, false, false);
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

        if (request.getCategory() != null) {
            post.updateCategory(request.getCategory());
        }

        Post updatedPost = postRepository.save(post);

        boolean userLiked = likeRepository.findByPostIdAndUserId(postId, userId).isPresent();
        boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, userId);

        return postConverter.toResponseDTO(
                updatedPost, 
                updatedPost.getLikeCount(), 
                updatedPost.getCommentCount(), 
                userLiked, 
                userBookmarked
        );
    }

    @Override
    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public boolean toggleLike(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

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