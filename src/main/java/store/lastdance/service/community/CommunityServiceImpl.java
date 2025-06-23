package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.community.Post;
import store.lastdance.dto.community.CreatePostRequestDTO;
import store.lastdance.dto.community.UpdatePostRequestDTO;
import store.lastdance.dto.community.PostResponseDTO;
import store.lastdance.repository.community.PostRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private final PostRepository postRepository;

    @Override
    public PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId) {
        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .userId(userId)
                .build();

        return PostResponseDTO.from(postRepository.save(post));
    }

    @Override
    public List<PostResponseDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public PostResponseDTO getPostById(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        return PostResponseDTO.from(post);
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
        post.updateCategory(request.getCategory());

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
}
