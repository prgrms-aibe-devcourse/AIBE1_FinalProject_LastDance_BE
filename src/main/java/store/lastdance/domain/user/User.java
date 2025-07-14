package store.lastdance.domain.user;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.common.ImageFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 20)
    private String username;

    @Column(name = "nickname", nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "inactived_at")
    private LocalDateTime inactivedAt;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Column(name = "ban_end_date")
    private LocalDateTime banEndDate;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "user_budget", nullable = false)
    private Integer userBudget = 1000000;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_file_id")
    private ImageFile profileImageFile;

    @Builder
    public User(@NonNull String email, @NonNull String username, @NonNull String nickname,
                @NonNull OAuthProvider provider, @NonNull String providerId, UserRole role, Boolean isActive, Boolean isBanned, Integer userBudget) {
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.isActive = isActive != null ? isActive : true;
        this.isBanned = isBanned != null ? isBanned : false;
        this.role = role != null ? role : UserRole.USER;
        this.userBudget = userBudget != null ? userBudget : 1000000;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public void updateProfileImage(ImageFile profileImageFile) {
        this.profileImageFile = profileImageFile;
    }

    public void removeProfileImage() {
        this.profileImageFile = null;
    }

    public void deactivate() {
        this.isActive = false;
        this.inactivedAt = LocalDateTime.now();
    }
    
    public void activate() {
        this.isActive = true;
        this.inactivedAt = null;
    }
    
    public void ban(LocalDateTime banEndDate) {
        this.isActive = false;
        this.isBanned = true;
        this.banEndDate = banEndDate;
    }
    
    public void unban() {
        this.isActive = true;
        this.isBanned = false;
        this.banEndDate = null;
    }
    
    public void updateBudget(Integer newBudget) {
        this.userBudget = newBudget;
    }
}
