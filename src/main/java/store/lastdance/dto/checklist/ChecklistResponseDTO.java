package store.lastdance.dto.checklist;

import store.lastdance.domain.checklist.ChecklistType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.group.GroupMemberDTO;

import java.util.UUID;

public record ChecklistResponseDTO(
        Long checklistId,
        String title,
        String description,
        ChecklistType type,
        UUID groupId,
        GroupMemberDTO assignee,
        Boolean isCompleted,
        String completedAt,
        String dueDate,
        String priority) {
}
