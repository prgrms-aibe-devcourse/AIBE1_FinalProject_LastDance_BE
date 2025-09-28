package store.lastdance.converter;

import org.springframework.stereotype.Component;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.community.PostCategory;
import store.lastdance.domain.user.User;
import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;

import java.util.UUID;

@Component
public class PostConverter {

    public Post toEntity(CreatePostRequestDTO request, User user) {
        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .title(request.getTitle())
                .content(request.getContent())
                .category(PostCategory.valueOf(String.valueOf(request.getCategory())))
                .userId(user.getUserId())
                .build();
        post.setUser(user);
        return post;
    }

    public PostResponseDTO toResponseDTO(Post post, long likeCount, long commentCount, boolean userLiked, boolean userBookmarked) {
        if (post == null) {
            return null;
        }

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
                .authorProfileImageUrl(profileImageUrl)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .userLiked(userLiked)
                .userBookmarked(userBookmarked)
                .isDeleted(post.getIsDeleted())
                .build();
    }
}