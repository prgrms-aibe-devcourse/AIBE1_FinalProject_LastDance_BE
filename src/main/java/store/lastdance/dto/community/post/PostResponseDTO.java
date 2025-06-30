package store.lastdance.dto.community.post;

import lombok.Builder;
import lombok.Getter;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.community.PostCategory;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PostResponseDTO {

    private UUID postId;
    private String title;
    private String content;
    private PostCategory category;
    private UUID userId;
    private String authorNickname; // ✅ username 대신 authorNickname으로 변경
    private Integer likeCount;
    private Integer reportCount;
    private LocalDateTime createdAt;

    public static PostResponseDTO from(Post post) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .userId(post.getUserId())
                .authorNickname(post.getUser() != null ? post.getUser().getNickname() : null) // ✅ username 대신 nickname을 가져오도록 수정
                .likeCount(post.getLikeCount())
                .reportCount(post.getReportCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}