package store.lastdance.dto.checklist;

import store.lastdance.domain.checklist.ChecklistType;
import store.lastdance.domain.checklist.Priority;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChecklistRequestDTO(
        String title,
        String description,
        UUID assigneeId,
        LocalDateTime dueDate,
        Priority priority) {
}
