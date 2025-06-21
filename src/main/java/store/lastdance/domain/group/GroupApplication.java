package store.lastdance.domain.group;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.user.User;

@Getter
@Entity
@Table(name = "group_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupApplication extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_application_id")
    private Long groupApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public GroupApplication(@NonNull Group group,@NonNull User user) {
        this.group = group;
        this.user = user;
    }
}
