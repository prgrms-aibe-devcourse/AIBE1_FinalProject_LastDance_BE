package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.converter.CommentConverter;
import store.lastdance.domain.community.Comment;
import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;
import store.lastdance.dto.community.comment.UpdateCommentRequestDTO;
import store.lastdance.repository.community.CommentRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentV2ServiceImpl implements CommentV2Service {
    private final CommentRepository commentRepository;
    private final CommentConverter commentConverter;

    @Override
    public CommentResponseDTO createComment(CreateCommentRequestDTO request, UUID userId) {
        Comment comment = commentConverter.toEntity(request, userId);
        return commentConverter.toResponseDTO(commentRepository.save(comment));
    }

    @Override
    public CommentResponseDTO updateComment(UUID commentId, UpdateCommentRequestDTO request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }
        comment.updateContent(request.getContent());
        return commentConverter.toResponseDTO(commentRepository.save(comment));
    }

    @Override
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
    }
}