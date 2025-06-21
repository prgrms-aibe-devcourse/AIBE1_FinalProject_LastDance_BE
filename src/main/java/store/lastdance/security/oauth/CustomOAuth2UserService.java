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
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        };
    }

    private User findOrCreateUser(String provider, String providerId, String email,
                                  String username, String nickname, String profileImageUrl) {
        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        
        // 먼저 조회 시도
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(oAuthProvider, providerId);
        if (existingUser.isPresent()) {
            log.debug("기존 사용자 조회 성공: provider={}, providerId={}", provider, providerId);
            return existingUser.get();
        }
        
        // 사용자가 없으면 생성
        try {
            User newUser = User.builder()
                    .userId(UUID.randomUUID())
                    .email(email)
                    .username(username)
                    .nickname(nickname)
                    .provider(oAuthProvider)
                    .providerId(providerId)
                    .build();
                    
            User savedUser = userRepository.save(newUser);
            log.debug("새 사용자 생성 성공: userId={}, provider={}, providerId={}", 
                     savedUser.getUserId(), provider, providerId);
            return savedUser;
            
        } catch (Exception e) {
            // 동시 생성으로 인한 충돌 시 다시 조회 (좀 더 관대하게)
            log.warn("사용자 생성 중 충돌 발생, 재조회 시도: provider={}, providerId={}, error={}", 
                    provider, providerId, e.getMessage());
            
            // 잠시 대기 후 재조회
            try {
                Thread.sleep(100); // 100ms 대기
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            Optional<User> retryUser = userRepository.findByProviderAndProviderId(oAuthProvider, providerId);
            if (retryUser.isPresent()) {
                log.debug("재조회 성공: provider={}, providerId={}", provider, providerId);
                return retryUser.get();
            }
            
            // 그래도 실패하면 에러
            log.error("사용자 생성/조회 최종 실패: provider={}, providerId={}", provider, providerId, e);
            throw new RuntimeException("사용자 생성에 실패했습니다. 다시 시도해주세요.", e);
        }
    }
}