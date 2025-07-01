package store.lastdance.domain.group;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.user.User;

import java.util.UUID;

@Getter
@Entity
@Table(name = "group_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupApplication extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_application_id")
    private Long groupApplicationId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public GroupApplication(@NonNull UUID groupId,@NonNull UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }
}
