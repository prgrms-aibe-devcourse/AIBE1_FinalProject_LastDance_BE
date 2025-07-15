package store.lastdance.controller.community;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.community.report.ReportRequestDTO;
import store.lastdance.dto.community.report.ReportResponseDTO;
import store.lastdance.dto.community.post.CreatePostRequestDTO;
import store.lastdance.dto.community.post.UpdatePostRequestDTO;
import store.lastdance.dto.community.post.PostResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.community.ReportService;
import store.lastdance.service.community.CommunityService;
import java.util.List;
import java.util.UUID;

@Tag(name = "Community", description = "커뮤니티 관리 API")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor

public class CommunityController {

    private final CommunityService communityService;
    private final ReportService reportService;

    @Operation(
            summary = "게시글 작성",
            description = "새로운 게시글을 작성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponseDTO>> createPost(
            @Valid @RequestBody CreatePostRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            PostResponseDTO response = communityService.createPost(request, user.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "게시글이 성공적으로 작성되었습니다."));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("게시글 작성에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "전체 게시글 목록 조회",
            description = "모든 게시글을 최신순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDTO>>> getAllPosts(
            @AuthenticationPrincipal CustomOAuth2User user) {
        try {
            List<PostResponseDTO> posts = communityService.getAllPosts(user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(posts));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("게시글 목록 조회에 실패했습니다."));
        }
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "특정 게시글의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> getPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomOAuth2User user) {
        try {
            PostResponseDTO response = communityService.getPostById(postId, user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("게시글을 찾을 수 없습니다."));
        }
    }

    @Operation(
            summary = "게시글 수정",
            description = "기존 게시글의 내용을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            PostResponseDTO response = communityService.updatePost(postId, request, user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(response, "게시글이 성공적으로 수정되었습니다."));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("게시글 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "게시글 삭제",
            description = "기존 게시글을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            communityService.deletePost(postId, user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null, "게시글이 성공적으로 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("게시글 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "게시글 좋아요/좋아요 취소",
            description = "특정 게시글에 좋아요를 누르거나 취소합니다. 이미 좋아요를 눌렀다면 취소되고, 누르지 않았다면 좋아요가 추가됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            boolean isLiked = communityService.toggleLike(postId, user.getUserId());
            String message = isLiked ? "게시글에 좋아요가 적용되었습니다." : "게시글 좋아요가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponse.success(isLiked, message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("좋아요 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "게시글 북마크/북마크 취소",
            description = "특정 게시글을 북마크하거나 북마크를 취소합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{postId}/bookmarks")
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            boolean isBookmarked = communityService.toggleBookmark(postId, user.getUserId());
            String message = isBookmarked ? "북마크가 추가되었습니다." : "북마크가 취소되었습니다.";
            return ResponseEntity.ok(ApiResponse.success(isBookmarked, message));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("북마크 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "게시글/댓글 신고",
            description = "특정 게시글 또는 댓글을 신고합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/report")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> reportContent(
            @Valid @RequestBody ReportRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {


        try {
            ReportResponseDTO response = reportService.createReport(request, user.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "콘텐츠가 성공적으로 신고되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("콘텐츠 신고에 실패했습니다: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("콘텐츠 신고 중 오류가 발생했습니다."));
        }
    }
}