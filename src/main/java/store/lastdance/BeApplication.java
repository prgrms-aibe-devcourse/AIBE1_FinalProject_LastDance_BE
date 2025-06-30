package store.lastdance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import store.lastdance.config.oauth.OAuth2Properties;

@SpringBootApplication
@EnableConfigurationProperties(OAuth2Properties.class)
@EnableScheduling  // 스케줄링 활성화
//@EnableJpaAuditing  // JPA Auditing 활성화
public class BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }
}
