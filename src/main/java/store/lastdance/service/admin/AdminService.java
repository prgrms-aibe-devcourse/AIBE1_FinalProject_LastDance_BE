package store.lastdance.service.admin;

import store.lastdance.domain.admin.ReportStatus;
import store.lastdance.domain.admin.ReportType;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.admin.*;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    AdminVerifyResponseDTO verifyAdmin(UUID userId);

    DashboardStatsDTO getDashboardStats(UUID userId);

    List<SignupTrendDTO> getSignupTrend(UUID userId, String period);

    UserManagementResponseDTO getUserManagement(UUID userId, int page, int limit, String search, Boolean isActive, Boolean isBanned, String provider, UserRole role, String sortBy, String sortOrder);

    UserManagementDetailDTO getUserManagementDetail(UUID AdminId, UUID userId);

    BanResponseDTO banUser(UUID AdminId, UUID userId, BanRequestDTO request);

    UnbanResponseDTO unbanUser(UUID AdminId, UUID userId, UnbanRequestDTO request);

    ReportManagementResponseDTO getReportManagement(UUID userId, int page, int limit, ReportStatus status, ReportType reportType, String reason, String reporterNickname, String reporterEmail, String reportedUserNickname, String reportedUserEmail, String dateFrom, String dateTo);

    ReportManagementDetailDTO getReportManagementDetail(UUID userId, Long reportId);

    ReportProcessResponseDTO processReport(UUID userId, Long reportId, ReportProcessRequestDTO request);

    AiJudgmentResponseDTO getAiJudgment(UUID userId, int page, int limit, String search, String rating, String dateFrom, String dateTo);

    AiJudgmentDetailDTO getAiJudgmentDetail(UUID userId, UUID judgmentId);

    AiJudgmentStatsDTO getAiJudgmentStats(UUID userId, String period);
}
