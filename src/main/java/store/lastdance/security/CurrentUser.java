package store.lastdance.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 현재 인증된 사용자 정보를 편리하게 가져오기 위한 커스텀 애노테이션
 * 
 * 사용법:
 * @PostMapping
 * public ResponseEntity<?> someMethod(@CurrentUser UserPrincipal user) {
 *     UUID userId = user.getId();
 *     // ...
 * }
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
