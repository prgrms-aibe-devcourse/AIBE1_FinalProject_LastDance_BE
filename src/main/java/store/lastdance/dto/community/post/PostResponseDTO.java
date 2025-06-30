package store.lastdance.dto.community.post;

import lombok.Builder;
import lombok.Getter;
import store.lastdance.domain.community.Post;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PostResponseDTO {
    private UUID postId;
    private String title;
    private String content;
    private UUID authorId;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private boolean userLiked;

    public static PostResponseDTO from(Post post) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static PostResponseDTO from(Post post, long likeCount, boolean userLiked) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .userLiked(userLiked)
                .build();
    }
}