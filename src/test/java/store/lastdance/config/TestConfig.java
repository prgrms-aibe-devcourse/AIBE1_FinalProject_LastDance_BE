package store.lastdance.config;

import org.mockito.Mockito;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import io.awspring.cloud.s3.S3Template;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import store.lastdance.config.SecurityConfig;

@TestConfiguration
@Profile("test")
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
public class TestConfig {

    @Bean
    @Primary
    public S3Client s3Client() {
        return Mockito.mock(S3Client.class);
    }

    @Bean
    @Primary
    public S3Template s3Template() {
        return Mockito.mock(S3Template.class);
    }

    @Bean
    @Primary
    public S3Presigner s3Presigner() {
        return Mockito.mock(S3Presigner.class);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
