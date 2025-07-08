package store.lastdance.dto.group;

import jakarta.validation.constraints.NotBlank;

public record InviteCodeRequestDTO(
    @NotBlank(message = "초대 코드는 필수입니다")
    String inviteCode
) {
}
