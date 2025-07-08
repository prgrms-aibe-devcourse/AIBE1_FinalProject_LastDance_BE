package store.lastdance.validation.admin;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BanEndDateValidator.class) // 👈 실제 검증 로직을 담은 클래스 지정
@Target({ElementType.TYPE}) // 클래스 또는 인터페이스에 적용
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBanEndDate {
    String message() default "사용자를 정지할 경우, 정지 종료일은 null(영구 정지)이거나 미래 날짜여야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}