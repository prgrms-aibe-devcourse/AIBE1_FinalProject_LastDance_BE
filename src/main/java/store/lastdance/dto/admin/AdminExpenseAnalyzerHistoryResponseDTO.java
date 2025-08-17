package store.lastdance.dto.admin;

import java.util.List;

public record AdminExpenseAnalyzerHistoryResponseDTO(
    List<AdminExpenseAnalyzerHistoryDTO> histories,
    PaginationDTO pagination
) {
}
