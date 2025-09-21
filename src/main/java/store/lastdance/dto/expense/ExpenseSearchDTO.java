package store.lastdance.dto.expense;

import jakarta.validation.constraints.*;

public record ExpenseSearchDTO(
        @NotNull(message = "연도는 필수입니다")
        @Min(value = 1900, message = "연도는 1900년 이상이어야 합니다")
        @Max(value = 2100, message = "연도는 2100년 이하여야 합니다")
        Integer year,

        @NotNull(message = "월은 필수입니다")
        @Min(value = 1, message = "월은 1 이상이어야 합니다")
        @Max(value = 12, message = "월은 12 이하여야 합니다")
        Integer month,

        @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
        String category,

        @Size(max = 100, message = "검색어는 100자 이하여야 합니다")
        String search,

        @Min(value = 1, message = "조회 개월 수는 1개월 이상이어야 합니다")
        @Max(value = 24, message = "조회 개월 수는 24개월 이하여야 합니다")
        Integer months  // trend API용
) {
        public ExpenseSearchDTO {
                if (months == null || months < 1) {
                        months = 1;
                }
        }
}
