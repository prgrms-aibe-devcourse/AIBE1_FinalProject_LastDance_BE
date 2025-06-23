package store.lastdance.service.customoauth;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.security.oauth.userinfo.GoogleUserInfo;
import store.lastdance.security.oauth.userinfo.KakaoUserInfo;
import store.lastdance.security.oauth.userinfo.NaverUserInfo;
import store.lastdance.security.oauth.userinfo.OAuth2UserInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    // ✅ 추가: 허용된 개발자 이메일 목록
    @Value("${allowed-dev-emails}")
    private String allowedDevEmails;

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

        log.debug("OAuth2 로그인 사용자: userId={}, email={}, name={}, nickname={}, provider={}",
                user.getUserId(), user.getEmail(), user.getUsername(), user.getNickname(), provider);

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
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        };
    }

    // ✅ 수정된 부분: 허용된 이메일이면 계정 자동 생성 허용
    private User findOrCreateUser(String provider, String providerId, String email,
                                  String username, String nickname, String profileImageUrl) {
        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(oAuthProvider, providerId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // ✅ 개발용 계정 허용
        if (isDevUser(email)) {
            User devUser = User.builder()
                    .userId(UUID.randomUUID())
                    .email(email)
                    .username(username)
                    .nickname(nickname)
                    .provider(oAuthProvider)
                    .providerId(providerId)
                    .build();

            return userRepository.save(devUser);
        }

        throw new CustomException(ErrorCode.USER_NOT_REGISTERED);
    }

    private boolean isDevUser(String email) {
        if (allowedDevEmails == null || allowedDevEmails.isBlank()) return false;
        return Arrays.asList(allowedDevEmails.split(",")).contains(email);
    }
}
