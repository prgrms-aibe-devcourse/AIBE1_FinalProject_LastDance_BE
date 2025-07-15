package store.lastdance.security.oauth;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.security.oauth.userinfo.GoogleUserInfo;
import store.lastdance.security.oauth.userinfo.KakaoUserInfo;
import store.lastdance.security.oauth.userinfo.NaverUserInfo;
import store.lastdance.security.oauth.userinfo.OAuth2UserInfo;
import store.lastdance.service.notification.NotificationSettingService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final NotificationSettingService notificationSettingService;

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

    private User findOrCreateUser(String provider, String providerId, String email,
                                  String username, String nickname, String profileImageUrl) {
        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        
        // 먼저 조회 시도
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(oAuthProvider, providerId);
        if (existingUser.isPresent()) {
            // 비활성화 사용자 체크
            User user = existingUser.get();
            if (!user.getIsActive()) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_inactive", "USER_INACTIVE", null)
                );
            }
            return existingUser.get();
        }

        // 이메일 중복 체크
        Optional<User> existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser.isPresent()) {
            User emailUser = existingEmailUser.get();
            String existingProvider = emailUser.getProvider().name();

            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_already_exists",
                            "이 이메일은 이미 %s 계정으로 가입되어 있습니다. %s로 로그인해주세요.".formatted(
                                    getProviderDisplayName(existingProvider),
                                    getProviderDisplayName(existingProvider)
                            ),
                            null)
            );
        }

        // 사용자가 없으면 생성
        String uniqueNickname = makeUniqueNickname(nickname);

        try {
            User newUser = User.builder()
                    .email(email)
                    .username(username)
                    .nickname(uniqueNickname)
                    .provider(oAuthProvider)
                    .providerId(providerId)
                    .build();
                    
            User savedUser = userRepository.save(newUser);

            
            // 새 사용자에 대한 기본 알림 설정 생성
            try {
                notificationSettingService.createDefaultSetting(savedUser.getUserId());
            } catch (Exception e) {

                // 알림 설정 생성 실패가 로그인을 막지 않도록 continue
            }
            
            return savedUser;
            
        } catch (Exception e) {
            // 동시 생성으로 인한 충돌 시 다시 조회 (좀 더 관대하게)

            
            // 잠시 대기 후 재조회
            try {
                Thread.sleep(100); // 100ms 대기
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            Optional<User> retryUser = userRepository.findByProviderAndProviderId(oAuthProvider, providerId);
            if (retryUser.isPresent()) {
                return retryUser.get();
            }
            
            // 그래도 실패하면 에러
            throw new CustomException(ErrorCode.USER_CREATE_FAILED);
        }
    }

    private String getProviderDisplayName(String provider) {
        return switch (provider) {
            case "GOOGLE" -> "구글";
            case "KAKAO" -> "카카오";
            case "NAVER" -> "네이버";
            default -> provider;
        };
    }

    private String makeUniqueNickname(String nickname) {
        String result = nickname;
        int counter = 1;

        while (userRepository.existsByNickname(result)) {
            result = nickname + counter;
            counter++;
        }

        return result;
    }
}