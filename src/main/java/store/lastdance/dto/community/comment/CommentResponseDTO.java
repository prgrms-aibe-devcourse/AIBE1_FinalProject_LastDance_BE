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
    private String authorNickname; // ✅ username 대신 authorNickname으로 변경
    private String content;
    private Integer reportCount;
    private LocalDateTime createdAt;

    public static CommentResponseDTO from(Comment comment) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorNickname(comment.getUser() != null ? comment.getUser().getNickname() : null) // ✅ username 대신 nickname을 가져오도록 수정
                .content(comment.getContent())
                .reportCount(comment.getReportCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}