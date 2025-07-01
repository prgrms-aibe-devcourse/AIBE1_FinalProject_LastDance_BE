package store.lastdance.dto.community.post;

import lombok.Builder;
import lombok.Getter;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.community.PostCategory; // PostCategory Enum을 임포트합니다.

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private long commentCount;
    private boolean userLiked;
    private boolean userBookmarked;

    // 1. 카테고리 정보를 포함하는 기본 from 메서드 (게시글 생성 직후 등에 사용)
    public static PostResponseDTO from(Post post) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name()) // PostCategory의 name()으로 영문 ID 설정
                .categoryName(post.getCategory().getDescription()) // PostCategory의 getDescription()으로 한글 이름 설정
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // 2. 좋아요 갯수만 있는 경우 (필요 시 사용)
    // 이 메서드는 모든 조회에 commentCount가 포함될 경우 더 이상 사용되지 않을 수 있습니다.
    public static PostResponseDTO from(Post post, long likeCount, boolean userLiked) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .categoryName(post.getCategory().getDescription())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .userLiked(userLiked)
                .build();
    }

    // 3. 좋아요, 댓글 갯수, 사용자 좋아요/북마크 여부를 모두 포함하는 경우 (게시글 목록/상세 조회 시 주로 사용)
    public static PostResponseDTO from(Post post, long likeCount, long commentCount, boolean userLiked, boolean userBookmarked) {
        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name()) // PostCategory의 name()으로 영문 ID 설정
                .categoryName(post.getCategory().getDescription()) // PostCategory의 getDescription()으로 한글 이름 설정
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .userLiked(userLiked)
                .userBookmarked(userBookmarked)
                .build();
    }
}