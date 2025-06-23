package store.lastdance.config.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.security.oauth2.client")
@Getter
@Setter
public class OAuth2Properties {
    private Map<String, Registration> registration = new HashMap<>();
    private Map<String, Provider> provider = new HashMap<>();

    @Getter @Setter
    public static class Registration {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String clientName;
        private List<String> scope;
        private String authorizationGrantType;
        private String clientAuthenticationMethod;
    }

    @Getter @Setter
    public static class Provider {
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;
    }
}