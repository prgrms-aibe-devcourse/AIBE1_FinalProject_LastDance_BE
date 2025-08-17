package store.lastdance.service.community;

import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.dto.community.post.UpdatePostRequestDTO;

import java.util.List;
import java.util.UUID;

public interface CommunityV2Service {
    PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId);

    // ✅ 파라미터 있는 메서드만 남김
    List<PostResponseDTO> getAllPosts(UUID currentUserId);

    PostResponseDTO getPostById(UUID postId, UUID currentUserId);

    PostResponseDTO updatePost(UUID postId, UpdatePostRequestDTO request, UUID userId);

    void deletePost(UUID postId, UUID userId);

    boolean toggleLike(UUID postId, UUID userId);

    boolean toggleBookmark(UUID postId, UUID userId);

}
