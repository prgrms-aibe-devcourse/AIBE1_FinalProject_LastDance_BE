package store.lastdance.dto.community;

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
    private String username;
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
                .username(post.getUser() != null ? post.getUser().getUsername() : null)
                .likeCount(post.getLikeCount())
                .reportCount(post.getReportCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
