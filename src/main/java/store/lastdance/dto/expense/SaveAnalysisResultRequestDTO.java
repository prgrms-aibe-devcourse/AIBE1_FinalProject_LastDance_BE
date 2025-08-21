package store.lastdance.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;

public record SaveAnalysisResultRequestDTO(
        @NotNull @Valid AnalyzeExpenseRequestDTO requestDTO,
        @NotNull @Valid AnalyzeExpenseResponseDTO analysisResponseDTO
) {
}
