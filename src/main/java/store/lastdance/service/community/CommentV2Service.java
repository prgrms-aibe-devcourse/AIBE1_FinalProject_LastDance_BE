package store.lastdance.service.community;

import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;
import store.lastdance.dto.community.comment.UpdateCommentRequestDTO;

import java.util.UUID;

public interface CommentV2Service {
    CommentResponseDTO createComment(CreateCommentRequestDTO request, UUID userId);
    CommentResponseDTO updateComment(UUID commentId, UpdateCommentRequestDTO request, UUID userId);
    void deleteComment(UUID commentId, UUID userId);
}