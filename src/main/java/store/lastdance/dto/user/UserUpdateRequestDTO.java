package store.lastdance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "사용자 정보 수정 요청 DTO")
public record UserUpdateRequestDTO(
        @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
        String nickname,

        @PositiveOrZero(message = "예산은 0 이상이어야 합니다.")
        Integer monthlyBudget
) {
}
