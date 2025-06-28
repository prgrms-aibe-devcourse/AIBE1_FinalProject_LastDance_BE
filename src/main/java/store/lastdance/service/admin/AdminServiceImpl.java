package store.lastdance.service.admin;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import store.lastdance.domain.admin.Report;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.dto.admin.*;
import store.lastdance.dto.admin.stats.DashboardContentStats;
import store.lastdance.dto.admin.stats.DashboardUserStats;
import store.lastdance.dto.admin.stats.UserStats;
import store.lastdance.repository.aijudement.AiJudgmentRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.repository.community.PostRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.admin.ReportRepository;
import jakarta.persistence.criteria.Predicate;
import store.lastdance.service.user.UserService;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    // Constants
    private static final int NEW_USER_DAYS_CRITERIA = 7;
    private static final int DAILY_STATS_DAYS_CRITERIA = 1;
    private static final String DAILY_PERIOD = "daily";
    private static final String WEEKLY_PERIOD = "weekly";
    private static final String MONTHLY_PERIOD = "monthly";
    private static final String UP_RATING = "UP";
    private static final String DOWN_RATING = "DOWN";
    private static final String SORT_ORDER_ASC = "asc";
    private static final String SORT_FIELD_EMAIL = "email";
    private static final String SORT_FIELD_CREATED_AT = "createdat";
    private static final String SORT_FIELD_UPDATED_AT = "updatedat";
    private static final String SORT_FIELD_NICKNAME = "nickname";
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ReportRepository reportRepository;
    private final AiJudgmentRepository AiJudgmentRepository;

    @Override
    public AdminVerifyResponseDTO verifyAdmin(UUID userId) {

        log.info("관리자 인증 로직 실행: userId={}", userId);

        if (userId == null) {
            log.error("유효하지 않은 userId: {}", userId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userService.findByActiveUser(userId);

        validateAdmin(userId);

        log.info("관리자 인증 성공: userId={}", userId);

        return new AdminVerifyResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );
    }

    // 관리자 권한 확인 메서드
    private void validateAdmin(UUID userId) {
        log.info("관리자 권한 확인: userId={}", userId);

        User user = userService.findByActiveUser(userId);

        if (user.getRole() != UserRole.ADMIN) {
            log.warn("관리자 권한이 없는 사용자 접근 시도: userId={}, role={}", userId, user.getRole());
            throw new CustomException("관리자 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        log.info("관리자 권한 확인 성공: userId={}", userId);
    }

    @Override
    public DashboardStatsDTO getDashboardStats(UUID userId) {
        log.info("대시보드 통계 조회: userId={}", userId);
        
        validateAdmin(userId);
        
        DashboardUserStats userStats = createDashboardUserStats();
        DashboardContentStats contentStats = createDashboardContentStats();
        
        log.info("대시보드 통계 조회 성공: userId={}", userId);
        
        return new DashboardStatsDTO(userStats, contentStats);
    }

    private DashboardUserStats createDashboardUserStats() {
        LocalDateTime newUserCriteria = LocalDateTime.now().minusDays(NEW_USER_DAYS_CRITERIA);
        
        return new DashboardUserStats(
                userRepository.count(),
                userRepository.countByIsActiveTrue(),
                userRepository.countByIsBannedTrue(),
                userRepository.countAllByCreatedAtAfter(newUserCriteria)
        );
    }

    private DashboardContentStats createDashboardContentStats() {
        LocalDateTime dailyStatsCriteria = LocalDateTime.now().minusDays(DAILY_STATS_DAYS_CRITERIA);
        
        return new DashboardContentStats(
                postRepository.count(),
                commentRepository.count(),
                postRepository.countByCreatedAtAfter(dailyStatsCriteria),
                commentRepository.countByCreatedAtAfter(dailyStatsCriteria)
        );
    }

    @Override
    public List<SignupTrendDTO> getSignupTrend(UUID userId, String period) {

        log.info("회원가입 추세 조회: userId={}", userId);

        validateAdmin(userId);

        // 기간 설정
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = parsePeriod(period, endDate);

        // 회원가입 추세 데이터 조회
        List<User> users = userRepository.findByCreatedAtBetween(startDate, endDate);

        log.info("회원가입 추세 데이터 조회 성공: userId={}, period={}, count={}", userId, period, users.size());

        // 기간별 회원가입 수 집계
        Map<LocalDateTime, Long> signupCounts = users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.DAYS),
                        Collectors.counting()
                ));

        // 결과 DTO 생성
        List<SignupTrendDTO> signupTrends = new ArrayList<>();

        for (LocalDateTime date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            long count = signupCounts.getOrDefault(date.truncatedTo(java.time.temporal.ChronoUnit.DAYS), 0L);
            signupTrends.add(new SignupTrendDTO(date, count));
        }

        log.info("회원가입 추세 조회 성공: userId={}, period={}, trends={}", userId, period, signupTrends.size());

        return signupTrends;
    }

    private LocalDateTime parsePeriod(String period, LocalDateTime endDate) {
        return switch (period.toLowerCase()) {
            case DAILY_PERIOD -> endDate.minusDays(1);
            case WEEKLY_PERIOD -> endDate.minusWeeks(1);
            case MONTHLY_PERIOD -> endDate.minusMonths(1);
            default -> throw new CustomException("유효하지 않은 기간입니다.", HttpStatus.BAD_REQUEST);
        };
    }

    @Override
    public UserManagementResponseDTO getUserManagement(
            UUID userId, int page, int limit, String search, Boolean isActive,
            Boolean isBanned, String provider, UserRole role, String sortBy, String sortOrder) {

        // 1. Specification 생성
        Specification<User> spec = createUserSpecification(search, isActive, isBanned, provider, role);

        // 2. 정렬 생성
        Sort sort = createSort(sortBy, sortOrder);

        // 3. 페이지 요청 생성
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        // 4. 데이터 조회
        Page<User> userPage;
        if (spec != null) {
            userPage = userRepository.findAll(spec, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // 5. DTO 변환
        List<UserManagementDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertToUserManagementDTO)
                .collect(Collectors.toList());

        // 6. 페이지네이션 정보 생성
        PaginationDTO pagination = new PaginationDTO(
                page,
                userPage.getTotalPages(),
                (int) userPage.getTotalElements(),
                userPage.hasNext(),
                userPage.hasPrevious()
        );

        return new UserManagementResponseDTO(userDTOs, pagination);
    }

    private Specification<User> createUserSpecification(String search, Boolean isActive,
                                                        Boolean isBanned, String provider, UserRole role) {
        List<Specification<User>> specs = new ArrayList<>();

        // 검색 조건
        if (search != null && !search.trim().isEmpty()) {
            Specification<User> searchSpec = (root, query, criteriaBuilder) -> {
                String searchPattern = "%" + search.toLowerCase() + "%";
                return criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nickname")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchPattern)
                );
            };
            specs.add(searchSpec);
        }

        // 활성 상태 조건
        if (isActive != null) {
            Specification<User> activeSpec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("isActive"), isActive);
            specs.add(activeSpec);
        }

        // 정지 상태 조건
        if (isBanned != null) {
            Specification<User> bannedSpec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("isBanned"), isBanned);
            specs.add(bannedSpec);
        }

        // 제공자 조건
        if (provider != null && !provider.trim().isEmpty()) {
            Specification<User> providerSpec = (root, query, criteriaBuilder) -> {
                try {
                    OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
                    return criteriaBuilder.equal(root.get("provider"), oauthProvider);
                } catch (IllegalArgumentException e) {
                    return criteriaBuilder.conjunction(); // 항상 true
                }
            };
            specs.add(providerSpec);
        }

        // 역할 조건
        if (role != null) {
            Specification<User> roleSpec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("role"), role);
            specs.add(roleSpec);
        }

        if (specs.isEmpty()) {
            return null;
        }

        return Specification.allOf(specs);
    }

    private Sort createSort(String sortBy, String sortOrder) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
        }

        Sort.Direction direction = SORT_ORDER_ASC.equalsIgnoreCase(sortOrder) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        String sortField;
        switch (sortBy.toLowerCase()) {
            case SORT_FIELD_EMAIL:
                sortField = SORT_FIELD_EMAIL;
                break;
            case SORT_FIELD_CREATED_AT:
                sortField = DEFAULT_SORT_FIELD;
                break;
            case SORT_FIELD_UPDATED_AT:
                sortField = "updatedAt";
                break;
            case SORT_FIELD_NICKNAME:
                sortField = SORT_FIELD_NICKNAME;
                break;
            default:
                sortField = DEFAULT_SORT_FIELD;
                direction = Sort.Direction.DESC;
                break;
        }

        return Sort.by(direction, sortField);
    }

    private UserManagementDTO convertToUserManagementDTO(User user) {
        // UserStats 계산
        UserStats stats = calculateUserStats(user.getUserId());

        return new UserManagementDTO(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getNickname(),
                user.getProvider().name(),
                user.getRole().name(),
                user.getIsActive(),
                user.getIsBanned(),
                user.getBanEndDate(),
                user.getUserBudget(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                stats
        );
    }

    private UserStats calculateUserStats(UUID userId) {
        return getUserStats(userId);
    }

    @Override
    public UserManagementDetailDTO getUserManagementDetail(UUID adminId, UUID userId) {
        log.info("사용자 관리 상세 조회: adminId={}, userId={}", adminId, userId);

        validateAdmin(adminId);
        userService.validateUserExists(userId);

        User user = userService.findByUserId(userId);

        return convertToUserManagementDetailDTO(user);
    }

    private UserStats getUserStats(UUID userId) {
        long postCount = postRepository.countByUserId(userId);
        long commentCount = commentRepository.countByUserId(userId);
        long groupCount = groupMemberRepository.countByUser_UserId(userId);
        long reportCount = reportRepository.countByReportedUserId(userId);

        return new UserStats(postCount, commentCount, groupCount, reportCount);
    }

    private List<RecentReportDTO> getRecentReports(UUID userId) {
        return reportRepository.findByReportedUserId(userId).stream()
                .map(report -> new RecentReportDTO(
                        report.getReportId(),
                        report.getReportType().name(),
                        report.getReason(),
                        report.getStatus().name(),
                        report.getCreatedAt()
                )).collect(Collectors.toList());
    }

    private UserManagementDetailDTO convertToUserManagementDetailDTO(User user) {
        return new UserManagementDetailDTO(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getNickname(),
                user.getProvider().name(),
                user.getProviderId(),
                user.getRole().name(),
                user.getIsActive(),
                user.getIsBanned(),
                user.getBanEndDate(),
                user.getUserBudget(),
                user.getProfileImageFileId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getInactivedAt(),
                getUserStats(user.getUserId()),
                getRecentReports(user.getUserId())
        );
    }

    @Override
    public BanResponseDTO banUser(UUID adminId, UUID userId, BanRequestDTO request) {

        log.info("사용자 정지 요청: adminId={}, userId={}, request={}", adminId, userId, request);

        validateAdmin(adminId);
        userService.validateUserExists(userId);

        User user = userService.findByUserId(userId);

        if (user.getIsBanned()) {
            log.warn("이미 정지된 사용자 접근 시도: userId={}", userId);
            throw new CustomException("이미 정지된 사용자입니다.", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime banEndDate = request.banEndDate() != null ? request.banEndDate() : LocalDateTime.MAX;

        user.ban(banEndDate);
        userRepository.save(user);

        log.info("사용자 정지 성공: userId={}, banEndDate={}", userId, banEndDate);

        return new BanResponseDTO(
                user.getUserId(),
                user.getIsBanned(),
                user.getBanEndDate(),
                user.getUpdatedAt()
        );
    }

    @Override
    public UnbanResponseDTO unbanUser(UUID adminId, UUID userId, UnbanRequestDTO request) {

        log.info("사용자 정지 해제 요청: adminId={}, userId={}, request={}", adminId, userId, request);

        validateAdmin(adminId);

        User user = userService.findByUserId(userId);

        if (!user.getIsBanned()) {
            log.warn("정지되지 않은 사용자 접근 시도: userId={}", userId);
            throw new CustomException("정지되지 않은 사용자입니다.", HttpStatus.BAD_REQUEST);
        }

        user.unban();
        userRepository.save(user);

        log.info("사용자 정지 해제 성공: userId={}", userId);

        return new UnbanResponseDTO(
                user.getUserId(),
                user.getIsBanned(),
                user.getBanEndDate() != null ? user.getBanEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                user.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Override
    public ReportManagementResponseDTO getReportManagement(UUID userId, int page, int limit, String status, String reportType, UUID reporterId, UUID reportedUserId, String dateFrom, String dateTo) {

        log.info("신고 관리 조회: userId={}, page={}, limit={}, status={}, reportType={}, reporterId={}, reportedUserId={}, dateFrom={}, dateTo={}",
                userId, page, limit, status, reportType, reporterId, reportedUserId, dateFrom, dateTo);

        validateAdmin(userId);

        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 신고 조건 생성
        Specification<Report> spec = createReportSpecification(status, reportType, reporterId, reportedUserId, dateFrom, dateTo);

        // 데이터 조회
        Page<Report> reportPage = reportRepository.findAll(spec, pageable);

        // DTO 변환
        List<ReportManagementDTO> reportDTOs = reportPage.getContent().stream()
                .map(this::convertToReportManagementDTO)
                .collect(Collectors.toList());

        // 페이지네이션 정보 생성
        PaginationDTO pagination = new PaginationDTO(
                page,
                reportPage.getTotalPages(),
                (int) reportPage.getTotalElements(),
                reportPage.hasNext(),
                reportPage.hasPrevious()
        );

        return new ReportManagementResponseDTO(reportDTOs, pagination);
    }

    private Specification<Report> createReportSpecification(String status, String reportType, UUID reporterId, UUID reportedUserId, String dateFrom, String dateTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addStringEqualPredicate(predicates, criteriaBuilder, root, "status", status);
            addStringEqualPredicate(predicates, criteriaBuilder, root, "reportType", reportType);
            
            if (reporterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("reporter").get("userId"), reporterId));
            }

            if (reportedUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("reportedUser").get("userId"), reportedUserId));
            }

            addDateRangePredicates(predicates, criteriaBuilder, root, dateFrom, dateTo);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addStringEqualPredicate(List<Predicate> predicates,
                                         jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                         jakarta.persistence.criteria.Root<?> root,
                                         String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get(fieldName), value));
        }
    }

    private void addDateRangePredicates(List<Predicate> predicates,
                                        jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                        jakarta.persistence.criteria.Root<?> root,
                                        String dateFrom, String dateTo) {
        if (dateFrom != null && !dateFrom.isEmpty()) {
            LocalDateTime fromDate = LocalDateTime.parse(dateFrom);
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }

        if (dateTo != null && !dateTo.isEmpty()) {
            LocalDateTime toDate = LocalDateTime.parse(dateTo);
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
    }

    private ReportManagementDTO convertToReportManagementDTO(Report report) {

        return new ReportManagementDTO(
                report.getReportId(),
                new AdminPageUserDTO(
                        report.getReporter().getUserId(),
                        report.getReporter().getEmail(),
                        report.getReporter().getNickname()
                ),
                new AdminPageUserDTO(
                        report.getReportedUser().getUserId(),
                        report.getReportedUser().getEmail(),
                        report.getReportedUser().getNickname()
                ),
                report.getReportType().name(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus().name(),
                report.getProcessedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    @Override
    public ReportManagementDetailDTO getReportManagementDetail(UUID userId, Long reportId) {

        log.info("신고 관리 상세 조회: userId={}, reportId={}", userId, reportId);

        validateAdmin(userId);
        validateReportExists(reportId);

        Report report = reportRepository.findByReportId(reportId);

        return new ReportManagementDetailDTO(
                report.getReportId(),
                new UserManagementDTO(
                        report.getReporter().getUserId(),
                        report.getReporter().getEmail(),
                        report.getReporter().getUsername(),
                        report.getReporter().getNickname(),
                        report.getReporter().getProvider().name(),
                        report.getReporter().getRole().name(),
                        report.getReporter().getIsActive(),
                        report.getReporter().getIsBanned(),
                        report.getReporter().getBanEndDate(),
                        report.getReporter().getUserBudget(),
                        report.getReporter().getCreatedAt(),
                        report.getReporter().getUpdatedAt(),
                        getUserStats(report.getReporter().getUserId())
                ),
                new UserManagementDTO(
                        report.getReportedUser().getUserId(),
                        report.getReportedUser().getEmail(),
                        report.getReportedUser().getUsername(),
                        report.getReportedUser().getNickname(),
                        report.getReportedUser().getProvider().name(),
                        report.getReportedUser().getRole().name(),
                        report.getReportedUser().getIsActive(),
                        report.getReportedUser().getIsBanned(),
                        report.getReportedUser().getBanEndDate(),
                        report.getReportedUser().getUserBudget(),
                        report.getReportedUser().getCreatedAt(),
                        report.getReportedUser().getUpdatedAt(),
                        getUserStats(report.getReportedUser().getUserId())
                ),
                report.getReportType().name(),
                String.valueOf(report.getTargetId()),
                null, // TargetContentDTO는 별도로 구현 필요
                report.getReason(),
                report.getStatus().name(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private void validateReportExists(Long reportId) {
        if (!reportRepository.existsByReportId(reportId)) {
            log.error("존재하지 않는 신고 ID: {}", reportId);
            throw new CustomException("존재하지 않는 신고 ID입니다.", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ReportProcessResponseDTO processReport(UUID userId, Long reportId, ReportProcessRequestDTO request) {

        log.info("신고 처리 요청: userId={}, reportId={}, request={}", userId, reportId, request);

        validateAdmin(userId);
        validateReportExists(reportId);

        Report report = reportRepository.findByReportId(reportId);

        // 신고 상태, 신고 처리 시간 업데이트
        report.updateStatus(request.status());

        // 사용자 정지 여부 확인
        if (request.banUser()) {
            LocalDateTime banEndDate = request.banEndDate() != null ? LocalDateTime.parse(request.banEndDate()) : LocalDateTime.MAX;
            report.getReportedUser().ban(banEndDate);
            userRepository.save(report.getReportedUser());
        }

        // 신고 저장
        reportRepository.save(report);

        log.info("신고 처리 성공: reportId={}, status={}", reportId, request.status());

        return new ReportProcessResponseDTO(
                report.getReportId(),
                report.getStatus().name(),
                userId.toString(), // 관리자 ID는 UUID를 String으로 변환
                report.getProcessedAt() != null ? report.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                new ReportActionDTO[]{} // ReportActionDTO는 별도로 구현 필요
        );

    }

    @Override
    public AiJudgmentResponseDTO getAiJudgment(UUID userId, int page, int limit, String search, String rating, String category, String requestType, String dateFrom, String dateTo) {

        log.info("AI 판단 조회: userId={}, page={}, limit={}, search={}, rating={}, category={}, requestType={}, dateFrom={}, dateTo={}",
                userId, page, limit, search, rating, category, requestType, dateFrom, dateTo);

        validateAdmin(userId);

        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        // AI 판단 조건 생성
        Specification<AiJudgment> spec = createAiJudgmentSpecification(search, rating, category, requestType, dateFrom, dateTo);

        // 데이터 조회
        Page<AiJudgment> aiJudgmentPage = AiJudgmentRepository.findAll(spec, pageable);

        // DTO 변환
        List<AiJudgmentDTO> aiJudgmentDTOs = aiJudgmentPage.getContent().stream()
                .map(this::convertToAiJudgmentResponseDTO)
                .collect(Collectors.toList());

        // 페이지네이션 정보 생성
        PaginationDTO pagination = new PaginationDTO(
                page,
                aiJudgmentPage.getTotalPages(),
                (int) aiJudgmentPage.getTotalElements(),
                aiJudgmentPage.hasNext(),
                aiJudgmentPage.hasPrevious()
        );

        return new AiJudgmentResponseDTO(aiJudgmentDTOs, pagination);
    }

    private Specification<AiJudgment> createAiJudgmentSpecification(String search, String rating, String category, String requestType, String dateFrom, String dateTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addUserSearchPredicate(predicates, criteriaBuilder, root, search);
            addStringEqualPredicate(predicates, criteriaBuilder, root, "rating", rating);
            addStringEqualPredicate(predicates, criteriaBuilder, root, "category", category);
            addStringEqualPredicate(predicates, criteriaBuilder, root, "requestType", requestType);
            addDateRangePredicates(predicates, criteriaBuilder, root, dateFrom, dateTo);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addUserSearchPredicate(List<Predicate> predicates,
                                        jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                        jakarta.persistence.criteria.Root<?> root,
                                        String search) {
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("email")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("nickname")), searchPattern)
            ));
        }
    }

    private AiJudgmentDTO convertToAiJudgmentResponseDTO(AiJudgment aiJudgment) {
        return new AiJudgmentDTO(
                aiJudgment.getJudgmentId(),
                new AdminPageUserDTO(
                        aiJudgment.getUser().getUserId(),
                        aiJudgment.getUser().getEmail(),
                        aiJudgment.getUser().getNickname()
                ),
                aiJudgment.getGroup() != null ? new AdminPageGroupDTO(
                        aiJudgment.getGroup().getGroupId(),
                        aiJudgment.getGroup().getGroupName()
                ) : null,
                aiJudgment.getSituation(),
                aiJudgment.getJudgmentResult(),
                aiJudgment.getUp() != null ? (aiJudgment.getUp() ? UP_RATING : DOWN_RATING) : null,
                aiJudgment.getDownReason(),
                aiJudgment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Override
    public AiJudgmentDetailDTO getAiJudgmentDetail(UUID userId, UUID judgmentId) {

        log.info("AI 판단 상세 조회: userId={}, judgmentId={}", userId, judgmentId);

        validateAdmin(userId);
        validateAiJudgmentExists(judgmentId);

        AiJudgment aiJudgment = AiJudgmentRepository.findByJudgmentId(judgmentId);

        return new AiJudgmentDetailDTO(
                aiJudgment.getJudgmentId(),
                new AdminPageUserDTO(
                        aiJudgment.getUser().getUserId(),
                        aiJudgment.getUser().getEmail(),
                        aiJudgment.getUser().getNickname()
                ),
                aiJudgment.getGroup() != null ? new AdminPageGroupDTO(
                        aiJudgment.getGroup().getGroupId(),
                        aiJudgment.getGroup().getGroupName()
                ) : null,
                aiJudgment.getSituation(),
                aiJudgment.getJudgmentResult(),
                aiJudgment.getUp() != null ? (aiJudgment.getUp() ? UP_RATING : DOWN_RATING) : null,
                aiJudgment.getDownReason(),
                aiJudgment.getCreatedAt(),
                aiJudgment.getUpdatedAt()
        );
    }

    private void validateAiJudgmentExists(UUID judgmentId) {
        if (!AiJudgmentRepository.existsByJudgmentId(judgmentId)) {
            log.error("존재하지 않는 AI 판단 ID: {}", judgmentId);
            throw new CustomException("존재하지 않는 AI 판단 ID입니다.", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public AiJudgmentStatsDTO getAiJudgmentStats(UUID userId, String period) {

        log.info("AI 판단 통계 조회: userId={}, period={}", userId, period);

        validateAdmin(userId);

        // 기간 설정
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        startDate = parsePeriod(period, endDate);

        // AI 판단 통계 조회
        List<AiJudgment> judgments = AiJudgmentRepository.findByCreatedAtBetween(startDate, endDate);

        if (judgments.isEmpty()) {
            return new AiJudgmentStatsDTO(0.0, 0, Collections.emptyList());
        }

        // 만족도 계산
        long totalCount = judgments.size();
        long satisfiedCount = judgments.stream()
                .filter(j -> Boolean.TRUE.equals(j.getUp()))
                .count();
        double satisfactionRate = (double) satisfiedCount / totalCount * 100;

        // 불만족 카운트
        int dissatisfactionCount = (int) judgments.stream()
                .filter(j -> Boolean.TRUE.equals(j.getDown()))
                .count();

        // 추세 계산
        List<AiJudgmentTrendsDTO> trends = judgments.stream()
                .filter(j -> j.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        j -> j.getCreatedAt().toLocalDate().atStartOfDay()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    LocalDateTime date = entry.getKey();
                    List<AiJudgment> dailyJudgments = entry.getValue();

                    long total = dailyJudgments.size();
                    long satisfied = dailyJudgments.stream()
                            .filter(j -> Boolean.TRUE.equals(j.getUp()))
                            .count();

                    double dailySatisfactionRate = total > 0 ? (double) satisfied / total * 100 : 0.0;

                    return new AiJudgmentTrendsDTO(date, dailySatisfactionRate);
                })
                .sorted(Comparator.comparing(AiJudgmentTrendsDTO::date))
                .collect(Collectors.toList());

        log.info("AI 판단 통계 조회 성공: userId={}, satisfactionRate={}, dissatisfactionCount={}",
                userId, satisfactionRate, dissatisfactionCount);

        return new AiJudgmentStatsDTO(satisfactionRate, dissatisfactionCount, trends);
    }

}
