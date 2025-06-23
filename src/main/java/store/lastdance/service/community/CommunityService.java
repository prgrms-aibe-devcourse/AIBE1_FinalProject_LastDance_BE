package store.lastdance.service.community;

import store.lastdance.dto.community.CreatePostRequestDTO;
import store.lastdance.dto.community.UpdatePostRequestDTO;
import store.lastdance.dto.community.PostResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CommunityService {
    PostResponseDTO createPost(CreatePostRequestDTO request, UUID userId);
    List<PostResponseDTO> getAllPosts();
    PostResponseDTO getPostById(UUID postId);
    PostResponseDTO updatePost(UUID postId, UpdatePostRequestDTO request, UUID userId);
    void deletePost(UUID postId, UUID userId);
}
