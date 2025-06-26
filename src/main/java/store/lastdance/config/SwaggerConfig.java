package store.lastdance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LastDance API")
                        .description("""
                                LastDance 애플리케이션의 REST API 문서
                                
                                ⚠️ 인증 방식: 쿠키 기반 JWT 인증
                                - 로그인은 OAuth2를 통해 수행됩니다
                                - 인증이 필요한 API는 브라우저의 쿠키를 자동으로 사용합니다
                                - Swagger UI에서 직접 인증 테스트는 제한적입니다
                                
                                📋 응답 형식:
                                - 성공: ApiResponse<T> { success, data, message, errorCode }
                                - 에러: ErrorResponseDTO { status, error, message, path }
                                """)
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("개발 서버"),
                        new Server().url("https://api.lastdance.store").description("운영 서버")
                ))
                .components(new Components()
                        // 실제 에러 응답 구조에 맞게 수정
                        .addResponses("400", new ApiResponse()
                                .description("잘못된 요청")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                        .addResponses("401", new ApiResponse()
                                .description("인증 필요 (로그인이 필요합니다)")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                        .addResponses("403", new ApiResponse()
                                .description("접근 권한 없음")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                        .addResponses("404", new ApiResponse()
                                .description("리소스를 찾을 수 없음")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                        .addResponses("409", new ApiResponse()
                                .description("충돌 (중복된 리소스)")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                        .addResponses("500", new ApiResponse()
                                .description("서버 내부 오류")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema().$ref("#/components/schemas/ErrorResponseDTO")))))
                );
    }
}