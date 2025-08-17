package store.lastdance.service.community;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.admin.Report;
import store.lastdance.domain.admin.ReportType;
import store.lastdance.domain.community.Comment;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.user.User;
import store.lastdance.dto.community.report.ReportRequestDTO;
import store.lastdance.dto.community.report.ReportResponseDTO;
import store.lastdance.repository.admin.ReportRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.community.PostRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportV2ServiceImpl implements ReportV2Service {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ReportResponseDTO createReport(ReportRequestDTO request, UUID reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        UUID reportedUserId;
        if (request.reportType() == ReportType.POST) {
            Post post = postRepository.findById(request.targetId())
                    .orElseThrow(() -> new IllegalArgumentException("신고 대상 게시글을 찾을 수 없습니다."));
            reportedUserId = post.getUserId();
        } else if (request.reportType() == ReportType.COMMENT) {
            Comment comment = commentRepository.findById(request.targetId())
                    .orElseThrow(() -> new IllegalArgumentException("신고 대상 댓글을 찾을 수 없습니다."));
            reportedUserId = comment.getUserId();
        } else {
            throw new IllegalArgumentException("지원하지 않는 신고 타입입니다.");
        }

        if (reporterId.equals(reportedUserId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reportType(request.reportType())
                .targetId(request.targetId())
                .reason(request.reason())
                .build();

        Report savedReport = reportRepository.save(report);

        return new ReportResponseDTO(
                savedReport.getReportId(),
                savedReport.getReporterId(),
                savedReport.getReportedUserId(),
                savedReport.getReportType(),
                savedReport.getTargetId(),
                savedReport.getReason(),
                savedReport.getStatus(),
                savedReport.getAdminId(),
                savedReport.getAdminComment(),
                savedReport.getProcessedAt(),
                savedReport.getCreatedAt(),
                savedReport.getUpdatedAt()
        );
    }
}