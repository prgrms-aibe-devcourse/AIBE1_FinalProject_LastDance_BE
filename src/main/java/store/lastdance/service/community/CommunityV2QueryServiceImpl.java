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

import java.util.List;
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
        return postRepository.findAll().stream()
                .map(post -> {
                    long likeCount = likeRepository.countByPostId(post.getPostId());
                    long commentCount = commentRepository.countByPostId(post.getPostId());
                    boolean userLiked = likeRepository.findByPostIdAndUserId(post.getPostId(), currentUserId).isPresent();
                    boolean userBookmarked = bookmarkRepository.existsByPostIdAndUserId(post.getPostId(), currentUserId);
                    return postConverter.toResponseDTO(post, likeCount, commentCount, userLiked, userBookmarked);
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
        return postConverter.toResponseDTO(post, likeCount, commentCount, userLiked, userBookmarked);
    }
}
