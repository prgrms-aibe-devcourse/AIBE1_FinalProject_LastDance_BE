package store.lastdance.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import store.lastdance.security.AuthRedisService;
import store.lastdance.security.JwtAuthenticationFilter;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.security.oauth.OAuth2LoginSuccessHandler;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CookieUtils cookieUtils;
    private final ObjectMapper objectMapper;
    private final AuthRedisService authRedisService;
    private final UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/oauth2/**", "/login/**").permitAll()
                         .requestMatchers("/api/v1/auth/refresh").permitAll()
                        // 테스트 API (개발 환경에서만)
                        .requestMatchers("/api/test/no-auth", "/api/test/validate-token", "/api/test/token-info").permitAll()
                        // Swagger UI 관련 경로 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Actuator 경로 허용 (필요시)
                        .requestMatchers("/actuator/**").permitAll()
                        // 기타 공개 경로들
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                // API 요청에 대해서는 401 응답 (로그인 페이지로 리다이렉트 안함)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, cookieUtils, objectMapper, authRedisService, userService),
                                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
