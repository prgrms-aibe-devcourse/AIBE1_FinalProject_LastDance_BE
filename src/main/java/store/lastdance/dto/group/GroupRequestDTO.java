package store.lastdance.dto.group;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupRequestDTO(
    @NotBlank(message = "그룹명은 필수입니다")
    String groupName,
    
    @NotNull(message = "최대 멤버 수는 필수입니다")
    @Min(value = 1, message = "최대 멤버 수는 1명 이상이어야 합니다")
    Integer maxMembers,
    
    @NotNull(message = "그룹 예산은 필수입니다")
    @Min(value = 0, message = "그룹 예산은 0원 이상이어야 합니다")
    Integer groupBudget
) {
}
