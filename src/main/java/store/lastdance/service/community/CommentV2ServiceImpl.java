package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.CommentConverter;
import store.lastdance.domain.community.Comment;
import store.lastdance.domain.community.Post;
import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.dto.community.comment.CreateCommentRequestDTO;
import store.lastdance.dto.community.comment.UpdateCommentRequestDTO;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.community.PostRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentV2ServiceImpl implements CommentV2Service {
    private final CommentRepository commentRepository;
    private final CommentConverter commentConverter;

    private final PostRepository postRepository;

    @Override
    @Transactional
    public CommentResponseDTO createComment(CreateCommentRequestDTO request, UUID userId) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        Comment comment = commentConverter.toEntity(request, userId);
        post.incrementCommentCount();
        postRepository.save(post);
        return commentConverter.toResponseDTO(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentResponseDTO updateComment(UUID commentId, UpdateCommentRequestDTO request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }
        comment.updateContent(request.getContent());
        return commentConverter.toResponseDTO(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        post.decrementCommentCount();
        postRepository.save(post);
        commentRepository.delete(comment);
    }
}