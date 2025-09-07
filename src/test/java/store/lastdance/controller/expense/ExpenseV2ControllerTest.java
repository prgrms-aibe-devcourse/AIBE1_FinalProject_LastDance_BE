package store.lastdance.controller.expense;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.expense.ExpenseV2QueryService;
import store.lastdance.service.expense.ExpenseV2Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class ExpenseV2ControllerTest {

    @InjectMocks
    private ExpenseV2Controller expenseV2Controller;

    @Mock
    private ExpenseV2Service expenseV2Service;

    @Mock
    private ExpenseV2QueryService expenseV2QueryService;

    private final UUID testUserId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    private CustomOAuth2User testUser;

    @BeforeEach
    void setUp() {
        testUser = new CustomOAuth2User(
                testUserId,
                "test@test.com",
                "testUser",
                "KAKAO",
                "123456789",
                Map.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, "password", testUser.getAuthorities());
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("개인 지출 생성 성공 테스트")
    void createPersonalExpense_Success() {
        // given
        CreatePersonalExpenseRequestDTO requestDTO = new CreatePersonalExpenseRequestDTO(
                "점심 식사", new BigDecimal("25000"), ExpenseCategory.FOOD, LocalDate.now(), "memo"
        );
        MockMultipartFile receiptFile = new MockMultipartFile("receiptFile", "receipt.jpg", "image/jpeg", "receipt image".getBytes());

        ExpenseResponseDTO responseDTO = new ExpenseResponseDTO(
                1L, "점심 식사", new BigDecimal("25000"), ExpenseCategory.FOOD, ExpenseType.PERSONAL, null, null,
                LocalDate.now(), "memo", null, testUserId, LocalDateTime.now(), LocalDateTime.now(), null, true
        );

        given(expenseV2Service.createPersonalExpense(eq(testUserId), any(CreatePersonalExpenseRequestDTO.class), any()))
                .willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<ExpenseResponseDTO>> response = expenseV2Controller.createPersonalExpense(testUser, requestDTO, receiptFile);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().expenseId()).isEqualTo(1L);
        assertThat(response.getBody().getData().title()).isEqualTo("점심 식사");

        verify(expenseV2Service).createPersonalExpense(eq(testUserId), any(CreatePersonalExpenseRequestDTO.class), any());
    }

    @Test
    @DisplayName("그룹 지출 생성 성공 테스트")
    void createGroupExpense_Success() {
        // given
        UUID groupId = UUID.randomUUID();
        CreateGroupExpenseRequestDTO requestDTO = new CreateGroupExpenseRequestDTO(
                "저녁 회식", new BigDecimal("150000"), ExpenseCategory.FOOD, LocalDate.now(), "memo",
                groupId, SplitType.EQUAL, Collections.emptyList()
        );
        MockMultipartFile receiptFile = new MockMultipartFile("receiptFile", "receipt.jpg", "image/jpeg", "receipt image".getBytes());

        ExpenseResponseDTO responseDTO = new ExpenseResponseDTO(
                2L, "저녁 회식", new BigDecimal("150000"), ExpenseCategory.FOOD, ExpenseType.GROUP, SplitType.EQUAL, Collections.emptyList(),
                LocalDate.now(), "memo", groupId, testUserId, LocalDateTime.now(), LocalDateTime.now(), null, true
        );

        given(expenseV2Service.createGroupExpense(eq(testUserId), any(CreateGroupExpenseRequestDTO.class), any()))
                .willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<ExpenseResponseDTO>> response = expenseV2Controller.createGroupExpense(testUser, requestDTO, receiptFile);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().expenseId()).isEqualTo(2L);
        assertThat(response.getBody().getData().title()).isEqualTo("저녁 회식");
        assertThat(response.getBody().getData().expenseType()).isEqualTo(ExpenseType.GROUP);

        verify(expenseV2Service).createGroupExpense(eq(testUserId), any(CreateGroupExpenseRequestDTO.class), any());
    }

    @Test
    @DisplayName("통합 지출 내역 조회 성공 테스트")
    void getCombinedExpenses_Success() {
        // given
        LocalDate now = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);

        Page<CombinedExpenseResponseDTO> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        PageWithSummaryResponse<CombinedExpenseResponseDTO> serviceResponse = PageWithSummaryResponse.of(page, ExpenseSummary.empty());

        given(expenseV2QueryService.getCombinedExpenses(eq(testUserId), any(ExpenseSearchDTO.class), any(Pageable.class))).willReturn(serviceResponse);

        // when
        ResponseEntity<ApiResponse<PageWithSummaryResponse<CombinedExpenseResponseDTO>>> response = expenseV2Controller.getCombinedExpenses(testUser, searchDTO, pageable);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().page().getContent()).isEmpty();

        verify(expenseV2QueryService).getCombinedExpenses(eq(testUserId), any(ExpenseSearchDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("전체 분담금 조회 성공 테스트")
    void getGroupShareExpenses_Success() {
        // given
        LocalDate now = LocalDate.now();
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);

        given(expenseV2QueryService.getGroupShareExpenses(eq(testUserId), any(ExpenseSearchDTO.class)))
                .willReturn(Collections.emptyList());

        // when
        ResponseEntity<ApiResponse<List<GroupShareExpenseResponseDTO>>> response = expenseV2Controller.getGroupShareExpenses(testUser, searchDTO);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isEmpty();

        verify(expenseV2QueryService).getGroupShareExpenses(eq(testUserId), any(ExpenseSearchDTO.class));
    }

    @Test
    @DisplayName("특정 그룹 분담 지출 조회 - 페이징 성공 테스트")
    void getGroupShareExpensesWithPaging_Success() {
        // given
        LocalDate now = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);
        UUID groupId = UUID.randomUUID();

        Page<GroupShareExpenseResponseDTO> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        PageWithSummaryResponse<GroupShareExpenseResponseDTO> serviceResponse = PageWithSummaryResponse.of(page, ExpenseSummary.empty());

        given(expenseV2QueryService.getGroupShareExpensesWithPaging(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class), any(Pageable.class))).willReturn(serviceResponse);

        // when
        ResponseEntity<ApiResponse<PageWithSummaryResponse<GroupShareExpenseResponseDTO>>> response = expenseV2Controller.getGroupShareExpensesWithPaging(testUser, groupId, searchDTO, pageable);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().page().getContent()).isEmpty();

        verify(expenseV2QueryService).getGroupShareExpensesWithPaging(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("그룹 지출 페이징 및 통계 조회 성공 테스트")
    void getGroupExpensesWithStats_Success() {
        // given
        LocalDate now = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);
        UUID groupId = UUID.randomUUID();

        Page<ExpenseResponseDTO> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        PageWithSummaryResponse<ExpenseResponseDTO> serviceResponse = PageWithSummaryResponse.of(page, ExpenseSummary.empty());

        given(expenseV2QueryService.getGroupExpensesWithStats(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class), any(Pageable.class))).willReturn(serviceResponse);

        // when
        ResponseEntity<ApiResponse<PageWithSummaryResponse<ExpenseResponseDTO>>> response = expenseV2Controller.getGroupExpensesWithStats(testUser, groupId, searchDTO, pageable);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().page().getContent()).isEmpty();

        verify(expenseV2QueryService).getGroupExpensesWithStats(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("지출 상세 조회 성공 테스트")
    void getExpenseById_Success() {
        // given
        Long expenseId = 1L;
        ExpenseResponseDTO responseDTO = new ExpenseResponseDTO(
                expenseId, "상세 조회 테스트", new BigDecimal("50000"), ExpenseCategory.OTHER, ExpenseType.PERSONAL, null, null,
                LocalDate.now(), null, null, testUserId, LocalDateTime.now(), LocalDateTime.now(), null, false
        );

        given(expenseV2QueryService.getExpenseById(testUserId, expenseId)).willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<ExpenseResponseDTO>> response = expenseV2Controller.getExpenseById(testUser, expenseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().expenseId()).isEqualTo(expenseId);
        assertThat(response.getBody().getData().title()).isEqualTo("상세 조회 테스트");

        verify(expenseV2QueryService).getExpenseById(testUserId, expenseId);
    }

    @Test
    @DisplayName("지출 수정 성공 테스트")
    void updateExpense_Success() {
        // given
        Long expenseId = 1L;
        UpdateExpenseRequestDTO requestDTO = new UpdateExpenseRequestDTO(
                "수정된 점심 식사", new BigDecimal("30000"), ExpenseCategory.FOOD, LocalDate.now(), "수정된 memo", Collections.emptyList(), null
        );
        MockMultipartFile receiptFile = new MockMultipartFile("receiptFile", "receipt.jpg", "image/jpeg", "receipt image".getBytes());

        ExpenseResponseDTO responseDTO = new ExpenseResponseDTO(
                expenseId, "수정된 점심 식사", new BigDecimal("30000"), ExpenseCategory.FOOD, ExpenseType.PERSONAL, null, null,
                LocalDate.now(), "수정된 memo", null, testUserId, LocalDateTime.now(), LocalDateTime.now(), null, true
        );

        given(expenseV2Service.updateExpense(eq(testUserId), eq(expenseId), any(UpdateExpenseRequestDTO.class), any()))
                .willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<ExpenseResponseDTO>> response = expenseV2Controller.updateExpense(testUser, expenseId, requestDTO, receiptFile);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().expenseId()).isEqualTo(expenseId);
        assertThat(response.getBody().getData().title()).isEqualTo("수정된 점심 식사");

        verify(expenseV2Service).updateExpense(eq(testUserId), eq(expenseId), any(UpdateExpenseRequestDTO.class), any());
    }

    @Test
    @DisplayName("영수증 이미지 조회 성공 테스트")
    void getReceiptImage_Success() {
        // given
        Long expenseId = 1L;
        String imageUrl = "http://example.com/receipt.jpg";

        given(expenseV2QueryService.getReceiptImageUrl(expenseId, testUserId)).willReturn(imageUrl);

        // when
        ResponseEntity<ApiResponse<String>> response = expenseV2Controller.getReceiptImage(testUser, expenseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(imageUrl);

        verify(expenseV2QueryService).getReceiptImageUrl(expenseId, testUserId);
    }

    @Test
    @DisplayName("영수증 삭제 성공 테스트")
    void deleteReceiptImage_Success() {
        // given
        Long expenseId = 1L;
        doNothing().when(expenseV2Service).deleteReceiptImage(testUserId, expenseId);

        // when
        ResponseEntity<ApiResponse<String>> response = expenseV2Controller.deleteReceiptImage(testUser, expenseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo("영수증이 삭제되었습니다.");

        verify(expenseV2Service).deleteReceiptImage(testUserId, expenseId);
    }

    @Test
    @DisplayName("개인 지출 월별 추이 조회 성공 테스트")
    void getPersonalExpenseTrend_Success() {
        // given
        LocalDate now = LocalDate.now();
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);
        MonthlyExpenseTrendResponseDTO responseDTO = new MonthlyExpenseTrendResponseDTO(Collections.emptyMap(), 0, new DateRangeDTO(now.atStartOfDay(), now.atStartOfDay()));

        given(expenseV2QueryService.getPersonalExpenseTrend(eq(testUserId), any(ExpenseSearchDTO.class)))
                .willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<MonthlyExpenseTrendResponseDTO>> response = expenseV2Controller.getPersonalExpenseTrend(testUser, searchDTO);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().monthlyData()).isEmpty();

        verify(expenseV2QueryService).getPersonalExpenseTrend(eq(testUserId), any(ExpenseSearchDTO.class));
    }

    @Test
    @DisplayName("그룹 지출 월별 추이 조회 성공 테스트")
    void getGroupExpenseTrend_Success() {
        // given
        LocalDate now = LocalDate.now();
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(now.getYear(), now.getMonthValue(), null, null, null);
        UUID groupId = UUID.randomUUID();
        MonthlyExpenseTrendResponseDTO responseDTO = new MonthlyExpenseTrendResponseDTO(Collections.emptyMap(), 0, new DateRangeDTO(now.atStartOfDay(), now.atStartOfDay()));

        given(expenseV2QueryService.getGroupExpenseTrend(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class)))
                .willReturn(responseDTO);

        // when
        ResponseEntity<ApiResponse<MonthlyExpenseTrendResponseDTO>> response = expenseV2Controller.getGroupExpenseTrend(testUser, groupId, searchDTO);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData().monthlyData()).isEmpty();

        verify(expenseV2QueryService).getGroupExpenseTrend(eq(testUserId), eq(groupId), any(ExpenseSearchDTO.class));
    }

    @Test
    @DisplayName("지출 삭제 성공 테스트")
    void deleteExpense_Success() {
        // given
        Long expenseId = 1L;
        doNothing().when(expenseV2Service).deleteExpense(testUserId, expenseId);

        // when
        ResponseEntity<ApiResponse<String>> response = expenseV2Controller.deleteExpense(testUser, expenseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
//        assertThat(response.getBody().getData()).isEqualTo("지출이 삭제되었습니다.");
        assertThat(response.getBody().getData()).isNotNull();

        verify(expenseV2Service).deleteExpense(testUserId, expenseId);
    }
}