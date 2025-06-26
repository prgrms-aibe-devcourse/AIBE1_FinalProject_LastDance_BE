package store.lastdance.service.community;

import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;
import store.lastdance.dto.community.comment.UpdateCommentRequestDTO;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    CommentResponseDTO createComment(CreateCommentRequestDTO request, UUID userId);
    List<CommentResponseDTO> getCommentsByPostId(UUID postId);
    CommentResponseDTO updateComment(UUID commentId, UpdateCommentRequestDTO request, UUID userId);
    void deleteComment(UUID commentId, UUID userId);
}
