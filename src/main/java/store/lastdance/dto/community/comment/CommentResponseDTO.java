package store.lastdance.dto.community.comment;

import lombok.Builder;
import lombok.Getter;
import store.lastdance.domain.community.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CommentResponseDTO {
    private UUID commentId;
    private UUID postId;
    private UUID userId;
    private String username;
    private String content;
    private Integer reportCount;
    private LocalDateTime createdAt;

    public static CommentResponseDTO from(Comment comment) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .username(comment.getUser() != null ? comment.getUser().getUsername() : null)
                .content(comment.getContent())
                .reportCount(comment.getReportCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}