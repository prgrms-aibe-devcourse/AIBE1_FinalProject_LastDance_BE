package store.lastdance.dto.checklist;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import store.lastdance.domain.checklist.ChecklistType;
import store.lastdance.domain.checklist.Priority;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChecklistRequestDTO(

        @NotBlank(message = "체크리스트 제목은 필수입니다.")
        String title,

        String description,

        @NotNull(message = "담당자 ID는 필수 항목입니다.")
        UUID assigneeId,

        @Future(message = "마감일은 현재 시간 이후여야 합니다.")
        LocalDateTime dueDate,

        @NotNull(message = "중요도는 필수 항목입니다.")
        Priority priority
) {
}
