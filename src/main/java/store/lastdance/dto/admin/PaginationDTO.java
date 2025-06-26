package store.lastdance.dto.admin;

public record PaginationDTO(
        int currentPage,
        int totalPages,
        int totalItems,
        boolean hasNext,
        boolean hasPrev
) {
}
