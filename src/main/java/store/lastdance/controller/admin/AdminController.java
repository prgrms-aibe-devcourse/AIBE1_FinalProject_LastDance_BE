package store.lastdance.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.admin.*;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.admin.AdminService;
import store.lastdance.dto.response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin", description = "관리자 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "관리자 인증",
            description = "현재 로그인한 사용자가 관리자 권한을 가지고 있는지 확인합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "관리자 인증 성공",
                    content = @Content(schema = @Schema(implementation = AdminVerifyResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/auth/verify")
    public ResponseEntity<ApiResponse<AdminVerifyResponseDTO>> verifyAdmin(
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("관리자 인증 요청: {}", user.getEmail());

        AdminVerifyResponseDTO response = adminService.verifyAdmin(user.getUserId());

        log.info("관리자 인증 응답: {}", response);

        return ResponseEntity.ok(ApiResponse.success(response, "관리자 인증 성공"));
    }

    @Operation(
            summary = "대시보드 통계 조회",
            description = "관리자 대시보드에서 사용할 전체 통계 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대시보드 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardStatsDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats(
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("대시보드 통계 요청: {}", user.getEmail());

        DashboardStatsDTO stats = adminService.getDashboardStats(user.getUserId());

        log.info("대시보드 통계 응답: {}", stats);

        return ResponseEntity.ok(ApiResponse.success(stats, "대시보드 통계 조회 성공"));
    }

    @Operation(
            summary = "회원 가입 추세 조회",
            description = "특정 기간 동안의 회원 가입 추세를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 가입 추세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SignupTrendDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 기간 파라미터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/dashboard/signup-trend")
    public ResponseEntity<ApiResponse<List<SignupTrendDTO>>> getSignupTrend(
            @Parameter(description = "조회 기간 (daily, weekly, monthly)") @RequestParam (required = false) String period,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("회원 가입 추세 요청: {}", user.getEmail());

        List<SignupTrendDTO> signupTrends = adminService.getSignupTrend(user.getUserId(), period);

        log.info("회원 가입 추세 응답: {}", signupTrends);

        return ResponseEntity.ok(ApiResponse.success(signupTrends, "회원 가입 추세 조회 성공"));
    }

    @Operation(
            summary = "사용자 관리 목록 조회",
            description = "사용자 목록을 조회합니다. 검색, 필터링, 정렬 기능을 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 관리 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserManagementResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserManagementResponseDTO>> getUserManagement(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "검색 키워드 (이메일, 닉네임, 사용자명)") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "활성 상태 필터") @RequestParam(value = "isActive", required = false) Boolean isActive,
            @Parameter(description = "정지 상태 필터") @RequestParam(value = "isBanned", required = false) Boolean isBanned,
            @Parameter(description = "OAuth 제공자 필터") @RequestParam(value = "provider", required = false) String provider,
            @Parameter(description = "사용자 역할 필터") @RequestParam(value = "role", required = false) UserRole role,
            @Parameter(description = "정렬 기준 필드") @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "정렬 순서 (asc, desc)") @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("사용자 관리 요청: page={}, limit={}, search={}, isActive={}, isBanned={}, provider={}, role={}, sortBy={}, sortOrder={}, adminEmail={}",
                page, limit, search, isActive, isBanned, provider, role, sortBy, sortOrder, user.getEmail());

        UserManagementResponseDTO userManagementList = adminService.getUserManagement(
                user.getUserId(), page, limit, search, isActive, isBanned, provider, role, sortBy, sortOrder
        );

        if (userManagementList != null && userManagementList.users() != null) {
            log.info("사용자 관리 응답: {}건 조회", userManagementList.users().size());
        }

        return ResponseEntity.ok(ApiResponse.success(userManagementList, "사용자 관리 조회 성공"));
    }

    @Operation(
            summary = "사용자 관리 상세 조회",
            description = "특정 사용자의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 관리 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserManagementDetailDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserManagementDetailDTO>> getUserManagementDetail(
            @Parameter(description = "조회할 사용자 ID", required = true) @PathVariable UUID userId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("사용자 관리 상세 요청: userId={}, adminEmail={}", userId, user.getEmail());

        UserManagementDetailDTO userDetail = adminService.getUserManagementDetail(user.getUserId(), userId);

        log.info("사용자 관리 상세 응답: {}", userDetail);

        return ResponseEntity.ok(ApiResponse.success(userDetail, "사용자 관리 상세 조회 성공"));
    }

    @Operation(
            summary = "사용자 정지",
            description = "특정 사용자를 정지시킵니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 정지 성공",
                    content = @Content(schema = @Schema(implementation = BanResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 정지된 사용자 또는 잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<BanResponseDTO>> banUser(
            @Parameter(description = "정지할 사용자 ID", required = true) @PathVariable UUID userId,
            @Parameter(description = "정지 요청 정보", required = true) @RequestBody BanRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user
            ) {

        log.info("사용자 차단 요청: userId={}, adminEmail={}", userId, user.getEmail());

        BanResponseDTO banResponse = adminService.banUser(user.getUserId(), userId, request);

        log.info("사용자 차단 응답: {}", banResponse);

        return ResponseEntity.ok(ApiResponse.success(banResponse, "사용자 차단 성공"));
    }

    @Operation(
            summary = "사용자 정지 해제",
            description = "정지된 사용자의 정지를 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 정지 해제 성공",
                    content = @Content(schema = @Schema(implementation = UnbanResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "정지되지 않은 사용자 또는 잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/users/{userId}/unban")
    public ResponseEntity<ApiResponse<UnbanResponseDTO>> unbanUser(
            @Parameter(description = "정지 해제할 사용자 ID", required = true) @PathVariable UUID userId,
            @Parameter(description = "정지 해제 요청 정보", required = true) @RequestBody UnbanRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("사용자 차단 해제 요청: userId={}, adminEmail={}", userId, user.getEmail());

        UnbanResponseDTO unbanResponse = adminService.unbanUser(user.getUserId(), userId, request);

        log.info("사용자 차단 해제 응답: {}", unbanResponse);

        return ResponseEntity.ok(ApiResponse.success(unbanResponse, "사용자 차단 해제 성공"));
    }

    @Operation(
            summary = "신고 관리 목록 조회",
            description = "신고 목록을 조회합니다. 상태, 유형, 날짜 범위 등으로 필터링할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "신고 관리 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReportManagementResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<ReportManagementResponseDTO>> getReportManagement(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "신고 상태 필터") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "신고 유형 필터") @RequestParam(value = "reportType", required = false) String reportType,
            @Parameter(description = "신고자 ID 필터") @RequestParam(value = "reporterId", required = false) UUID reporterId,
            @Parameter(description = "신고 대상자 ID 필터") @RequestParam(value = "reportedUserId", required = false) UUID reportedUserId,
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)") @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @Parameter(description = "종료 날짜 (YYYY-MM-DD)") @RequestParam(value = "dateTo", required = false) String dateTo,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("신고 관리 요청: page={}, limit={}, status={}, reportType={}, reporterId={}, reportedUserId={}, dateFrom={}, dateTo={}, adminEmail={}",
                page, limit, status, reportType, reporterId, reportedUserId, dateFrom, dateTo, user.getEmail());

        ReportManagementResponseDTO reports = adminService.getReportManagement(
                user.getUserId(), page, limit, status, reportType, reporterId, reportedUserId, dateFrom, dateTo
        );

        if (reports != null && reports.reportManagements() != null) {
            log.info("신고 관리 응답: {}건 조회", reports.reportManagements().size());
        }

        return ResponseEntity.ok(ApiResponse.success(reports, "신고 관리 조회 성공"));
    }

    @Operation(
            summary = "신고 관리 상세 조회",
            description = "특정 신고의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "신고 관리 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReportManagementDetailDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<ReportManagementDetailDTO>> getReportManagementDetail(
            @Parameter(description = "조회할 신고 ID", required = true) @PathVariable Long reportId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("신고 관리 상세 요청: reportId={}, adminEmail={}", reportId, user.getEmail());

        ReportManagementDetailDTO reportDetail = adminService.getReportManagementDetail(user.getUserId(), reportId);

        log.info("신고 관리 상세 응답: {}", reportDetail);

        return ResponseEntity.ok(ApiResponse.success(reportDetail, "신고 관리 상세 조회 성공"));
    }

    @Operation(
            summary = "신고 처리",
            description = "신고를 처리합니다. 상태 변경 및 사용자 정지 등의 조치를 취할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "신고 처리 성공",
                    content = @Content(schema = @Schema(implementation = ReportProcessResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @PatchMapping("/reports/{reportId}/process")
    public ResponseEntity<ApiResponse<ReportProcessResponseDTO>> processReport(
            @Parameter(description = "처리할 신고 ID", required = true) @PathVariable Long reportId,
            @Parameter(description = "신고 처리 요청 정보", required = true) @RequestBody ReportProcessRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("신고 처리 요청: reportId={}, adminEmail={}", reportId, user.getEmail());

        ReportProcessResponseDTO processResponse = adminService.processReport(user.getUserId(), reportId, request);

        log.info("신고 처리 응답: {}", processResponse);

        return ResponseEntity.ok(ApiResponse.success(processResponse, "신고 처리 성공"));
    }

    @Operation(
            summary = "AI 판단 목록 조회",
            description = "AI 판단 결과 목록을 조회합니다. 평가, 카테고리, 날짜 범위 등으로 필터링할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 판단 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = AiJudgmentResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/ai/judgment")
    public ResponseEntity<ApiResponse<AiJudgmentResponseDTO>> getAiJudgment(
            @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "검색 키워드 (사용자 이메일, 닉네임)") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "평가 필터 (UP, DOWN)") @RequestParam(value = "rating", required = false) String rating,
            @Parameter(description = "카테고리 필터") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "요청 유형 필터") @RequestParam(value = "requestType", required = false) String requestType,
            @Parameter(description = "시작 날짜 (YYYY-MM-DD)") @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @Parameter(description = "종료 날짜 (YYYY-MM-DD)") @RequestParam(value = "dateTo", required = false) String dateTo,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("AI 피드백 요청: page={}, limit={}, search={}, rating={}, category={}, requestType={}, dateFrom={}, dateTo={}, adminEmail={}",
                page, limit, search, rating, category, requestType, dateFrom, dateTo, user.getEmail());

        AiJudgmentResponseDTO judgmentList = adminService.getAiJudgment(
                user.getUserId(), page, limit, search, rating, dateFrom, dateTo
        );

        if (judgmentList != null && judgmentList.aiJudgments() != null) {
            log.info("AI 피드백 응답: {}건 조회", judgmentList.aiJudgments().size());
        }

        return ResponseEntity.ok(ApiResponse.success(judgmentList, "AI 피드백 조회 성공"));
    }

    @Operation(
            summary = "AI 판단 상세 조회",
            description = "특정 AI 판단의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 판단 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = AiJudgmentDetailDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "AI 판단을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/ai/judgment/{judgmentId}")
    public ResponseEntity<ApiResponse<AiJudgmentDetailDTO>> getAiJudgmentDetail(
            @Parameter(description = "조회할 AI 판단 ID", required = true) @PathVariable UUID judgmentId,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("AI 피드백 상세 요청: judgmentId={}, adminEmail={}", judgmentId, user.getEmail());

        AiJudgmentDetailDTO judgmentDetail = adminService.getAiJudgmentDetail(user.getUserId(), judgmentId);

        log.info("AI 피드백 상세 응답: {}", judgmentDetail);

        return ResponseEntity.ok(ApiResponse.success(judgmentDetail, "AI 피드백 상세 조회 성공"));
    }

    @Operation(
            summary = "AI 판단 통계 조회",
            description = "AI 판단에 대한 통계 정보를 조회합니다. 만족도, 불만족 카운트, 추세 등을 포함합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 판단 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = AiJudgmentStatsDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 기간 파라미터",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/ai/judgment/stats")
    public ResponseEntity<ApiResponse<AiJudgmentStatsDTO>> getAiJudgmentStats(
            @Parameter(description = "조회 기간 (daily, weekly, monthly)") @RequestParam(value = "period", defaultValue = "weekly") String period,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("AI 피드백 통계 요청: period={}, adminEmail={}", period, user.getEmail());

        AiJudgmentStatsDTO stats = adminService.getAiJudgmentStats(user.getUserId(), period);

        log.info("AI 피드백 통계 응답: {}", stats);

        return ResponseEntity.ok(ApiResponse.success(stats, "AI 피드백 통계 조회 성공"));
    }

}
