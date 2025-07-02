package store.lastdance.domain.group;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;

import java.util.UUID;

@Getter
@Entity
@Table(name = "group_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_id")
    private Long groupMemberId;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupRole role = GroupRole.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public GroupMember(@NonNull Group group, @NonNull User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
    }

    public void changeRole(GroupRole newRole) {
        this.role = newRole;
    }
}
