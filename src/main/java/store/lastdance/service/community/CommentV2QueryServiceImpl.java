package store.lastdance.service.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.dto.community.comment.CommentResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.community.CommentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentV2QueryServiceImpl implements CommentV2QueryService {
    private final CommentRepository commentRepository;

    @Override
    public List<CommentResponseDTO> getCommentsByPostId(UUID postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(CommentResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDTO getCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(CommentResponseDTO::from)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
