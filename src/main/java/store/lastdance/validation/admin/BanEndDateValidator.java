package store.lastdance.validation.admin;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import store.lastdance.dto.admin.ReportProcessRequestDTO;

import java.time.LocalDateTime;

public class BanEndDateValidator implements ConstraintValidator<ValidBanEndDate, ReportProcessRequestDTO> {

    @Override
    public boolean isValid(ReportProcessRequestDTO dto, ConstraintValidatorContext context) {
        // 사용자를 정지하지 않는 경우: banEndDate는 반드시 null이어야 함
        if (!dto.banUser()) {
            return dto.banEndDate() == null;
        }

        // 사용자를 정지하는 경우: banEndDate는 null(영구 정지)이거나, 현재보다 미래 날짜여야 함
        return dto.banEndDate() == null || dto.banEndDate().isAfter(LocalDateTime.now());
    }
}