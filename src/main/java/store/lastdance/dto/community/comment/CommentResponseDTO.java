package store.lastdance.dto.community.comment;

import lombok.Builder;
import lombok.Getter;
import store.lastdance.domain.community.Comment; // Comment 엔티티 임포트
import store.lastdance.domain.user.User;       // User 엔티트 임포트
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CommentResponseDTO {
    private UUID commentId;
    private UUID postId;
    private UUID userId;
    private String authorNickname;
    private String authorProfileImageUrl; // ⭐ 이 줄을 추가합니다.
    private String content;
    private Integer reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // ⭐ updatedAt 추가 (프론트엔드 인터페이스와 일치)

    public static CommentResponseDTO from(Comment comment) {
        User author = comment.getUser(); // Comment 엔티티에서 User 객체를 가져옵니다.

        // PostResponseDTO와 동일하게 ProfileImageFile에서 URL을 가져오는 로직
        String profileImageUrl = (author != null && author.getProfileImageFile() != null)
                ? author.getProfileImageFile().getFileUrl()
                : null; // 또는 기본 이미지 URL 등을 설정할 수 있습니다.

        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorNickname(author != null ? author.getNickname() : null) // User 객체가 있으면 nickname 가져오기
                .authorProfileImageUrl(profileImageUrl) // ⭐ PostResponseDTO처럼 ProfileImageFile에서 가져오도록 수정
                .content(comment.getContent())
                .reportCount(comment.getReportCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt()) // ⭐ Comment 엔티티에 updatedAt 필드가 있다면
                .build();
    }
}