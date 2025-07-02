package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "정산 데이터")
public record SplitDataDTO(
        @Schema(description = "사용자 ID")
        @NotNull UUID userId,

        @Schema(description = "분담 금액")
        @NotNull @Positive BigDecimal amount
) {
}