package store.lastdance.dto.admin;

public record TargetContentDTO(
        String type,
        String postId,
        String title,
        String content,
        String category,
        Integer likeCount,
        Integer reportCount,
        String createdAt
) {
}
