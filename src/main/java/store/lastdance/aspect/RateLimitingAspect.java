package store.lastdance.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.oauth.CustomOAuth2User;


@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingAspect {
    private final RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = getKey(joinPoint);

        long currentRequests = redisTemplate.opsForValue().increment(key);

        if(currentRequests == 1){
            redisTemplate.expire(key, rateLimit.time(), rateLimit.unit());
        }
        if(currentRequests > rateLimit.count()){
            log.warn("Rate limit for key : {}. ( {} requests in {} {})",
                    key, currentRequests, rateLimit.time(), rateLimit.unit().toString().toLowerCase());
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return joinPoint.proceed();
    }

    private String getKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())){
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            }

        CustomOAuth2User userPrincipal = (CustomOAuth2User) authentication.getPrincipal();
        String userId = userPrincipal.getUserId().toString();

        return "rate-limit:" + userId + ":" + methodName;
    }
}
