package store.lastdance.dto.admin;

import java.util.List;

public record ReportManagementResponseDTO(
        List<ReportManagementDTO> reportManagements,
        PaginationDTO pagination
) {
}
