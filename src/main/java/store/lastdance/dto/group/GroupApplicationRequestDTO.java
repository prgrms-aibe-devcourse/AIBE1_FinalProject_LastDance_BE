package store.lastdance.dto.group;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GroupApplicationRequestDTO (

        @NotNull(message = "그룹 ID는 필수 항목입니다.")
        UUID groupId,

        @NotNull(message = "사용자 ID는 필수 항목입니다.")
        UUID userId
) {
}