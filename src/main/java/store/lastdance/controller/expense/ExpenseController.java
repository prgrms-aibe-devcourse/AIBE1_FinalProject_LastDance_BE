package store.lastdance.controller.expense;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.*;
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

    @PostMapping(value = "/personal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "개인 지출 생성", description = "새로운 개인 지출 내역 추가")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createPersonalExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Parameter(description = "개인 지출 정보", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @Valid @RequestPart("expense") CreatePersonalExpenseRequestDTO requestDTO,
            @Parameter(description = "영수증 이미지 파일", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE))
            @RequestPart(required = false) MultipartFile receiptFile
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.createPersonalExpense(userId, requestDTO, receiptFile);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "그룹 지출 생성", description = "새로운 그룹 지출 내역 추가")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createGroupExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Parameter(description = "그룹 지출 정보", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @Valid @RequestPart("expense") CreateGroupExpenseRequestDTO requestDTO,
            @Parameter(description = "영수증 이미지 파일", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE))
            @RequestPart(required = false) MultipartFile receiptFile
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.createGroupExpense(userId, requestDTO, receiptFile);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/personal")
    @Operation(summary = "개인 지출 조회", description = "개인 지출 내역 조회 (PERSONAL 타입)")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getPersonalExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        UUID userId = oAuth2User.getUserId();
        List<ExpenseResponseDTO> response = expenseService.getPersonalExpenses(userId, year, month, category, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "그룹 지출 조회", description = "특정 그룹의 지출 내역 조회 (GROUP 타입)")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getGroupExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable UUID groupId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = oAuth2User.getUserId();
        List<ExpenseResponseDTO> response = expenseService.getGroupExpenses(userId, groupId, year, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/group/shares")
    @Operation(summary = "그룹 분담 지출 조회", description = "내가 분담하는 그룹 지출 내역 조회 (SHARE 타입)")
    public ResponseEntity<ApiResponse<List<GroupShareExpenseResponseDTO>>> getGroupShareExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = oAuth2User.getUserId();
        List<GroupShareExpenseResponseDTO> response = expenseService.getGroupShareExpenses(userId, year, month);
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

    @PatchMapping(value = "/{expenseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "지출 수정", description = "지출 내역 수정")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> updateExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId,
            @Parameter(description = "수정할 지출 정보", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @Valid @RequestPart("expense") UpdateExpenseRequestDTO requestDTO,
            @Parameter(description = "새 영수증 이미지 파일", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE))
            @RequestPart(required = false) MultipartFile receiptFile
    ) {
        UUID userId = oAuth2User.getUserId();
        ExpenseResponseDTO response = expenseService.updateExpense(userId, expenseId, requestDTO, receiptFile);
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

    @GetMapping("/{expenseId}/receipt")
    @Operation(summary = "영수증 이미지 조회", description = "지출의 영수증 이미지 Pre-signed URL 조회")
    public ResponseEntity<ApiResponse<String>> getReceiptImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId
    ) {
        UUID userId = oAuth2User.getUserId();
        String receiptImageUrl = expenseService.getReceiptImageUrl(expenseId, userId);

        if (receiptImageUrl == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "영수증이 없습니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(receiptImageUrl));
    }

    @DeleteMapping("/{expenseId}/receipt")
    @Operation(summary = "영수증 삭제", description = "지출의 영수증만 삭제")
    public ResponseEntity<ApiResponse<String>> deleteReceiptImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long expenseId
    ) {
        UUID userId = oAuth2User.getUserId();
        expenseService.deleteReceiptImage(expenseId, userId);
        return ResponseEntity.ok(ApiResponse.success("영수증이 삭제되었습니다."));
    }
}