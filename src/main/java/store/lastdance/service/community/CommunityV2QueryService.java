package store.lastdance.service.community;

import store.lastdance.dto.community.post.PostResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CommunityV2QueryService {
    List<PostResponseDTO> getAllPosts(UUID currentUserId);
    PostResponseDTO getPostById(UUID postId, UUID currentUserId);
}
