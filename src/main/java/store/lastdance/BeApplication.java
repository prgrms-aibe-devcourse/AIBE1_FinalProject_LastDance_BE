package store.lastdance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import store.lastdance.config.oauth.OAuth2Properties;

@SpringBootApplication
@EnableConfigurationProperties(OAuth2Properties.class)
//@EnableJpaAuditing  // JPA Auditing 활성화
public class BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }

}
