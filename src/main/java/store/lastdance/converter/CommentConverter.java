package store.lastdance.converter;

import org.springframework.stereotype.Component;
import store.lastdance.domain.community.Comment;
import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;

import java.util.UUID;

@Component
public class CommentConverter {

    public Comment toEntity(CreateCommentRequestDTO request, UUID userId) {
        return Comment.builder()
                .commentId(UUID.randomUUID())
                .postId(request.getPostId())
                .userId(userId)
                .content(request.getContent())
                .build();
    }

    public CommentResponseDTO toResponseDTO(Comment comment) {
        if (comment == null) {
            return null;
        }
        return CommentResponseDTO.from(comment);
    }
}
