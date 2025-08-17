package store.lastdance.service.community;

import store.lastdance.dto.community.comment.CommentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CommentV2QueryService {
    List<CommentResponseDTO> getCommentsByPostId(UUID postId);
    CommentResponseDTO getCommentById(UUID commentId);
}
