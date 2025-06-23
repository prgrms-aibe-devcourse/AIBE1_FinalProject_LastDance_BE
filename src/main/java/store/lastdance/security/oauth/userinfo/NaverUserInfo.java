package store.lastdance.security.oauth.userinfo;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo {

    private final Map<String, Object> response;

    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) response.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        String name = (String) response.get("name");
        if (name == null || name.trim().isEmpty()) {
            return getNickname();
        }
        return name;
    }

    @Override
    public String getProfileImageUrl() {
        return (String) response.get("profile_image");
    }

    public String getNickname() {
        return (String) response.get("nickname");
    }

}