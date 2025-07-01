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
    private long commentCount; // ✅ 이 줄 추가: 댓글 갯수 필드
    private boolean userLiked;
    private boolean userBookmarked;

    // 기존 from 메서드 유지 (댓글 갯수가 필요 없는 곳에서 사용할 수 있도록)
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

    // 기존 from 메서드 유지 (좋아요 갯수만 있는 경우)
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
                // .commentCount(0) // 기본값 0 또는 필요에 따라 생략 가능
                .build();
    }

    // ✅ commentCount를 추가한 from 메서드 (게시글 목록/상세 조회 시 사용)
    public static PostResponseDTO from(Post post, long likeCount, long commentCount, boolean userLiked, boolean userBookmarked) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount) // ✅ 댓글 갯수 설정
                .userLiked(userLiked)
                .userBookmarked(userBookmarked)
                .build();
    }
}