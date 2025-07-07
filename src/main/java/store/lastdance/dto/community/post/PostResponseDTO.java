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
    private String authorProfileImageUrl; // 추가: 작성자 프로필 이미지 URL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private long commentCount;
    private boolean userLiked;
    private boolean userBookmarked;

    // 1. 카테고리 정보를 포함하는 기본 from 메서드 (게시글 생성 직후 등에 사용)
    public static PostResponseDTO from(Post post) {
        // null 체크 추가: user나 profileImageFile이 null일 수 있으므로 방어 코드 작성
        String profileImageUrl = (post.getUser() != null && post.getUser().getProfileImageFile() != null)
                ? post.getUser().getProfileImageFile().getFileUrl()
                : null; // 또는 기본 이미지 URL 등을 설정

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .categoryName(post.getCategory().getDescription())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .authorProfileImageUrl(profileImageUrl) // 추가: 프로필 이미지 URL 설정
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // 2. 좋아요 갯수만 있는 경우 (필요 시 사용)
    public static PostResponseDTO from(Post post, long likeCount, boolean userLiked) {
        String profileImageUrl = (post.getUser() != null && post.getUser().getProfileImageFile() != null)
                ? post.getUser().getProfileImageFile().getFileUrl()
                : null;

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .categoryName(post.getCategory().getDescription())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .authorProfileImageUrl(profileImageUrl) // 추가
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .userLiked(userLiked)
                .build();
    }

    // 3. 좋아요, 댓글 갯수, 사용자 좋아요/북마크 여부를 모두 포함하는 경우 (게시글 목록/상세 조회 시 주로 사용)
    public static PostResponseDTO from(Post post, long likeCount, long commentCount, boolean userLiked, boolean userBookmarked) {
        String profileImageUrl = (post.getUser() != null && post.getUser().getProfileImageFile() != null)
                ? post.getUser().getProfileImageFile().getFileUrl()
                : null;

        return PostResponseDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .categoryName(post.getCategory().getDescription())
                .authorId(post.getUser().getUserId())
                .authorNickname(post.getUser().getNickname())
                .authorProfileImageUrl(profileImageUrl) // 추가
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .userLiked(userLiked)
                .userBookmarked(userBookmarked)
                .build();
    }
}