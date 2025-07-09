package store.lastdance.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 제한 시간을 정하는 변수
     * 기본값: 30, 단위: SECONDS
     */
    int time() default 30;

    /**
     * 제한 시간 단위를 정하는 변수
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     *  제한 시간 내의 최대 요청 횟수
     */
    int count() default 1;
}
