package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "LLM 분석 응답")
public record AnalyzeExpenseRequestDTO(
        LocalDate startDate,
        LocalDate endDate
) {}
