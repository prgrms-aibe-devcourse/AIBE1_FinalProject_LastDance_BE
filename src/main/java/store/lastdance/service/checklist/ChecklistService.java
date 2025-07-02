package store.lastdance.service.checklist;

import store.lastdance.dto.checklist.ChecklistRequestDTO;
import store.lastdance.dto.checklist.ChecklistResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ChecklistService {
    ChecklistResponseDTO createChecklist(ChecklistRequestDTO checklistRequestDTO, UUID userId, UUID groupId);

    List<ChecklistResponseDTO> getGroupChecklist(UUID groupId, UUID userId);

    List<ChecklistResponseDTO> getPersonalChecklist(UUID userId);

    ChecklistResponseDTO updateGroupChecklist(Long checklistId, ChecklistRequestDTO checklistRequestDTO, UUID userId, UUID groupId);

    ChecklistResponseDTO updatePersonalChecklist(Long checklistId, ChecklistRequestDTO checklistRequestDTO, UUID userId);

    void deleteChecklist(Long checklistId, UUID userId);

    ChecklistResponseDTO completeChecklist(Long checklistId, UUID userId);

    ChecklistResponseDTO undoChecklist(Long checklistId, UUID userId);
}
