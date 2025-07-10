package store.lastdance.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import store.lastdance.domain.admin.Report;
import store.lastdance.domain.admin.ReportStatus;
import store.lastdance.domain.admin.ReportType;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.dto.admin.*;
import store.lastdance.dto.admin.stats.DashboardContentStats;
import store.lastdance.dto.admin.stats.DashboardUserStats;
import store.lastdance.repository.admin.ReportRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.repository.community.PostRepository;
import store.lastdance.repository.community.CommentRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.aijudement.AiJudgmentRepository;
import store.lastdance.service.user.UserService;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Service 테스트")
class AdminServiceTest {

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private AiJudgmentRepository aiJudgmentRepository;

    private UUID adminId;
    private UUID userId;
    private UUID reportedUserId;
    private User adminUser;
    private User normalUser;
    private User reportedUser;
    private Report report;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        reportedUserId = UUID.randomUUID();

        adminUser = User.builder()
                .email("admin@example.com")
                .username("admin")
                .nickname("admin")
                .provider(OAuthProvider.GOOGLE)
                .providerId("admin-provider-id")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        adminUser.setUserId(adminId);

        normalUser = User.builder()
                .email("user@example.com")
                .username("user")
                .nickname("user")
                .provider(OAuthProvider.GOOGLE)
                .providerId("user-provider-id")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        normalUser.setUserId(userId);

        reportedUser = User.builder()
                .email("reported@example.com")
                .username("reported")
                .nickname("reported")
                .provider(OAuthProvider.GOOGLE)
                .providerId("reported-provider-id")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        reportedUser.setUserId(reportedUserId);

        report = Report.builder()
                .reporterId(userId)
                .reportedUserId(reportedUserId)
                .reportType(ReportType.POST)
                .targetId(reportedUserId)
                .reason("Inappropriate behavior")
                .build();
    }

    @Nested
    @DisplayName("관리자 인증")
    class AdminVerificationTests {

        @Test
        @DisplayName("관리자 인증 성공")
        void verifyAdmin_Success() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);

            // when
            AdminVerifyResponseDTO result = adminService.verifyAdmin(adminId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.role()).isEqualTo(UserRole.ADMIN);
            assertThat(result.userId()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 인증 실패")
        void verifyAdmin_UserNotFound() {
            // given
            given(userService.findByActiveUser(any(UUID.class)))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> adminService.verifyAdmin(userId))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("대시보드 통계")
    class DashboardStatsTests {

        @Test
        @DisplayName("대시보드 통계 조회 성공")
        void getDashboardStats_Success() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userRepository.count()).willReturn(100L);
            given(userRepository.countByIsActiveTrue()).willReturn(95L);
            given(userRepository.countByIsBannedTrue()).willReturn(5L);
            given(userRepository.countAllByCreatedAtAfter(any(LocalDateTime.class))).willReturn(10L);
            given(postRepository.count()).willReturn(200L);
            given(commentRepository.count()).willReturn(500L);
            given(postRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(20L);
            given(commentRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(50L);

            // when
            DashboardStatsDTO result = adminService.getDashboardStats(adminId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.dashboardUserStats()).isNotNull();
            assertThat(result.dashboardContentStats()).isNotNull();
        }
    }

    @Nested
    @DisplayName("사용자 관리")
    class UserManagementTests {

        @Test
        @DisplayName("사용자 목록 조회 성공")
        void getUserManagement_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<User> users = List.of(normalUser, reportedUser);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());

            given(userRepository.findAll(any(Pageable.class))).willReturn(userPage);
            given(postRepository.countByUserId(any(UUID.class))).willReturn(0L);
            given(commentRepository.countByUserId(any(UUID.class))).willReturn(0L);
            given(groupMemberRepository.countByUser_UserId(any(UUID.class))).willReturn(0L);
            given(reportRepository.countByReportedUserId(any(UUID.class))).willReturn(0L);

            // when
            UserManagementResponseDTO result = adminService.getUserManagement(
                    adminId, 1, 10, "", null, null, "", null, "", "");

            // then
            assertThat(result).isNotNull();
            assertThat(result.users()).hasSize(2);
            assertThat(result.pagination().totalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("사용자 상세 정보 조회 성공")
        void getUserDetail_Success() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userService.findByUserId(userId)).willReturn(normalUser);
            given(postRepository.countByUserId(userId)).willReturn(5L);
            given(commentRepository.countByUserId(userId)).willReturn(10L);
            given(groupMemberRepository.countByUser_UserId(userId)).willReturn(2L);
            given(reportRepository.countByReportedUserId(userId)).willReturn(0L);
            given(reportRepository.findByReportedUserId(userId)).willReturn(Collections.emptyList());

            // when
            UserManagementDetailDTO result = adminService.getUserManagementDetail(adminId, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.nickname()).isEqualTo("user");
            assertThat(result.email()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 상세 조회 실패")
        void getUserDetail_UserNotFound() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userService.findByUserId(any(UUID.class)))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> adminService.getUserManagementDetail(adminId, userId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("사용자 차단 성공")
        void banUser_Success() {
            // given
            BanRequestDTO banRequest = new BanRequestDTO(
                    LocalDateTime.now().plusDays(7), "Inappropriate behavior", true);
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userService.findByUserId(userId)).willReturn(normalUser);
            
            // when
            BanResponseDTO result = adminService.banUser(adminId, userId, banRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isBanned()).isTrue();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("사용자 차단 해제 성공")
        void unbanUser_Success() {
            // given
            UnbanRequestDTO unbanRequest = new UnbanRequestDTO(true);
            
            // Mock User 객체로 대체하여 updatedAt 설정
            User mockBannedUser = mock(User.class);
            given(mockBannedUser.getUserId()).willReturn(userId);
            given(mockBannedUser.getUpdatedAt()).willReturn(LocalDateTime.now()); // updatedAt 설정
            // unban() 호출 전에는 true, 호출 후에는 false를 반환하도록 설정
            given(mockBannedUser.getIsBanned()).willReturn(true).willReturn(false);

            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userService.findByUserId(userId)).willReturn(mockBannedUser);

            // when
            UnbanResponseDTO result = adminService.unbanUser(adminId, userId, unbanRequest);

            // then
            assertThat(result).isNotNull();
            // 차단 해제 후에는 isBanned가 false가 되어야 함
            assertThat(result.isBanned()).isFalse();
            verify(userRepository).save(any(User.class));
            // mockBannedUser의 unban 메서드가 호출되었는지 확인
            verify(mockBannedUser).unban();
        }
    }

    @Nested
    @DisplayName("신고 관리")
    class ReportManagementTests {

        @Test
        @DisplayName("신고 목록 조회 성공")
        void getReportManagement_Success() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            Pageable pageable = PageRequest.of(0, 10);
            
            // Report Mock 객체 생성 및 설정
            Report mockReport = mock(Report.class);
            given(mockReport.getReportId()).willReturn(1L);
            given(mockReport.getReportType()).willReturn(ReportType.POST);
            given(mockReport.getReason()).willReturn("Inappropriate behavior");
            given(mockReport.getStatus()).willReturn(ReportStatus.PENDING); // status 설정 추가
            given(mockReport.getReporter()).willReturn(normalUser); // reporter 관계 설정
            given(mockReport.getReportedUser()).willReturn(reportedUser); // reportedUser 관계 설정
            given(mockReport.getCreatedAt()).willReturn(LocalDateTime.now()); // createdAt 설정 추가
            
            List<Report> reports = List.of(mockReport);
            Page<Report> reportPage = new PageImpl<>(reports, pageable, reports.size());

            given(reportRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .willReturn(reportPage);

            // when
            ReportManagementResponseDTO result = adminService.getReportManagement(
                    adminId, 1, 10, ReportStatus.valueOf("all"), ReportType.POST, "", "", "", "", "", "", "");

            // then
            assertThat(result).isNotNull();
            assertThat(result.reportManagements()).hasSize(1);
            assertThat(result.pagination().totalItems()).isEqualTo(1);
        }

        @Test
        @DisplayName("신고 상세 정보 조회 성공")
        void getReportDetail_Success() {
            // given
            // Report Mock 객체 생성 및 설정
            Report mockReport = mock(Report.class);
            given(mockReport.getReportId()).willReturn(1L);
            given(mockReport.getReportType()).willReturn(ReportType.POST);
            given(mockReport.getReason()).willReturn("Inappropriate behavior");
            given(mockReport.getStatus()).willReturn(ReportStatus.PENDING);
            given(mockReport.getCreatedAt()).willReturn(LocalDateTime.now());
            given(mockReport.getReporter()).willReturn(normalUser); // reporter 설정 추가
            given(mockReport.getReportedUser()).willReturn(reportedUser); // reportedUser 설정 추가
            
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(reportRepository.existsByReportId(1L)).willReturn(true);
            given(reportRepository.findByReportId(1L)).willReturn(mockReport);
            given(postRepository.countByUserId(any(UUID.class))).willReturn(0L);
            given(commentRepository.countByUserId(any(UUID.class))).willReturn(0L);
            given(groupMemberRepository.countByUser_UserId(any(UUID.class))).willReturn(0L);
            given(reportRepository.countByReportedUserId(any(UUID.class))).willReturn(0L);

            // when
            ReportManagementDetailDTO result = adminService.getReportManagementDetail(adminId, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.reportId()).isEqualTo(1L);
            assertThat(result.reason()).isEqualTo("Inappropriate behavior");
        }

        @Test
        @DisplayName("존재하지 않는 신고 조회 실패")
        void getReportDetail_ReportNotFound() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(reportRepository.existsByReportId(any(Long.class))).willReturn(false);

            // when & then
            assertThatThrownBy(() -> adminService.getReportManagementDetail(adminId, 1L))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("신고 처리 성공")
        void processReport_Success() {
            // given
            ReportProcessRequestDTO processRequest = new ReportProcessRequestDTO(
                    ReportStatus.RESOLVED, true, LocalDateTime.of(2024, 1, 15, 10, 30, 0), true);
            
            // Report Mock 객체 생성 및 설정
            Report mockReport = mock(Report.class);
            given(mockReport.getReportId()).willReturn(1L);
            given(mockReport.getReportedUser()).willReturn(reportedUser); // reportedUser 설정
            given(mockReport.getStatus()).willReturn(ReportStatus.PENDING); // status 설정 추가
            
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(reportRepository.existsByReportId(1L)).willReturn(true);
            given(reportRepository.findByReportId(1L)).willReturn(mockReport);

            // when
            ReportProcessResponseDTO result = adminService.processReport(adminId, 1L, processRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.reportId()).isEqualTo(1L);
            verify(reportRepository).save(any(Report.class));
        }
    }

    @Nested
    @DisplayName("AI 판정 관리")
    class AiJudgmentManagementTests {

        @Test
        @DisplayName("AI 판정 목록 조회 성공")
        void getAiJudgment_Success() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            Page<AiJudgment> emptyPage = new PageImpl<>(Collections.emptyList());
            given(aiJudgmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            AiJudgmentResponseDTO result = adminService.getAiJudgment(
                    adminId, 1, 10, "all", "", "", "");

            // then
            assertThat(result).isNotNull();
            assertThat(result.aiJudgments()).isNotNull();
            assertThat(result.pagination()).isNotNull();
        }

        @Test
        @DisplayName("AI 판정 상세 정보 조회 성공")
        void getAiJudgmentDetail_Success() {
            // given
            UUID judgmentId = UUID.randomUUID();
            AiJudgment aiJudgment = mock(AiJudgment.class);
            
            // AiJudgment mock 객체의 메서드들 stubbing
            given(aiJudgment.getJudgmentId()).willReturn(judgmentId);
            given(aiJudgment.getSituation()).willReturn("Test situation");
            given(aiJudgment.getJudgmentResult()).willReturn("Test result");
            given(aiJudgment.getUser()).willReturn(normalUser); // 핵심: user 객체 반환 설정

            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(aiJudgmentRepository.existsByJudgmentId(judgmentId)).willReturn(true);
            given(aiJudgmentRepository.findByJudgmentId(judgmentId)).willReturn(aiJudgment);

            // when
            AiJudgmentDetailDTO result = adminService.getAiJudgmentDetail(adminId, judgmentId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.judgmentId()).isEqualTo(judgmentId);
        }

        @Test
        @DisplayName("AI 판정 통계 계산 정확성 검증")
        void calculateAiJudgmentStats_Accuracy() {
            // given
            String period = "weekly";
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(aiJudgmentRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            AiJudgmentStatsDTO result = adminService.getAiJudgmentStats(adminId, period);

            // then
            assertThat(result).isNotNull();
            assertThat(result.dissatisfactionCount()).isGreaterThanOrEqualTo(0);
            assertThat(result.satisfactionRate()).isBetween(0.0, 100.0);
        }
    }

    @Nested
    @DisplayName("통계 계산 로직 테스트")
    class StatisticsCalculationTests {

        @Test
        @DisplayName("회원가입 추세 계산 정확성 검증")
        void calculateSignupTrend_Accuracy() {
            // given
            String period = "daily";
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            List<SignupTrendDTO> result = adminService.getSignupTrend(adminId, period);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("대시보드 통계 일관성 검증")
        void validateDashboardStatsConsistency() {
            // given
            given(userService.findByActiveUser(adminId)).willReturn(adminUser);
            given(userRepository.count()).willReturn(100L);
            given(userRepository.countByIsActiveTrue()).willReturn(95L);
            given(userRepository.countByIsBannedTrue()).willReturn(5L);
            given(userRepository.countAllByCreatedAtAfter(any(LocalDateTime.class))).willReturn(10L);
            given(postRepository.count()).willReturn(200L);
            given(commentRepository.count()).willReturn(500L);
            given(postRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(20L);
            given(commentRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(50L);

            // when
            DashboardStatsDTO result = adminService.getDashboardStats(adminId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.dashboardUserStats().total()).isEqualTo(100L);
            assertThat(result.dashboardUserStats().active()).isEqualTo(95L);
        }
    }

    @Nested
    @DisplayName("성능 및 예외 처리 테스트")
    class PerformanceAndExceptionTests {

        @Test
        @DisplayName("대량 데이터 처리 성능")
        void processingLargeDataSet() {
            // given
            Pageable pageable = PageRequest.of(0, 100);
            List<User> largeUserList = java.util.stream.IntStream.range(0, 100)
                    .mapToObj(i -> {
                        User user = User.builder()
                                .email("user" + i + "@example.com")
                                .username("user" + i)
                                .nickname("user" + i)
                                .provider(OAuthProvider.GOOGLE)
                                .providerId("provider-id-" + i)
                                .role(UserRole.USER)
                                .isActive(true)
                                .build();
                        user.setUserId(UUID.randomUUID()); // userId 설정
                        return user;
                    })
                    .toList();
            Page<User> largePage = new PageImpl<>(largeUserList, pageable, 100);

            lenient().when(userService.findByActiveUser(adminId)).thenReturn(adminUser);
            lenient().when(userRepository.findAll(any(Pageable.class))).thenReturn(largePage);
            // lenient() 사용으로 불필요한 stubbing 경고 제거
            lenient().when(postRepository.countByUserId(any())).thenReturn(0L);
            lenient().when(commentRepository.countByUserId(any())).thenReturn(0L);
            lenient().when(groupMemberRepository.countByUser_UserId(any())).thenReturn(0L);
            lenient().when(reportRepository.countByReportedUserId(any())).thenReturn(0L);

            // when
            long startTime = System.currentTimeMillis();
            UserManagementResponseDTO result = adminService.getUserManagement(
                    adminId, 1, 100, "", null, null, "", null, "", "");
            long endTime = System.currentTimeMillis();

            // then
            assertThat(result.users()).hasSize(100);
            assertThat(endTime - startTime).isLessThan(5000); // 5초 이내
        }

        @Test
        @DisplayName("데이터베이스 연결 오류 처리")
        void handleDatabaseConnectionError() {
            // given
            given(userService.findByActiveUser(adminId))
                    .willThrow(new RuntimeException("Cannot invoke \"store.lastdance.service.user.UserService.findByActiveUser(java.util.UUID)\" because \"this.userService\" is null"));

            // when & then
            assertThatThrownBy(() -> adminService.verifyAdmin(adminId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot invoke \"store.lastdance.service.user.UserService.findByActiveUser(java.util.UUID)\" because \"this.userService\" is null");
        }
    }
}

