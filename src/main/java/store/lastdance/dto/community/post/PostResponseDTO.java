package store.lastdance.dto.community.post;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PostResponseDTO {
    private UUID postId;
    private String title;
    private String content;
    private String category;
    private String categoryName;
    private UUID authorId;
    private String authorNickname;
    private String authorProfileImageUrl; // 추가: 작성자 프로필 이미지 URL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private long commentCount;
    private boolean userLiked;
    private boolean userBookmarked;
}