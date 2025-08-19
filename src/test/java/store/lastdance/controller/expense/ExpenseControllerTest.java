package store.lastdance.controller.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.CombinedExpenseResponseDTO;
import store.lastdance.dto.expense.CreateGroupExpenseRequestDTO;
import store.lastdance.dto.expense.CreatePersonalExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.GroupShareExpenseResponseDTO;
import store.lastdance.dto.expense.MonthlyExpenseTrendResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseSearchDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.expense.ExpenseService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testing library/framework: JUnit 5 (Jupiter) + Mockito; unit tests target controller methods directly
 * with a mocked ExpenseService and mocked CustomOAuth2User principal.
 * We assert HTTP response status/body wrapping and verify interaction with the service.
 */
public class ExpenseControllerTest {

    private ExpenseService expenseService;
    private ExpenseController controller;

    @BeforeEach
    void setUp() {
        expenseService = Mockito.mock(ExpenseService.class);
        controller = new ExpenseController(expenseService);
    }

    private CustomOAuth2User mockPrincipal(UUID userId) {
        CustomOAuth2User principal = mock(CustomOAuth2User.class);
        when(principal.getUserId()).thenReturn(userId);
        return principal;
    }

    @Nested
    @DisplayName("POST /api/v1/expenses/personal")
    class CreatePersonalExpenseTests {
        @Test
        @DisplayName("creates personal expense with receipt file (happy path)")
        void createPersonalExpense_withReceipt_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            CreatePersonalExpenseRequestDTO req = mock(CreatePersonalExpenseRequestDTO.class);
            MultipartFile receipt = mock(MultipartFile.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.createPersonalExpense(eq(userId), eq(req), eq(receipt)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.createPersonalExpense(principal, req, receipt);

            assertThat(resp).isNotNull();
            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService, times(1)).createPersonalExpense(userId, req, receipt);
        }

        @Test
        @DisplayName("creates personal expense without receipt file (null receipt)")
        void createPersonalExpense_withoutReceipt_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            CreatePersonalExpenseRequestDTO req = mock(CreatePersonalExpenseRequestDTO.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.createPersonalExpense(eq(userId), eq(req), isNull()))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.createPersonalExpense(principal, req, null);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).createPersonalExpense(userId, req, null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/expenses/group")
    class CreateGroupExpenseTests {
        @Test
        @DisplayName("creates group expense with receipt file")
        void createGroupExpense_withReceipt_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            CreateGroupExpenseRequestDTO req = mock(CreateGroupExpenseRequestDTO.class);
            MultipartFile receipt = mock(MultipartFile.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.createGroupExpense(eq(userId), eq(req), eq(receipt)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.createGroupExpense(principal, req, receipt);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).createGroupExpense(userId, req, receipt);
        }

        @Test
        @DisplayName("creates group expense with null receipt")
        void createGroupExpense_withoutReceipt_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            CreateGroupExpenseRequestDTO req = mock(CreateGroupExpenseRequestDTO.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.createGroupExpense(eq(userId), eq(req), isNull()))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.createGroupExpense(principal, req, null);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).createGroupExpense(userId, req, null);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/personal/combined")
    class GetCombinedExpensesTests {
        @Test
        @DisplayName("returns combined expenses with summary (happy path)")
        void getCombinedExpenses_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);
            Pageable pageable = PageRequest.of(0, 10);

            PageWithSummaryResponse<CombinedExpenseResponseDTO> serviceResp =
                new PageWithSummaryResponse<>(
                    new PageImpl<>(Collections.emptyList(), pageable, 0),
                    null // summary can be null or mocked as per implementation
                );

            when(expenseService.getCombinedExpenses(eq(userId), eq(search), eq(pageable)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<PageWithSummaryResponse<CombinedExpenseResponseDTO>>> resp =
                controller.getCombinedExpenses(principal, search, pageable);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            assertThat(resp.getBody().getMessage()).contains("성공");
            verify(expenseService).getCombinedExpenses(userId, search, pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/group/shares")
    class GetGroupShareExpensesTests {
        @Test
        @DisplayName("returns group share expenses list")
        void getGroupShareExpenses_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);

            List<GroupShareExpenseResponseDTO> serviceResp = List.of(mock(GroupShareExpenseResponseDTO.class));

            when(expenseService.getGroupShareExpenses(eq(userId), eq(search)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<List<GroupShareExpenseResponseDTO>>> resp =
                controller.getGroupShareExpenses(principal, search);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isEqualTo(serviceResp);
            verify(expenseService).getGroupShareExpenses(userId, search);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/group/{groupId}/shares/paging")
    class GetGroupShareExpensesWithPagingTests {
        @Test
        @DisplayName("returns group share expenses with paging and summary")
        void getGroupShareExpensesWithPaging_success() {
            UUID userId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);
            Pageable pageable = PageRequest.of(1, 5);

            PageWithSummaryResponse<GroupShareExpenseResponseDTO> serviceResp =
                new PageWithSummaryResponse<>(
                    new PageImpl<>(Collections.emptyList(), pageable, 0),
                    null
                );

            when(expenseService.getGroupShareExpensesWithPaging(eq(userId), eq(groupId), eq(search), eq(pageable)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<PageWithSummaryResponse<GroupShareExpenseResponseDTO>>> resp =
                controller.getGroupShareExpensesWithPaging(principal, groupId, search, pageable);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).getGroupShareExpensesWithPaging(userId, groupId, search, pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/group/{groupId}/with-stats")
    class GetGroupExpensesWithStatsTests {
        @Test
        @DisplayName("returns group expenses page with stats and success message")
        void getGroupExpensesWithStats_success() {
            UUID userId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);
            Pageable pageable = PageRequest.of(0, 20);

            PageWithSummaryResponse<ExpenseResponseDTO> serviceResp =
                new PageWithSummaryResponse<>(
                    new PageImpl<>(Collections.emptyList(), pageable, 0),
                    null
                );

            when(expenseService.getGroupExpensesWithStats(eq(userId), eq(groupId), eq(search), eq(pageable)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<PageWithSummaryResponse<ExpenseResponseDTO>>> resp =
                controller.getGroupExpensesWithStats(principal, groupId, search, pageable);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            assertThat(resp.getBody().getMessage()).contains("성공");
            verify(expenseService).getGroupExpensesWithStats(userId, groupId, search, pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/{expenseId}")
    class GetExpenseByIdTests {
        @Test
        @DisplayName("returns expense by id")
        void getExpenseById_success() {
            UUID userId = UUID.randomUUID();
            long expenseId = 42L;
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.getExpenseById(eq(userId), eq(expenseId)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.getExpenseById(principal, expenseId);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).getExpenseById(userId, expenseId);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/expenses/{expenseId}")
    class UpdateExpenseTests {
        @Test
        @DisplayName("updates expense with new receipt")
        void updateExpense_withReceipt_success() {
            UUID userId = UUID.randomUUID();
            long expenseId = 55L;
            CustomOAuth2User principal = mockPrincipal(userId);
            UpdateExpenseRequestDTO req = mock(UpdateExpenseRequestDTO.class);
            MultipartFile receipt = mock(MultipartFile.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.updateExpense(eq(userId), eq(expenseId), eq(req), eq(receipt)))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.updateExpense(principal, expenseId, req, receipt);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).updateExpense(userId, expenseId, req, receipt);
        }

        @Test
        @DisplayName("updates expense without new receipt (null)")
        void updateExpense_withoutReceipt_success() {
            UUID userId = UUID.randomUUID();
            long expenseId = 55L;
            CustomOAuth2User principal = mockPrincipal(userId);
            UpdateExpenseRequestDTO req = mock(UpdateExpenseRequestDTO.class);
            ExpenseResponseDTO serviceResp = mock(ExpenseResponseDTO.class);

            when(expenseService.updateExpense(eq(userId), eq(expenseId), eq(req), isNull()))
                .thenReturn(serviceResp);

            ResponseEntity<ApiResponse<ExpenseResponseDTO>> resp =
                controller.updateExpense(principal, expenseId, req, null);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).updateExpense(userId, expenseId, req, null);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/expenses/{expenseId}")
    class DeleteExpenseTests {
        @Test
        @DisplayName("deletes expense and returns success message")
        void deleteExpense_success() {
            UUID userId = UUID.randomUUID();
            long expenseId = 77L;
            CustomOAuth2User principal = mockPrincipal(userId);

            doNothing().when(expenseService).deleteExpense(userId, expenseId);

            ResponseEntity<ApiResponse<String>> resp =
                controller.deleteExpense(principal, expenseId);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).contains("삭제");
            verify(expenseService).deleteExpense(userId, expenseId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/{expenseId}/receipt")
    class GetReceiptImageTests {
        @Test
        @DisplayName("returns pre-signed URL when receipt exists")
        void getReceiptImage_present() {
            UUID userId = UUID.randomUUID();
            long expenseId = 99L;
            CustomOAuth2User principal = mockPrincipal(userId);
            String url = "https://example.com/presigned/url";

            when(expenseService.getReceiptImageUrl(expenseId, userId)).thenReturn(url);

            ResponseEntity<ApiResponse<String>> resp =
                controller.getReceiptImage(principal, expenseId);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isEqualTo(url);
            verify(expenseService).getReceiptImageUrl(expenseId, userId);
        }

        @Test
        @DisplayName("returns success with null data and message when no receipt exists")
        void getReceiptImage_absent() {
            UUID userId = UUID.randomUUID();
            long expenseId = 100L;
            CustomOAuth2User principal = mockPrincipal(userId);

            when(expenseService.getReceiptImageUrl(expenseId, userId)).thenReturn(null);

            ResponseEntity<ApiResponse<String>> resp =
                controller.getReceiptImage(principal, expenseId);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isNull();
            assertThat(resp.getBody().getMessage()).contains("영수증이 없습니다");
            verify(expenseService).getReceiptImageUrl(expenseId, userId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/expenses/{expenseId}/receipt")
    class DeleteReceiptImageTests {
        @Test
        @DisplayName("deletes receipt and returns success message")
        void deleteReceiptImage_success() {
            UUID userId = UUID.randomUUID();
            long expenseId = 123L;
            CustomOAuth2User principal = mockPrincipal(userId);

            doNothing().when(expenseService).deleteReceiptImage(expenseId, userId);

            ResponseEntity<ApiResponse<String>> resp =
                controller.deleteReceiptImage(principal, expenseId);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).contains("삭제");
            verify(expenseService).deleteReceiptImage(expenseId, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/personal/trend")
    class GetPersonalTrendTests {
        @Test
        @DisplayName("returns personal monthly expense trend")
        void getPersonalExpenseTrend_success() {
            UUID userId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);
            MonthlyExpenseTrendResponseDTO serviceResp = mock(MonthlyExpenseTrendResponseDTO.class);

            when(expenseService.getPersonalExpenseTrend(userId, search)).thenReturn(serviceResp);

            ResponseEntity<ApiResponse<MonthlyExpenseTrendResponseDTO>> resp =
                controller.getPersonalExpenseTrend(principal, search);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).getPersonalExpenseTrend(userId, search);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/group/{groupId}/trend")
    class GetGroupTrendTests {
        @Test
        @DisplayName("returns group monthly expense trend")
        void getGroupExpenseTrend_success() {
            UUID userId = UUID.randomUUID();
            UUID groupId = UUID.randomUUID();
            CustomOAuth2User principal = mockPrincipal(userId);
            ExpenseSearchDTO search = mock(ExpenseSearchDTO.class);
            MonthlyExpenseTrendResponseDTO serviceResp = mock(MonthlyExpenseTrendResponseDTO.class);

            when(expenseService.getGroupExpenseTrend(userId, groupId, search)).thenReturn(serviceResp);

            ResponseEntity<ApiResponse<MonthlyExpenseTrendResponseDTO>> resp =
                controller.getGroupExpenseTrend(principal, groupId, search);

            assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getData()).isSameAs(serviceResp);
            verify(expenseService).getGroupExpenseTrend(userId, groupId, search);
        }
    }
}