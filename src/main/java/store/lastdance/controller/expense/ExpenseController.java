package store.lastdance.controller.expense;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.expense.CreateExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.expense.ExpenseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense", description = "지출 관리 API")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "지출 생성", description = "새로운 지출 내역 추가")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Valid @RequestBody CreateExpenseRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.createExpense(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "지출 조회", description = "지출 내역 조회 (개인/그룹별, 월별 필터링)")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestParam(defaultValue = "personal") String mode,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID groupId
    ) {
        UUID userId = oAuth2User.getUserId();
        List<ExpenseResponseDTO> response = expenseService.getExpenses(
                userId, mode, year, month, category, search, groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "지출 상세 조회", description = "특정 지출 내역 상세 정보")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> getExpenseById(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.getExpenseById(userId, expenseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{expenseId}")
    @Operation(summary = "지출 수정", description = "지출 내역 수정")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> updateExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateExpenseRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.updateExpense(userId, expenseId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "지출 삭제", description = "지출 내역 삭제")
    public ResponseEntity<ApiResponse<String>> deleteExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId
    ) {
        UUID userId = oAuth2User.getUserId();
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.ok(ApiResponse.success("지출이 삭제되었습니다."));
    }
}