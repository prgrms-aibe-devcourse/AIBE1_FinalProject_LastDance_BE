package store.lastdance.service.community;

import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.dto.community.post.UpdatePostRequestDTO;

import java.util.UUID;

public interface CommunityV2Service {
    PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId);
    PostResponseDTO updatePost(UUID postId, UpdatePostRequestDTO request, UUID userId);
    void deletePost(UUID postId, UUID userId);
    boolean toggleLike(UUID postId, UUID userId);
    boolean toggleBookmark(UUID postId, UUID userId);
}