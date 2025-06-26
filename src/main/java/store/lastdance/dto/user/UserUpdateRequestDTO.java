package store.lastdance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정보 수정 요청")
public record UserUpdateRequestDTO(
        @Schema(description = "새 닉네임", example = "멋진사용자", maxLength = 50)
        @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
        String nickname,

        @Schema(description = "월 예산", example = "1000000", minimum = "0")
        @PositiveOrZero(message = "예산은 0 이상이어야 합니다.")
        Integer monthlyBudget
) {}