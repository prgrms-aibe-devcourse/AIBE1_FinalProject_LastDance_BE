package store.lastdance.security;

import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID userId;           // user_id (PK)
    private String email;          // email
    private String nickname;       // nickname (name 대신)
    private String provider;       // provider (KAKAO, GOOGLE)
    private String providerId;     // provider_id
    private String role;           // role (USER, ADMIN)
    private Boolean isActive;      // is_active
    private Boolean isBanned;      // is_banned
    private LocalDateTime banEndDate; // ban_end_date

    // Spring Security UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    @Override
    public String getPassword() {
        // OAuth 사용으로 password 불필요
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 제재 상태 확인
        return !isBanned || (banEndDate != null && LocalDateTime.now().isAfter(banEndDate));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정 활성화 상태 확인
        return isActive;
    }

    // 편의 메서드들
    public UUID getId() {
        return userId;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isUser() {
        return "USER".equals(role);
    }
}