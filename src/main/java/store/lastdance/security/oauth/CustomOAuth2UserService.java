package store.lastdance.security.oauth;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.security.oauth.userinfo.GoogleUserInfo;
import store.lastdance.security.oauth.userinfo.KakaoUserInfo;
import store.lastdance.security.oauth.userinfo.NaverUserInfo;
import store.lastdance.security.oauth.userinfo.OAuth2UserInfo;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo userInfo = createOAuth2UserInfo(provider, attributes);
        String providerId = userInfo.getProviderId();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String nickname = userInfo.getNickname();
        String profileImageUrl = userInfo.getProfileImageUrl();

        User user = findOrCreateUser(provider, providerId, email, name, nickname, profileImageUrl);

        log.info("OAuth2 로그인 사용자: userId={}, email={}, provider={}",
                user.getUserId(), user.getEmail(), provider);

        return new CustomOAuth2User(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                provider,
                providerId,
                attributes
        );
    }

    private OAuth2UserInfo createOAuth2UserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        };
    }

    private User findOrCreateUser(String provider, String providerId, String email,
                                  String username, String nickname, String profileImageUrl) {
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                OAuthProvider.valueOf(provider.toUpperCase()), providerId
        );

        return userOptional.orElseGet(() -> {
            return userRepository.save(User.builder()
                    .userId(UUID.randomUUID())
                    .email(email)
                    .username(username)
                    .nickname(nickname)
                    .provider(OAuthProvider.valueOf(provider.toUpperCase()))
                    .providerId(providerId)
                    .build());
        });
    }
}