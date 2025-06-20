package store.lastdance.dto.group;

public record GroupRequestDTO(
    String groupName,
    Integer maxMembers,
    Integer groupBudget
) {
}
