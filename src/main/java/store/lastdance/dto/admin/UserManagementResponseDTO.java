package store.lastdance.dto.admin;

import java.util.List;

public record UserManagementResponseDTO(
        List<UserManagementDTO> users,
        PaginationDTO pagination
) {
}
