package store.lastdance.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SaveAnalysisResultRequestDTO(
        @NotNull @Valid AnalyzeExpenseRequestDTO requestDTO,
        @NotNull @Valid AnalyzeExpenseResponseDTO analysisResponseDTO
) {
}
