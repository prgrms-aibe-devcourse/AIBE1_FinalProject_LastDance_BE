package store.lastdance.config;

import lombok.extern.slf4j.Slf4j;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import store.lastdance.security.AuthenticationProcessor;
import store.lastdance.security.JwtAuthenticationFilter;
import store.lastdance.security.oauth.OAuth2LoginFailureHandler;
import store.lastdance.security.oauth.OAuth2LoginSuccessHandler;
import store.lastdance.util.CookieUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CookieUtils cookieUtils;
    private final AuthenticationProcessor authenticationProcessor;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/favicon.ico");
    }

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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS Preflight 요청 허용
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        // Swagger UI 관련 경로 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Actuator 경로 허용 (필요시)
                        .requestMatchers("/actuator/**").permitAll()
                        // 기타 공개 경로들
                        .requestMatchers("/error").permitAll()
                        // /api/v1/expenses/analyze 요청은 인증이 필요함 AOP 프록시
                        .requestMatchers("/api/v1/expenses/analyze").authenticated()
                        // 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                // API 요청에 대해서는 401 응답 (로그인 페이지로 리다이렉트 안함)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (response.isCommitted()) {
                                log.debug("Response already committed, cannot send 401 Unauthorized for AuthenticationException.");
                                return;
                            }
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (response.isCommitted()) {
                                log.debug("Response already committed, cannot send 403 Forbidden for AccessDeniedException.");
                                return;
                            }
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"접근 권한이 없습니다.\"}");
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(cookieUtils, authenticationProcessor),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
