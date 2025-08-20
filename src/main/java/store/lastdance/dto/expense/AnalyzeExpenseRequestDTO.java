package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "LLM 분석 응답")
public record AnalyzeExpenseRequestDTO(
        LocalDate startDate,
        LocalDate endDate
) {}
