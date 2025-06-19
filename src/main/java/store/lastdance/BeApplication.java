package store.lastdance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import store.lastdance.config.oauth.OAuth2Properties;

@SpringBootApplication
@EnableConfigurationProperties(OAuth2Properties.class)
public class BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }

}
