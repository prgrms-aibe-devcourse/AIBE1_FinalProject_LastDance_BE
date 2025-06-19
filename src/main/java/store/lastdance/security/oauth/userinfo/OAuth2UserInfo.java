package store.lastdance.security.oauth.userinfo;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getProviderId();
    public abstract String getProvider();
    public abstract String getEmail();
    public abstract String getName();
    public abstract String getProfileImageUrl();

}