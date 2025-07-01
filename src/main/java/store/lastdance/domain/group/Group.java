package store.lastdance.domain.group;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.repository.group.GroupMemberRepository;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "invite_code", unique = true, nullable = false, length = 6)
    private String inviteCode;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;

    @Column(name = "group_budget", nullable = false)
    private Integer groupBudget = 1000000;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<GroupMember> members = new ArrayList<>();

    @Builder
    public Group(@NonNull String groupName, @NonNull String inviteCode, @NonNull UUID ownerId, Integer maxMembers, Integer groupBudget) {
        this.groupName = groupName;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
        this.maxMembers = maxMembers;
        this.groupBudget = groupBudget;
    }
    
    public void changeOwner(UUID newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public void addMember(GroupMember member) {
        members.add(member);
    }

    public void updateGroupDetails(String groupName, Integer maxMembers, Integer groupBudget) {

        if (groupName != null && !groupName.isEmpty()) {
            this.groupName = groupName;
        }
        if (maxMembers != null && maxMembers > 0) {
            this.maxMembers = maxMembers;
        }
        if (groupBudget != null && groupBudget >= 0) {
            this.groupBudget = groupBudget;
        }
    }
}
