package store.lastdance;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import store.lastdance.config.oauth.OAuth2Properties;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(OAuth2Properties.class)
@EnableScheduling  // 스케줄링 활성화
@EnableJpaRepositories(basePackages = "store.lastdance.repository")
@EnableRedisRepositories(basePackages = "store.lastdance.repository.notification")
//@EnableJpaAuditing  // JPA Auditing 활성화
public class BeApplication {

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }
}
