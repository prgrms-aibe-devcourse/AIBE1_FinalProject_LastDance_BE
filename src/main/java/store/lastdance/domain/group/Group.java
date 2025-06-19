package store.lastdance.domain.group;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Getter
@Entity
@Table(name = "groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {
    @Id
    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "invite_code", unique = true, nullable = false, length = 6)
    private String inviteCode;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;

    @Column(name = "group_budget", nullable = false)
    private Integer groupBudget = 1000000;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<GroupMember> members = new ArrayList<>();

    @Builder
    public Group(@NonNull UUID groupId, @NonNull String groupName, @NonNull String inviteCode, @NonNull UUID ownerId) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
        this.maxMembers = 10;
        this.groupBudget = 1000000;
    }

    public void updateGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public void updateMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public void updateBudget(Integer groupBudget) {
        this.groupBudget = groupBudget;
    }
    
    public void regenerateInviteCode(String newInviteCode) {
        this.inviteCode = newInviteCode;
    }
    
    public void changeOwner(UUID newOwnerId) {
        this.ownerId = newOwnerId;
    }
}
