// Service Implementation

package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.domain.community.Comment;
import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;
import store.lastdance.dto.community.comment.UpdateCommentRequestDTO;
import store.lastdance.repository.community.CommentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;

    @Override
    public CommentResponseDTO createComment(CreateCommentRequestDTO request, UUID userId) {
        Comment comment = Comment.builder()
                .commentId(UUID.randomUUID())
                .postId(request.getPostId())
                .userId(userId)
                .content(request.getContent())
                .build();
        return CommentResponseDTO.from(commentRepository.save(comment));
    }

    @Override
    public List<CommentResponseDTO> getCommentsByPostId(UUID postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(CommentResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDTO updateComment(UUID commentId, UpdateCommentRequestDTO request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }
        comment.updateContent(request.getContent());
        return CommentResponseDTO.from(commentRepository.save(comment));
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

    @Override
    public CommentResponseDTO getCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(CommentResponseDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }
}